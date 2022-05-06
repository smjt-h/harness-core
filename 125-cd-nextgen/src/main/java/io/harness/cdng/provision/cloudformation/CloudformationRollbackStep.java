/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.cloudformation;

import static io.harness.cdng.provision.cloudformation.CloudformationStepHelper.DEFAULT_TIMEOUT;
import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.cloudformation.CloudformationTaskType.CREATE_STACK;
import static io.harness.pms.execution.utils.AmbianceUtils.getAccountId;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.expressions.CDExpressionResolveFunctor;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.provision.cloudformation.beans.CloudFormationInheritOutput;
import io.harness.cdng.provision.cloudformation.beans.CloudformationConfig;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.exception.TaskNGDataException;
import io.harness.delegate.task.cloudformation.CloudFormationCreateStackNGResponse;
import io.harness.delegate.task.cloudformation.CloudformationCommandUnit;
import io.harness.delegate.task.cloudformation.CloudformationTaskNGParameters;
import io.harness.delegate.task.cloudformation.CloudformationTaskNGResponse;
import io.harness.delegate.task.cloudformation.CloudformationTaskType;
import io.harness.eraro.ErrorCode;
import io.harness.exception.AccessDeniedException;
import io.harness.exception.WingsException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.logging.CommandExecutionStatus;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskExecutableWithRollbackAndRbac;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.SkipTaskRequest;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepHelper;
import io.harness.steps.StepUtils;
import io.harness.supplier.ThrowingSupplier;

import software.wings.beans.TaskType;

import com.amazonaws.services.cloudformation.model.StackStatus;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class CloudformationRollbackStep extends TaskExecutableWithRollbackAndRbac<CloudformationTaskNGResponse> {
  @Inject CloudformationStepHelper cloudformationStepHelper;
  @Inject CloudformationConfigDAL cloudformationConfigDAL;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private StepHelper stepHelper;
  @Inject private EngineExpressionService engineExpressionService;
  @Inject private CDStepHelper cdStepHelper;
  @Inject private CDFeatureFlagHelper cdFeatureFlagHelper;

  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.CLOUDFORMATION_ROLLBACK_STACK.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {
    if (!cdFeatureFlagHelper.isEnabled(AmbianceUtils.getAccountId(ambiance), FeatureName.CLOUDFORMATION_NG)) {
      throw new AccessDeniedException(
          "Cloudformation NG is not enabled for this account. Please contact harness customer care.",
          ErrorCode.NG_ACCESS_DENIED, WingsException.USER);
    }
  }

  @Override
  public StepResponse handleTaskResultWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      ThrowingSupplier<CloudformationTaskNGResponse> responseDataSupplier) throws Exception {
    CloudformationTaskNGResponse cloudformationTaskNGResponse;
    try {
      cloudformationTaskNGResponse = responseDataSupplier.get();
    } catch (TaskNGDataException e) {
      String errorMessage =
          String.format("Error while processing Cloudformation Rollback Stack Task response: %s", e.getMessage());
      log.error(errorMessage, e);
      return cloudformationStepHelper.getFailureResponse(e.getCommandUnitsProgress().getUnitProgresses(), errorMessage);
    }

    if (cloudformationTaskNGResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
      return cloudformationStepHelper.getFailureResponse(
          cloudformationTaskNGResponse.getUnitProgressData().getUnitProgresses(),
          cloudformationTaskNGResponse.getErrorMessage());
    }

    StepResponseBuilder stepResponseBuilder =
        StepResponse.builder()
            .unitProgressList(cloudformationTaskNGResponse.getUnitProgressData().getUnitProgresses())
            .status(Status.SUCCEEDED);
    CloudFormationCreateStackNGResponse cloudFormationCreateStackNGResponse =
        (CloudFormationCreateStackNGResponse) cloudformationTaskNGResponse.getCloudFormationCommandNGResponse();
    if (cloudformationTaskNGResponse.getCloudFormationCommandNGResponse() != null) {
      stepResponseBuilder.stepOutcome(
          StepResponse.StepOutcome.builder()
              .name(OutcomeExpressionConstants.OUTPUT)
              .outcome(new CloudformationCreateStackOutcome(
                  isNotEmpty(cloudFormationCreateStackNGResponse.getCloudFormationOutputMap())
                      ? cloudFormationCreateStackNGResponse.getCloudFormationOutputMap()
                      : new HashMap<>()))
              .build());
    }
    return stepResponseBuilder.build();
  }

  @Override
  public TaskRequest obtainTaskAfterRbac(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    CloudformationRollbackStepParameters cloudformationRollbackStepParameters =
        (CloudformationRollbackStepParameters) stepParameters.getSpec();
    log.info("Starting execution Obtain Task after Rbac for the Cloudformation Rollback Step");
    String provisionerIdentifier =
        getParameterFieldValue(cloudformationRollbackStepParameters.getConfiguration().getProvisionerIdentifier());
    CloudFormationInheritOutput cloudFormationInheritOutput =
        cloudformationStepHelper.getSavedCloudFormationInheritOutput(provisionerIdentifier, ambiance);

    if (cloudFormationInheritOutput == null) {
      return obtainSkipRollbackTask(
          format("No successful Create Stack with provisionerIdentifier: [%s] found in this stage. Skipping rollback.",
              provisionerIdentifier));
    } else if (cloudFormationInheritOutput.isExistingStack()) {
      CloudformationConfig cloudformationConfig =
          cloudformationConfigDAL.getRollbackCloudformationConfig(ambiance, provisionerIdentifier);
      if (cloudformationConfig != null) {
        return obtainCloudformationRollbackTask(ambiance, stepParameters,
            getCreateStackCloudformationTaskNGParameters(ambiance, stepParameters, cloudformationConfig));
      } else {
        return obtainSkipRollbackTask(
            format("No successful Provisioning found with provisionerIdentifier: [%s]. Skipping rollback.",
                provisionerIdentifier));
      }
    } else {
      return obtainCloudformationRollbackTask(ambiance, stepParameters,
          getDeleteStackCloudformationTaskNGParameters(ambiance, cloudFormationInheritOutput));
    }
  }

  private TaskRequest obtainSkipRollbackTask(String reason) {
    return TaskRequest.newBuilder().setSkipTaskRequest(SkipTaskRequest.newBuilder().setMessage(reason).build()).build();
  }

  private TaskRequest obtainCloudformationRollbackTask(Ambiance ambiance, StepElementParameters stepParameters,
      CloudformationTaskNGParameters cloudformationTaskNGParameters) {
    TaskData taskData = TaskData.builder()
                            .async(true)
                            .taskType(TaskType.CLOUDFORMATION_TASK_NG.name())
                            .timeout(StepUtils.getTimeoutMillis(stepParameters.getTimeout(), DEFAULT_TIMEOUT))
                            .parameters(new Object[] {cloudformationTaskNGParameters})
                            .build();

    return StepUtils.prepareCDTaskRequest(ambiance, taskData, kryoSerializer,
        Collections.singletonList(cloudformationTaskNGParameters.getCfCommandUnit().name()),
        TaskType.CLOUDFORMATION_TASK_NG.getDisplayName(),
        StepUtils.getTaskSelectors(stepParameters.getDelegateSelectors()), stepHelper.getEnvironmentType(ambiance));
  }

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @VisibleForTesting
  CloudformationTaskNGParameters getCreateStackCloudformationTaskNGParameters(
      Ambiance ambiance, StepElementParameters stepParameters, CloudformationConfig cloudformationConfig) {
    AwsConnectorDTO awsConnectorDTO =
        (AwsConnectorDTO) cloudformationStepHelper.getConnectorDTO(cloudformationConfig.getConnectorRef(), ambiance)
            .getConnectorConfig();
    Map<String, String> parameters = new HashMap<>();
    cloudformationConfig.getParametersFiles().forEach(
        (s, strings)
            -> strings.forEach(fileContent
                -> parameters.putAll(cloudformationStepHelper.getParametersFromJson(ambiance, fileContent))));

    if (cloudformationConfig.getParameterOverrides() != null) {
      parameters.putAll(cloudformationConfig.getParameterOverrides());
    }

    CloudformationTaskNGParameters cloudformationTaskNGParameters =
        CloudformationTaskNGParameters.builder()
            .accountId(AmbianceUtils.getAccountId(ambiance))
            .stackName(cloudformationConfig.getStackName())
            .region(cloudformationConfig.getRegion())
            .taskType(CREATE_STACK)
            .cfCommandUnit(CloudformationCommandUnit.CreateStack)
            .templateUrl(cloudformationConfig.getTemplateUrl())
            .templateBody(cloudformationConfig.getTemplateBody())
            .awsConnector(awsConnectorDTO)
            .encryptedDataDetails(cloudformationStepHelper.getAwsEncryptionDetails(ambiance, awsConnectorDTO))
            .cloudFormationRoleArn(cloudformationConfig.getRoleArn())
            .parameters(parameters)
            .capabilities(cloudformationConfig.getCapabilities())
            .tags(cloudformationConfig.getTags())
            .stackStatusesToMarkAsSuccess(cloudformationConfig.getStackStatusesToMarkAsSuccess()
                                              .stream()
                                              .map(StackStatus::fromValue)
                                              .collect(Collectors.toList()))
            .timeoutInMs(StepUtils.getTimeoutMillis(stepParameters.getTimeout(), DEFAULT_TIMEOUT))
            .build();
    ExpressionEvaluatorUtils.updateExpressions(
        cloudformationTaskNGParameters, new CDExpressionResolveFunctor(engineExpressionService, ambiance));
    return cloudformationTaskNGParameters;
  }

  @VisibleForTesting
  CloudformationTaskNGParameters getDeleteStackCloudformationTaskNGParameters(
      Ambiance ambiance, CloudFormationInheritOutput cloudFormationInheritOutput) {
    ConnectorInfoDTO connectorInfoDTO =
        cloudformationStepHelper.getConnectorDTO(cloudFormationInheritOutput.getConnectorRef(), ambiance);
    AwsConnectorDTO connectorDTO = (AwsConnectorDTO) connectorInfoDTO.getConnectorConfig();
    CloudformationTaskNGParameters cloudformationTaskNGParameters =
        CloudformationTaskNGParameters.builder()
            .accountId(getAccountId(ambiance))
            .taskType(CloudformationTaskType.DELETE_STACK)
            .cfCommandUnit(CloudformationCommandUnit.DeleteStack)
            .awsConnector(connectorDTO)
            .region(cloudFormationInheritOutput.getRegion())
            .cloudFormationRoleArn(cloudFormationInheritOutput.getRoleArn())
            .encryptedDataDetails(cloudformationStepHelper.getAwsEncryptionDetails(ambiance, connectorDTO))
            .stackName(cloudFormationInheritOutput.getStackName())
            .build();
    ExpressionEvaluatorUtils.updateExpressions(
        cloudformationTaskNGParameters, new CDExpressionResolveFunctor(engineExpressionService, ambiance));
    return cloudformationTaskNGParameters;
  }
}
