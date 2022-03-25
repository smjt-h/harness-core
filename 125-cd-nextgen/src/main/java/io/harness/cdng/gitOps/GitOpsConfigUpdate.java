package io.harness.cdng.gitOps;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.git.model.ChangeType.MODIFY;
import static io.harness.steps.StepUtils.prepareCDTaskRequest;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.expressions.CDExpressionResolveFunctor;
import io.harness.cdng.gitOps.GitOpsConfigUpdatePassThroughData.GitOpsConfigUpdatePassThroughDataBuilder;
import io.harness.cdng.k8s.beans.GitFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.ValuesManifestOutcome;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.git.GitCommandExecutionResponse;
import io.harness.delegate.beans.git.GitCommandParams;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.git.GitFetchRequest;
import io.harness.delegate.task.git.GitFetchResponse;
import io.harness.delegate.task.git.TaskStatus;
import io.harness.exception.ExceptionUtils;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.git.model.CommitAndPushRequest;
import io.harness.git.model.FetchFilesResult;
import io.harness.git.model.GitFile;
import io.harness.git.model.GitFileChange;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskChainExecutableWithRollbackAndRbac;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepHelper;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import software.wings.beans.TaskType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jooq.tools.json.JSONObject;
import org.jooq.tools.json.JSONParser;
import org.jooq.tools.json.ParseException;

@OwnedBy(CDP)
public class GitOpsConfigUpdate extends TaskChainExecutableWithRollbackAndRbac {
  @Inject private EngineExpressionService engineExpressionService;
  @Inject private CDStepHelper cdStepHelper;
  @Inject private StepHelper stepHelper;
  @Inject private KryoSerializer kryoSerializer;
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.GITOPS_CONFIG_UPDATE.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {}

  @Override
  public TaskChainResponse startChainLinkAfterRbac(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    GitOpsConfigUpdateStepParams gitOpsSpecParams = (GitOpsConfigUpdateStepParams) stepParameters.getSpec();
    StoreConfig store = gitOpsSpecParams.getStore().getValue().getSpec();
    Map<String, String> stringMap = gitOpsSpecParams.getStringMap().getValue();
    ExpressionEvaluatorUtils.updateExpressions(
        store, new CDExpressionResolveFunctor(engineExpressionService, ambiance));
    ExpressionEvaluatorUtils.updateExpressions(
        stringMap, new CDExpressionResolveFunctor(engineExpressionService, ambiance));

    GitOpsConfigUpdatePassThroughDataBuilder passThroughDataBuilder =
        GitOpsConfigUpdatePassThroughData.builder().stringMap(stringMap).store(store);

    List<GitFetchFilesConfig> gitFetchFilesConfig = new ArrayList<>();
    gitFetchFilesConfig.add(getGitFetchFilesConfig(
        ambiance, store, ValuesManifestOutcome.builder().identifier("dummy").build(), passThroughDataBuilder));

    GitFetchRequest gitFetchRequest = GitFetchRequest.builder()
                                          .gitFetchFilesConfigs(gitFetchFilesConfig)
                                          .shouldOpenLogStream(true)
                                          .closeLogStream(true)
                                          .accountId(AmbianceUtils.getAccountId(ambiance))
                                          .build();

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(CDStepHelper.getTimeoutInMillis(stepParameters))
                                  .taskType(TaskType.GIT_FETCH_NEXT_GEN_TASK.name())
                                  .parameters(new Object[] {gitFetchRequest})
                                  .build();

    String taskName = TaskType.GIT_FETCH_NEXT_GEN_TASK.getDisplayName();

    final TaskRequest taskRequest = prepareCDTaskRequest(ambiance, taskData, kryoSerializer,
        gitOpsSpecParams.getCommandUnits(), taskName,
        TaskSelectorYaml.toTaskSelector(emptyIfNull(getParameterFieldValue(gitOpsSpecParams.getDelegateSelectors()))),
        stepHelper.getEnvironmentType(ambiance));

    return TaskChainResponse.builder()
        .chainEnd(false)
        .taskRequest(taskRequest)
        .passThroughData(passThroughDataBuilder.build())
        .build();
  }

  @Override
  public TaskChainResponse executeNextLinkWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseSupplier)
      throws Exception {
    ResponseData responseData = responseSupplier.get();

    try {
      if (responseData instanceof GitFetchResponse) {
        UnitProgressData unitProgressData = ((GitFetchResponse) responseData).getUnitProgressData();
        GitFetchResponse gitFetchResponse = (GitFetchResponse) responseData;
        if (gitFetchResponse.getTaskStatus() != TaskStatus.SUCCESS) {
          GitFetchResponsePassThroughData gitFetchResponsePassThroughData =
              GitFetchResponsePassThroughData.builder()
                  .errorMsg(gitFetchResponse.getErrorMessage())
                  .unitProgressData(gitFetchResponse.getUnitProgressData())
                  .build();
          return TaskChainResponse.builder().chainEnd(true).passThroughData(gitFetchResponsePassThroughData).build();
        }

        Map<String, FetchFilesResult> gitFetchFilesResultMap = gitFetchResponse.getFilesFromMultipleRepo();
        List<String> valuesFileContents = new ArrayList<>();
        if (!gitFetchFilesResultMap.isEmpty()) {
          for (GitFile gitFile : gitFetchFilesResultMap.get("dummy").getFiles()) {
            valuesFileContents.add(gitFile.getFileContent());
          }
        }

        List<String> renderedFiles =
            valuesFileContents.stream()
                .map(valuesFileContent -> engineExpressionService.renderExpression(ambiance, valuesFileContent, false))
                .collect(Collectors.toList());

        GitOpsConfigUpdatePassThroughData gitOpsConfigUpdatePassThroughData =
            (GitOpsConfigUpdatePassThroughData) passThroughData;

        // convert .yaml -> .json
        for (int i = 0; i < renderedFiles.size(); i++) {
          if (gitOpsConfigUpdatePassThroughData.getFilePaths().get(i).contains(".yaml")) {
            renderedFiles.set(i, convertYamlToJson(renderedFiles.get(i)));
          }
        }

        List<String> filesToBeCommitted;

        try {
          filesToBeCommitted = replaceFields(renderedFiles, gitOpsConfigUpdatePassThroughData.getStringMap());

          // convert .json back -> .yaml
          for (int i = 0; i < filesToBeCommitted.size(); i++) {
            if (gitOpsConfigUpdatePassThroughData.getFilePaths().get(i).contains(".yaml")) {
              filesToBeCommitted.set(i, convertJsonToYaml(filesToBeCommitted.get(i)));
            }
          }

        } catch (Exception e) {
          return TaskChainResponse.builder()
              .chainEnd(true)
              .passThroughData(StepExceptionPassThroughData.builder().errorMessage(e.getMessage()).build())
              .build();
        }

        if (filesToBeCommitted.isEmpty()) {
          // TODO: Handle appropriately
        }

        List<GitFileChange> gitFileChanges = new ArrayList<>();
        for (int i = 0; i < gitOpsConfigUpdatePassThroughData.getFilePaths().size(); i++) {
          gitFileChanges.add(GitFileChange.builder()
                                 .changeType(MODIFY)
                                 .filePath(gitOpsConfigUpdatePassThroughData.getFilePaths().get(i))
                                 .fileContent(filesToBeCommitted.get(i))
                                 .build());
        }
        GitStoreConfig gitStoreConfig = (GitStoreConfig) gitOpsConfigUpdatePassThroughData.getStore();

        String connectorId = gitStoreConfig.getConnectorRef().getValue();
        ConnectorInfoDTO connectorDTO = cdStepHelper.getConnector(connectorId, ambiance);

        GitCommandParams gitCommandParams = cdStepHelper.getGitCommitPushCommandParams(gitStoreConfig, connectorDTO,
            ValuesManifestOutcome.builder().identifier("dummy").build(),
            gitOpsConfigUpdatePassThroughData.getFilePaths(), ambiance, gitFileChanges, "fingers crossed");

        final TaskData taskData = TaskData.builder()
                                      .async(true)
                                      .timeout(CDStepHelper.getTimeoutInMillis(stepParameters))
                                      .taskType(TaskType.NG_GIT_COMMAND.name())
                                      .parameters(new Object[] {gitCommandParams})
                                      .build();

        String taskName = TaskType.NG_GIT_COMMAND.getDisplayName();

        GitOpsConfigUpdateStepParams gitOpsSpecParams = (GitOpsConfigUpdateStepParams) stepParameters.getSpec();

        final TaskRequest taskRequest =
            prepareCDTaskRequest(ambiance, taskData, kryoSerializer, gitOpsSpecParams.getCommandUnits(), taskName,
                TaskSelectorYaml.toTaskSelector(
                    emptyIfNull(getParameterFieldValue(gitOpsSpecParams.getDelegateSelectors()))),
                stepHelper.getEnvironmentType(ambiance));

        return TaskChainResponse.builder()
            .chainEnd(true)
            .taskRequest(taskRequest)
            .passThroughData(gitOpsConfigUpdatePassThroughData)
            .build();
      }

    } catch (Exception e) {
      return TaskChainResponse.builder()
          .chainEnd(true)
          .passThroughData(StepExceptionPassThroughData.builder()
                               .errorMessage(ExceptionUtils.getMessage(e))
                               //.unitProgressData(completeUnitProgressData(unitProgressData, ambiance, e))
                               .build())
          .build();
    }

    return null;
  }

  @Override
  public StepResponse finalizeExecutionWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    ResponseData responseData = responseDataSupplier.get();

    if (responseData instanceof GitCommandExecutionResponse) {
      // TODO: Handle aptly
    }

    return StepResponse.builder().build();
  }

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return null;
  }

  public CommitAndPushRequest getCommitPushReq() {
    return CommitAndPushRequest.builder().build();
  }

  public GitFetchFilesConfig getGitFetchFilesConfig(Ambiance ambiance, StoreConfig store,
      ManifestOutcome manifestOutcome, GitOpsConfigUpdatePassThroughDataBuilder passThroughDataBuilder) {
    GitStoreConfig gitStoreConfig = (GitStoreConfig) store;
    String connectorId = gitStoreConfig.getConnectorRef().getValue();
    ConnectorInfoDTO connectorDTO = cdStepHelper.getConnector(connectorId, ambiance);

    List<String> gitFilePaths = new ArrayList<>();
    gitFilePaths.addAll(getParameterFieldValue(gitStoreConfig.getPaths()));

    passThroughDataBuilder.filePaths(gitFilePaths);

    GitStoreDelegateConfig gitStoreDelegateConfig =
        cdStepHelper.getGitStoreDelegateConfig(gitStoreConfig, connectorDTO, manifestOutcome, gitFilePaths, ambiance);

    return GitFetchFilesConfig.builder()
        .identifier(manifestOutcome.getIdentifier())
        .manifestType(manifestOutcome.getType())
        .succeedIfFileNotFound(false)
        .gitStoreDelegateConfig(gitStoreDelegateConfig)
        .build();
  }

  public List<String> replaceFields(List<String> stringList, Map<String, String> stringMap) throws ParseException {
    List<String> result = new ArrayList<>();
    for (String str : stringList) {
      for (String key : stringMap.keySet()) {
        if (contains(str, key)) {
          str = replace(str, key, stringMap);
        }
      }
      result.add(str);
    }
    return result;
  }

  private boolean contains(String str, String key) {
    if (key.contains(".")) { // complex object
      String[] keys = key.split("\\.");
      boolean isSubstring = true;
      for (String s : keys) {
        isSubstring = isSubstring && str.contains(s);
      }
      return isSubstring;
    }

    // simple object
    return str.contains(key);
  }

  private String replace(String str, String key, Map<String, String> stringMap) throws ParseException {
    JSONParser parser = new JSONParser();
    JSONObject json = (JSONObject) parser.parse(str);

    if (key.contains(".")) {
      String[] keys = key.split("\\.");
      int len = keys.length - 1;
      return str.replaceAll("\"" + keys[len] + "\""
              + "\\s*:\\s*\"*" + recGet(json, keys) + "\"*",
          "\"" + keys[len] + "\": \"" + stringMap.get(key) + "\"");
    }

    return str.replaceAll("\"" + key + "\""
            + "\\s*:\\s*\"*" + json.get(key) + "\"*",
        "\"" + key + "\": \"" + stringMap.get(key) + "\"");
  }

  private String recGet(JSONObject jsonObject, String[] keys) {
    JSONObject json = jsonObject;
    int i = 0;
    while (i < keys.length - 1) {
      json = (JSONObject) json.get(keys[i]);
      i++;
    }
    return json.get(keys[i]).toString();
  }

  public String convertYamlToJson(String yaml) throws IOException {
    ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
    Object obj = yamlReader.readValue(yaml, Object.class);

    ObjectMapper jsonWriter = new ObjectMapper();
    return jsonWriter.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
  }

  public String convertJsonToYaml(String jsonString) throws IOException {
    JsonNode jsonNodeTree = new ObjectMapper().readTree(jsonString);
    return new YAMLMapper().writeValueAsString(jsonNodeTree);
  }
}
