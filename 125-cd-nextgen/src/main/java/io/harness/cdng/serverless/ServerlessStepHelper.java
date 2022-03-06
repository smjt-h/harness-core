/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.serverless;

import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.k8s.manifest.ManifestHelper.normalizeFolderPath;
import static io.harness.steps.StepUtils.prepareCDTaskRequest;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.expressions.CDExpressionResolveFunctor;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.yaml.InfrastructureKind;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.steps.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.*;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.cdng.serverless.beans.ServerlessExecutionPassThroughData;
import io.harness.cdng.serverless.beans.ServerlessGitFetchResponsePassThroughData;
import io.harness.cdng.serverless.beans.ServerlessStepExceptionPassThroughData;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.HarnessStringUtils;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.exception.TaskNGDataException;
import io.harness.delegate.task.git.TaskStatus;
import io.harness.delegate.task.serverless.*;
import io.harness.delegate.task.serverless.request.ServerlessDeployRequest;
import io.harness.delegate.task.serverless.request.ServerlessGitFetchRequest;
import io.harness.delegate.task.serverless.response.ServerlessDeployResponse;
import io.harness.delegate.task.serverless.response.ServerlessGitFetchResponse;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.git.model.FetchFilesResult;
import io.harness.git.model.GitFile;
import io.harness.ng.core.NGAccess;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureData;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepHelper;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import software.wings.beans.TaskType;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.CDP)
@Singleton
public class ServerlessStepHelper extends CDStepHelper {
  @Inject private OutcomeService outcomeService;
  @Inject private EngineExpressionService engineExpressionService;
  @Inject private ServerlessEntityHelper serverlessEntityHelper;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private StepHelper stepHelper;

  private static final Set<String> SERVERLESS_SUPPORTED_MANIFEST_TYPES = ImmutableSet.of(ManifestType.ServerlessAws);

  public TaskChainResponse startChainLink(
      ServerlessStepExecutor serverlessStepExecutor, Ambiance ambiance, StepElementParameters stepElementParameters) {
    ManifestsOutcome manifestsOutcome = resolveServerlessManifestsOutcome(ambiance);
    // todo: validation for manifest and infrastructure type
    InfrastructureOutcome infrastructureOutcome = (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME));
    ExpressionEvaluatorUtils.updateExpressions(
        manifestsOutcome, new CDExpressionResolveFunctor(engineExpressionService, ambiance));
    validateManifestsOutcome(ambiance, manifestsOutcome);
    ManifestOutcome serverlessManifestOutcome = getServerlessSupportedManifestOutcome(manifestsOutcome.values());
    return prepareServerlessManifestFetchTask(
        serverlessStepExecutor, serverlessManifestOutcome, ambiance, stepElementParameters, infrastructureOutcome);
  }

  public TaskChainResponse executeNextLink(ServerlessStepExecutor serverlessStepExecutor, Ambiance ambiance,
      StepElementParameters stepElementParameters, PassThroughData passThroughData,
      ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    ServerlessStepPassThroughData serverlessStepPassThroughData = (ServerlessStepPassThroughData) passThroughData;
    ManifestOutcome serverlessManifest = serverlessStepPassThroughData.getServerlessManifestOutcome();
    ResponseData responseData = responseDataSupplier.get();
    UnitProgressData unitProgressData = null;
    try {
      ServerlessGitFetchResponse serverlessGitFetchResponse = (ServerlessGitFetchResponse) responseData;
      return handleServerlessGitFetchFilesResponse(serverlessGitFetchResponse, serverlessStepExecutor, ambiance,
          stepElementParameters, serverlessStepPassThroughData, serverlessManifest);
    } catch (Exception e) {
      return TaskChainResponse.builder()
          .chainEnd(true)
          .passThroughData(ServerlessStepExceptionPassThroughData.builder()
                               .errorMessage(ExceptionUtils.getMessage(e))
                               .unitProgressData(completeUnitProgressData(unitProgressData, ambiance, e))
                               .build())
          .build();
    }
  }

  public ManifestsOutcome resolveServerlessManifestsOutcome(Ambiance ambiance) {
    OptionalOutcome manifestsOutcome = outcomeService.resolveOptional(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.MANIFESTS));

    if (!manifestsOutcome.isFound()) {
      String stageName =
          AmbianceUtils.getStageLevelFromAmbiance(ambiance).map(Level::getIdentifier).orElse("Deployment stage");
      String stepType =
          Optional.ofNullable(AmbianceUtils.getCurrentStepType(ambiance)).map(StepType::getType).orElse("Serverless");
      throw new GeneralException(format(
          "No manifests found in stage %s. %s step requires at least one manifest defined in stage service definition",
          stageName, stepType));
    }
    return (ManifestsOutcome) manifestsOutcome.getOutcome();
  }

  private TaskChainResponse prepareServerlessManifestFetchTask(ServerlessStepExecutor serverlessStepExecutor,
      ManifestOutcome manifestOutcome, Ambiance ambiance, StepElementParameters stepElementParameters,
      InfrastructureOutcome infrastructureOutcome) {
    switch (manifestOutcome.getType()) {
      case ManifestType.ServerlessAws:
        ServerlessAwsManifestOutcome serverlessAwsManifestOutcome = (ServerlessAwsManifestOutcome) manifestOutcome;
        return prepareAwsLambdaGitFetchManifestTaskChainResponse(
            ambiance, stepElementParameters, infrastructureOutcome, manifestOutcome, serverlessAwsManifestOutcome);
      default:
        throw new UnsupportedOperationException(format("Unsupported Manifest type: [%s]", manifestOutcome.getType()));
    }
  }

  private TaskChainResponse prepareAwsLambdaGitFetchManifestTaskChainResponse(Ambiance ambiance,
      StepElementParameters stepElementParameters, InfrastructureOutcome infrastructureOutcome,
      ManifestOutcome manifestOutcome, ServerlessAwsManifestOutcome serverlessAwsManifestOutcome) {
    StoreConfig storeConfig = manifestOutcome.getStore();
    GitStoreConfig gitStoreConfig = (GitStoreConfig) storeConfig;
    if (!ManifestStoreType.isInGitSubset(storeConfig.getKind())) {
      throw new InvalidRequestException("Invalid kind of storeConfig for Serverless step", USER);
    }
    ServerlessGitFetchFileConfig serverlessGitFetchFileConfig = mapAwsLambdaServerlessManifestToGitFetchFileConfig(
        ambiance, manifestOutcome, serverlessAwsManifestOutcome, gitStoreConfig);
    ServerlessStepPassThroughData serverlessStepPassThroughData =
        ServerlessStepPassThroughData.builder()
            .serverlessManifestOutcome(serverlessAwsManifestOutcome)
            .infrastructureOutcome(infrastructureOutcome)
            .build();
    return getGitFetchFileTaskResponse(
        ambiance, true, stepElementParameters, serverlessStepPassThroughData, serverlessGitFetchFileConfig);
  }

  public TaskChainResponse queueServerlessTask(StepElementParameters stepElementParameters,
      ServerlessDeployRequest serverlessDeployRequest, Ambiance ambiance,
      ServerlessExecutionPassThroughData executionPassThroughData) {
    TaskData taskData = TaskData.builder()
                            .parameters(new Object[] {serverlessDeployRequest})
                            .taskType(TaskType.SERVERLESS_COMMAND_TASK.name())
                            .timeout(CDStepHelper.getTimeoutInMillis(stepElementParameters))
                            .async(true)
                            .build();
    String taskName =
        TaskType.SERVERLESS_COMMAND_TASK.getDisplayName() + " : " + serverlessDeployRequest.getCommandName();
    ServerlessSpecParameters serverlessSpecParameters = (ServerlessSpecParameters) stepElementParameters.getSpec();
    final TaskRequest taskRequest =
        prepareCDTaskRequest(ambiance, taskData, kryoSerializer, serverlessSpecParameters.getCommandUnits(), taskName,
            TaskSelectorYaml.toTaskSelector(
                emptyIfNull(getParameterFieldValue(serverlessSpecParameters.getDelegateSelectors()))),
            stepHelper.getEnvironmentType(ambiance));
    return TaskChainResponse.builder()
        .taskRequest(taskRequest)
        .chainEnd(true)
        .passThroughData(executionPassThroughData)
        .build();
  }

  private TaskChainResponse handleServerlessGitFetchFilesResponse(ServerlessGitFetchResponse serverlessGitFetchResponse,
      ServerlessStepExecutor serverlessStepExecutor, Ambiance ambiance, StepElementParameters stepElementParameters,
      ServerlessStepPassThroughData serverlessStepPassThroughData, ManifestOutcome serverlessManifest) {
    if (serverlessGitFetchResponse.getTaskStatus() != TaskStatus.SUCCESS) {
      ServerlessGitFetchResponsePassThroughData serverlessGitFetchResponsePassThroughData =
          ServerlessGitFetchResponsePassThroughData.builder()
              .errorMsg(serverlessGitFetchResponse.getErrorMessage())
              .unitProgressData(serverlessGitFetchResponse.getUnitProgressData())
              .build();
      return TaskChainResponse.builder()
          .passThroughData(serverlessGitFetchResponsePassThroughData)
          .chainEnd(true)
          .build();
    }
    Map<String, FetchFilesResult> fetchFilesResultMap = serverlessGitFetchResponse.getFilesFromMultipleRepo();
    Optional<Pair<String, String>> manifestFilePathContent =
        getManifestFileContent(fetchFilesResultMap, serverlessManifest);
    if (!manifestFilePathContent.isPresent()) {
      throw new GeneralException("Found No Manifest Content from serverless git fetch task");
    }
    ServerlessExecutionPassThroughData serverlessExecutionPassThroughData =
        ServerlessExecutionPassThroughData.builder()
            .infrastructure(serverlessStepPassThroughData.getInfrastructureOutcome())
            .lastActiveUnitProgressData(serverlessGitFetchResponse.getUnitProgressData())
            .build();
    return serverlessStepExecutor.executeServerlessTask(serverlessManifest, ambiance, stepElementParameters,
        manifestFilePathContent.get(), serverlessExecutionPassThroughData, false,
        serverlessGitFetchResponse.getUnitProgressData());
  }

  public StepResponse handleGitTaskFailure(ServerlessGitFetchResponsePassThroughData serverlessGitFetchResponse) {
    UnitProgressData unitProgressData = serverlessGitFetchResponse.getUnitProgressData();
    return StepResponse.builder()
        .unitProgressList(unitProgressData.getUnitProgresses())
        .status(Status.FAILED)
        .failureInfo(FailureInfo.newBuilder().setErrorMessage(serverlessGitFetchResponse.getErrorMsg()).build())
        .build();
  }

  public StepResponse handleStepExceptionFailure(ServerlessStepExceptionPassThroughData stepException) {
    FailureData failureData = FailureData.newBuilder()
                                  .addFailureTypes(FailureType.APPLICATION_FAILURE)
                                  .setLevel(io.harness.eraro.Level.ERROR.name())
                                  .setCode(GENERAL_ERROR.name())
                                  .setMessage(HarnessStringUtils.emptyIfNull(stepException.getErrorMessage()))
                                  .build();
    return StepResponse.builder()
        .unitProgressList(stepException.getUnitProgressData().getUnitProgresses())
        .status(Status.FAILED)
        .failureInfo(FailureInfo.newBuilder()
                         .addAllFailureTypes(failureData.getFailureTypesList())
                         .setErrorMessage(failureData.getMessage())
                         .addFailureData(failureData)
                         .build())
        .build();
  }

  public StepResponse handleTaskException(
      Ambiance ambiance, ServerlessExecutionPassThroughData executionPassThroughData, Exception e) throws Exception {
    if (ExceptionUtils.cause(TaskNGDataException.class, e) != null) {
      throw e;
    }

    UnitProgressData unitProgressData =
        completeUnitProgressData(executionPassThroughData.getLastActiveUnitProgressData(), ambiance, e);
    FailureData failureData = FailureData.newBuilder()
                                  .addFailureTypes(FailureType.APPLICATION_FAILURE)
                                  .setLevel(io.harness.eraro.Level.ERROR.name())
                                  .setCode(GENERAL_ERROR.name())
                                  .setMessage(HarnessStringUtils.emptyIfNull(ExceptionUtils.getMessage(e)))
                                  .build();

    return StepResponse.builder()
        .unitProgressList(unitProgressData.getUnitProgresses())
        .status(Status.FAILED)
        .failureInfo(FailureInfo.newBuilder()
                         .addAllFailureTypes(failureData.getFailureTypesList())
                         .setErrorMessage(failureData.getMessage())
                         .addFailureData(failureData)
                         .build())
        .build();
  }

  public static StepResponse.StepResponseBuilder getFailureResponseBuilder(
      ServerlessDeployResponse serverlessDeployResponse, StepResponse.StepResponseBuilder stepResponseBuilder) {
    stepResponseBuilder.status(Status.FAILED)
        .failureInfo(FailureInfo.newBuilder()
                         .setErrorMessage(ServerlessStepHelper.getErrorMessage(serverlessDeployResponse))
                         .build());
    return stepResponseBuilder;
  }

  public static String getErrorMessage(ServerlessDeployResponse serverlessDeployResponse) {
    return serverlessDeployResponse.getErrorMessage() == null ? "" : serverlessDeployResponse.getErrorMessage();
  }

  public String getPreviousVersionStamp(ServerlessDeployResponse serverlessDeployResponse) {
    return null;
  }

  public String getServiceName(ServerlessDeployResponse serverlessDeployResponse) {
    return null;
  }

  private Optional<Pair<String, String>> getManifestFileContent(
      Map<String, FetchFilesResult> fetchFilesResultMap, ManifestOutcome manifestOutcome) {
    StoreConfig store = manifestOutcome.getStore();
    if (ManifestStoreType.isInGitSubset(store.getKind())) {
      FetchFilesResult fetchFilesResult = fetchFilesResultMap.get(manifestOutcome.getIdentifier());
      if (EmptyPredicate.isNotEmpty(fetchFilesResult.getFiles())) {
        GitFile gitFile = fetchFilesResult.getFiles().get(0);
        return Optional.of(ImmutablePair.of(gitFile.getFilePath(), gitFile.getFileContent()));
      }
    }
    return Optional.empty();
  }

  private TaskChainResponse getGitFetchFileTaskResponse(Ambiance ambiance, boolean shouldOpenLogStream,
      StepElementParameters stepElementParameters, ServerlessStepPassThroughData serverlessStepPassThroughData,
      ServerlessGitFetchFileConfig serverlessGitFetchFilesConfig) {
    String accountId = AmbianceUtils.getAccountId(ambiance);
    ServerlessGitFetchRequest serverlessGitFetchRequest =
        ServerlessGitFetchRequest.builder()
            .accountId(accountId)
            .serverlessGitFetchFileConfig(serverlessGitFetchFilesConfig)
            .shouldOpenLogStream(shouldOpenLogStream)
            .build();
    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(CDStepHelper.getTimeoutInMillis(stepElementParameters))
                                  .taskType(TaskType.SERVERLESS_GIT_FETCH_TASK_NG.name())
                                  .parameters(new Object[] {serverlessGitFetchRequest})
                                  .build();
    String taskName = TaskType.SERVERLESS_GIT_FETCH_TASK_NG.getDisplayName();
    ServerlessSpecParameters serverlessSpecParameters = (ServerlessSpecParameters) stepElementParameters.getSpec();
    final TaskRequest taskRequest =
        prepareCDTaskRequest(ambiance, taskData, kryoSerializer, serverlessSpecParameters.getCommandUnits(), taskName,
            TaskSelectorYaml.toTaskSelector(
                emptyIfNull(getParameterFieldValue(serverlessSpecParameters.getDelegateSelectors()))),
            stepHelper.getEnvironmentType(ambiance));
    return TaskChainResponse.builder()
        .chainEnd(false)
        .taskRequest(taskRequest)
        .passThroughData(serverlessStepPassThroughData)
        .build();
  }

  private ServerlessGitFetchFileConfig mapAwsLambdaServerlessManifestToGitFetchFileConfig(Ambiance ambiance,
      ManifestOutcome manifestOutcome, ServerlessAwsManifestOutcome serverlessAwsManifestOutcome,
      GitStoreConfig gitStoreConfig) {
    String validationMessage = format("Serverless manifest with Id [%s]", serverlessAwsManifestOutcome.getIdentifier());
    return getAwsLambdaManifestGitFetchFilesConfig(
        ambiance, validationMessage, gitStoreConfig, manifestOutcome, serverlessAwsManifestOutcome);
  }

  private ServerlessGitFetchFileConfig getAwsLambdaManifestGitFetchFilesConfig(Ambiance ambiance,
      String validationMessage, GitStoreConfig gitStoreConfig, ManifestOutcome manifestOutcome,
      ServerlessAwsManifestOutcome serverlessAwsManifestOutcome) {
    return ServerlessGitFetchFileConfig.builder()
        .gitStoreDelegateConfig(getGitStoreDelegateConfig(ambiance, gitStoreConfig, manifestOutcome))
        .identifier(manifestOutcome.getIdentifier())
        .manifestType(ManifestType.ServerlessAws)
        .configOverridePath(getParameterFieldValue(serverlessAwsManifestOutcome.getConfigOverridePath()))
        .succeedIfFileNotFound(false)
        .build();
  }

  private GitStoreDelegateConfig getGitStoreDelegateConfig(
      Ambiance ambiance, GitStoreConfig gitStoreConfig, ManifestOutcome manifestOutcome) {
    String connectorId = gitStoreConfig.getConnectorRef().getValue();
    String validationMessage = format("Serverless manifest with Id [%s]", manifestOutcome.getIdentifier());
    ConnectorInfoDTO connectorDTO = getConnectorDTO(connectorId, ambiance);
    validateManifest(gitStoreConfig.getKind(), connectorDTO, validationMessage);
    List<String> gitPaths = getFolderPathsForManifest(gitStoreConfig);
    return getGitStoreDelegateConfig(gitStoreConfig, connectorDTO, manifestOutcome, gitPaths, ambiance);
  }

  public String renderManifestContent(Ambiance ambiance, String manifestFileContent) {
    if (isEmpty(manifestFileContent)) {
      return manifestFileContent;
    }
    return engineExpressionService.renderExpression(ambiance, manifestFileContent);
  }

  public ServerlessCommandType getServerlessDeployCommandType(InfrastructureOutcome infrastructureOutcome) {
    switch (infrastructureOutcome.getKind()) {
      case InfrastructureKind.SERVERLESS_AWS:
        return ServerlessCommandType.AWS_LAMBDA_DEPLOY;
      default:
        throw new UnsupportedOperationException(
            format("Unsupported infra kind: [%s]", infrastructureOutcome.getKind()));
    }
  }

  public ServerlessInfraConfig getServerlessInfraConfig(InfrastructureOutcome infrastructure, Ambiance ambiance) {
    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    return serverlessEntityHelper.getServerlessInfraConfig(infrastructure, ngAccess);
  }

  public ServerlessDeployConfig getServerlessDeployConfig(
      ServerlessCommandType serverlessCommandType, ServerlessDeployStepParameters serverlessDeployStepParameters) {
    switch (serverlessCommandType) {
      case AWS_LAMBDA_DEPLOY:
        ServerlessAwsDeployConfig serverlessAwsDeployConfig =
            ServerlessAwsDeployConfig.builder().commandOptions(Collections.emptyList()).build();
        // todo: need to change command options
        return serverlessAwsDeployConfig;
      default:
        throw new UnsupportedOperationException(
            format("Unsupported serverless command: [%s]", serverlessCommandType.name()));
    }
  }

  public ServerlessManifest getServerlessManifestConfig(Pair<String, String> manifestFilePathContent,
      String manifestFileOverrideContent, ManifestOutcome serverlessManifestOutcome, Ambiance ambiance) {
    switch (serverlessManifestOutcome.getType()) {
      case ManifestType.ServerlessAws:
        ServerlessAwsManifestOutcome serverlessAwsManifestOutcome =
            (ServerlessAwsManifestOutcome) serverlessManifestOutcome;
        GitStoreConfig gitStoreConfig = (GitStoreConfig) serverlessAwsManifestOutcome.getStore();
        ServerlessManifestConfig serverlessManifestConfig =
            ServerlessManifestConfig.builder()
                .manifestPath(manifestFilePathContent.getKey())
                .manifestContent(manifestFileOverrideContent)
                .gitStoreDelegateConfig(getGitStoreDelegateConfig(ambiance, gitStoreConfig, serverlessManifestOutcome))
                .build();
        return serverlessManifestConfig;
      default:
        throw new UnsupportedOperationException(
            format("Unsupported serverless manifest type: [%s]", serverlessManifestOutcome.getType()));
    }
  }

  public List<ServerInstanceInfo> getFunctionInstanceInfo(ServerlessDeployResponse serverlessDeployResponse) {
    return null;
    // todo: implement it.
  }

  @VisibleForTesting
  public ManifestOutcome getServerlessSupportedManifestOutcome(@NotEmpty Collection<ManifestOutcome> manifestOutcomes) {
    List<ManifestOutcome> serverlessManifests =
        manifestOutcomes.stream()
            .filter(manifestOutcome -> SERVERLESS_SUPPORTED_MANIFEST_TYPES.contains(manifestOutcome.getType()))
            .collect(Collectors.toList());
    if (isEmpty(serverlessManifests)) {
      throw new InvalidRequestException("Manifests are mandatory for Serverless step", USER);
    }
    if (serverlessManifests.size() > 1) {
      throw new InvalidRequestException("There can be only a single manifest for Serverless step", USER);
    }
    return serverlessManifests.get(0);
  }

  private ConnectorInfoDTO getConnectorDTO(String connectorId, Ambiance ambiance) {
    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    return serverlessEntityHelper.getConnectorInfoDTO(connectorId, ngAccess);
  }

  private List<String> getFolderPathsForManifest(GitStoreConfig gitStoreConfig) {
    List<String> folderPaths = new ArrayList<>();
    String folderPath = getParameterFieldValue(gitStoreConfig.getFolderPath());
    folderPaths.add(normalizeFolderPath(folderPath));
    return folderPaths;
  }
}
