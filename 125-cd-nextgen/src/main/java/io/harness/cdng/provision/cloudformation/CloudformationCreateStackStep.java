/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.cloudformation;

import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.provision.cloudformation.beans.CloudFormationCreateStackPassThroughData;
import io.harness.cdng.provision.cloudformation.beans.CloudformationConfig;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.exception.TaskNGDataException;
import io.harness.delegate.task.cloudformation.CloudFormationCreateStackNGResponse;
import io.harness.delegate.task.cloudformation.CloudformationCommandUnit;
import io.harness.delegate.task.cloudformation.CloudformationTaskNGParameters;
import io.harness.delegate.task.cloudformation.CloudformationTaskNGResponse;
import io.harness.eraro.ErrorCode;
import io.harness.exception.AccessDeniedException;
import io.harness.exception.WingsException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ng.core.EntityDetail;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskChainExecutableWithRollbackAndRbac;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.rbac.PipelineRbacHelper;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepHelper;
import io.harness.steps.StepUtils;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;
import io.harness.utils.IdentifierRefHelper;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class CloudformationCreateStackStep
    extends TaskChainExecutableWithRollbackAndRbac implements CloudformationStepExecutor {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.CLOUDFORMATION_CREATE_STACK.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();
  @Inject private KryoSerializer kryoSerializer;
  @Inject private PipelineRbacHelper pipelineRbacHelper;
  @Inject private StepHelper stepHelper;
  @Inject private CDStepHelper cdStepHelper;
  @Inject private CloudformationStepHelper cloudformationStepHelper;
  @Inject private CDFeatureFlagHelper cdFeatureFlagHelper;
  @Inject private CloudformationConfigDAL cloudformationConfigDAL;

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {
    if (!cdFeatureFlagHelper.isEnabled(AmbianceUtils.getAccountId(ambiance), FeatureName.CLOUDFORMATION_NG)) {
      throw new AccessDeniedException(
          "Cloudformation NG is not enabled for this account. Please contact harness customer care.",
          ErrorCode.NG_ACCESS_DENIED, WingsException.USER);
    }
    List<EntityDetail> entityDetailList = new ArrayList<>();
    String accountId = AmbianceUtils.getAccountId(ambiance);
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);

    // Template file connector
    CloudformationCreateStackStepParameters cloudformationCreateStackStepParameters =
        (CloudformationCreateStackStepParameters) stepParameters.getSpec();
    CloudformationTemplateFileSpec cloudformationTemplateFileSpec =
        cloudformationCreateStackStepParameters.getConfiguration().getTemplateFile().getSpec();

    if (cloudformationTemplateFileSpec.getType().equals(CloudformationTemplateFileTypes.Remote)) {
      RemoteCloudformationTemplateFileSpec remoteTemplateFile =
          (RemoteCloudformationTemplateFileSpec) cloudformationTemplateFileSpec;
      String connectorRef = remoteTemplateFile.getStore().getSpec().getConnectorReference().getValue();
      IdentifierRef identifierRef =
          IdentifierRefHelper.getIdentifierRef(connectorRef, accountId, orgIdentifier, projectIdentifier);
      EntityDetail entityDetail = EntityDetail.builder().type(EntityType.CONNECTORS).entityRef(identifierRef).build();
      entityDetailList.add(entityDetail);
    }

    // Parameters file connectors
    if (isNotEmpty(cloudformationCreateStackStepParameters.getConfiguration().getParametersFilesSpecs())) {
      cloudformationCreateStackStepParameters.getConfiguration().getParametersFilesSpecs().forEach(
          cloudformationParametersFileSpec -> {
            String connectorRef =
                getParameterFieldValue(cloudformationParametersFileSpec.getStore().getSpec().getConnectorReference());
            IdentifierRef identifierRef =
                IdentifierRefHelper.getIdentifierRef(connectorRef, accountId, orgIdentifier, projectIdentifier);
            EntityDetail entityDetail =
                EntityDetail.builder().type(EntityType.CONNECTORS).entityRef(identifierRef).build();
            entityDetailList.add(entityDetail);
          });
    }

    // AWS connector
    String connectorRef =
        getParameterFieldValue(cloudformationCreateStackStepParameters.getConfiguration().getConnectorRef());
    IdentifierRef identifierRef =
        IdentifierRefHelper.getIdentifierRef(connectorRef, accountId, orgIdentifier, projectIdentifier);
    EntityDetail entityDetail = EntityDetail.builder().type(EntityType.CONNECTORS).entityRef(identifierRef).build();
    entityDetailList.add(entityDetail);

    pipelineRbacHelper.checkRuntimePermissions(ambiance, entityDetailList, true);
  }

  @Override
  public TaskChainResponse executeNextLinkWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseSupplier)
      throws Exception {
    return cloudformationStepHelper.executeNextLink(this, ambiance, stepParameters, passThroughData, responseSupplier);
  }

  @Override
  public StepResponse finalizeExecutionWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    CloudformationCreateStackStepParameters cloudformationCreateStackStepParameters =
        (CloudformationCreateStackStepParameters) stepParameters.getSpec();
    CloudformationCreateStackStepConfiguration stepConfiguration =
        cloudformationCreateStackStepParameters.getConfiguration();
    if (passThroughData instanceof StepExceptionPassThroughData) {
      StepExceptionPassThroughData stepExceptionPassThroughData = (StepExceptionPassThroughData) passThroughData;
      return cdStepHelper.handleStepExceptionFailure(stepExceptionPassThroughData);
    }

    CloudformationTaskNGResponse cloudformationTaskNGResponse;
    try {
      cloudformationTaskNGResponse = (CloudformationTaskNGResponse) responseDataSupplier.get();
    } catch (TaskNGDataException e) {
      String errorMessage =
          String.format("Error while processing Cloudformation Create Stack Task response: %s", e.getMessage());
      log.error(errorMessage, e);
      return cloudformationStepHelper.getFailureResponse(e.getCommandUnitsProgress().getUnitProgresses(), errorMessage);
    }

    if (cloudformationTaskNGResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
      return cloudformationStepHelper.getFailureResponse(
          cloudformationTaskNGResponse.getUnitProgressData().getUnitProgresses(),
          cloudformationTaskNGResponse.getErrorMessage());
    }
    CloudFormationCreateStackNGResponse cloudFormationCreateStackNGResponse =
        (CloudFormationCreateStackNGResponse) cloudformationTaskNGResponse.getCloudFormationCommandNGResponse();

    cloudformationStepHelper.saveCloudFormationInheritOutput(stepConfiguration,
        getParameterFieldValue(cloudformationCreateStackStepParameters.getProvisionerIdentifier()), ambiance,
        cloudFormationCreateStackNGResponse.isExistentStack());
    CloudformationConfig cloudformationConfig = cloudformationStepHelper.getCloudformationConfig(
        ambiance, stepParameters, (CloudFormationCreateStackPassThroughData) passThroughData);
    cloudformationConfigDAL.saveCloudformationConfig(cloudformationConfig);
    return StepResponse.builder()
        .unitProgressList(cloudformationTaskNGResponse.getUnitProgressData().getUnitProgresses())
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(OutcomeExpressionConstants.OUTPUT)
                         .outcome(new CloudformationCreateStackOutcome(
                             isNotEmpty(cloudFormationCreateStackNGResponse.getCloudFormationOutputMap())
                                 ? cloudFormationCreateStackNGResponse.getCloudFormationOutputMap()
                                 : new HashMap<>()))
                         .build())
        .status(Status.SUCCEEDED)
        .build();
  }

  @Override
  public TaskChainResponse startChainLinkAfterRbac(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    return cloudformationStepHelper.startChainLink(this, ambiance, stepParameters);
  }

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public TaskChainResponse executeCloudformationTask(Ambiance ambiance, StepElementParameters stepParameters,
      CloudformationTaskNGParameters parameters, CloudFormationCreateStackPassThroughData passThroughData) {
    TaskData taskData =
        TaskData.builder()
            .async(true)
            .taskType(TaskType.CLOUDFORMATION_TASK_NG.name())
            .timeout(StepUtils.getTimeoutMillis(stepParameters.getTimeout(), CloudformationStepHelper.DEFAULT_TIMEOUT))
            .parameters(new Object[] {parameters})
            .build();
    final TaskRequest taskRequest = StepUtils.prepareCDTaskRequest(ambiance, taskData, kryoSerializer,
        Arrays.asList(K8sCommandUnitConstants.FetchFiles, CloudformationCommandUnit.CreateStack.name()),
        TaskType.CLOUDFORMATION_TASK_NG.getDisplayName(),
        StepUtils.getTaskSelectors(stepParameters.getDelegateSelectors()), stepHelper.getEnvironmentType(ambiance));
    return TaskChainResponse.builder().taskRequest(taskRequest).passThroughData(passThroughData).chainEnd(true).build();
  }
}
