/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.cloudformation;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.common.resources.AwsResourceServiceHelper;
import io.harness.common.ParameterFieldHelper;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.task.cloudformation.CloudformationCommandUnit;
import io.harness.delegate.task.cloudformation.CloudformationTaskNGParameters;
import io.harness.delegate.task.cloudformation.CloudformationTaskNGParameters.CloudformationTaskNGParametersBuilder;
import io.harness.delegate.task.cloudformation.CloudformationTaskNGResponse;
import io.harness.delegate.task.cloudformation.CloudformationTaskType;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.UnitProgress;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.EntityDetail;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskExecutableWithRollbackAndRbac;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.rbac.PipelineRbacHelper;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepHelper;
import io.harness.steps.StepUtils;
import io.harness.supplier.ThrowingSupplier;
import io.harness.utils.IdentifierRefHelper;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class CloudformationDeleteStackStep extends TaskExecutableWithRollbackAndRbac<CloudformationTaskNGResponse> {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.CLOUDFORMATION_DELETE_STACK.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();
  @Inject private CloudformationStepHelper helper;
  @Inject private AwsResourceServiceHelper awsHelper;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private PipelineRbacHelper pipelineRbacHelper;
  @Inject private StepHelper stepHelper;
  @Inject private CloudformationConfigDAL cloudformationConfigDAL;

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {
    List<EntityDetail> entityDetailList = new ArrayList<>();
    String accountId = AmbianceUtils.getAccountId(ambiance);
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);

    CloudformationDeleteStackStepParameters parameters =
        (CloudformationDeleteStackStepParameters) stepParameters.getSpec();
    if (parameters.configuration.getType() == CloudformationStepConfigurationType.INLINE) {
      String connectorRef = parameters.getConfiguration().getSpec().getConnectorRef().getValue();
      IdentifierRef identifierRef =
          IdentifierRefHelper.getIdentifierRef(connectorRef, accountId, orgIdentifier, projectIdentifier);
      EntityDetail entityDetail = EntityDetail.builder().type(EntityType.CONNECTORS).entityRef(identifierRef).build();
      entityDetailList.add(entityDetail);
      pipelineRbacHelper.checkRuntimePermissions(ambiance, entityDetailList, true);
    }
  }

  @Override
  public StepResponse handleTaskResultWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      ThrowingSupplier<CloudformationTaskNGResponse> responseDataSupplier) throws Exception {
    log.info("Handling Task Result With Security Context for the DeleteStack Step");
    CloudformationDeleteStackStepParameters parameters =
        (CloudformationDeleteStackStepParameters) stepParameters.getSpec();
    StepResponseBuilder builder = StepResponse.builder();
    CloudformationTaskNGResponse response = responseDataSupplier.get();
    List<UnitProgress> unitProgresses = response.getUnitProgressData() == null
        ? Collections.emptyList()
        : response.getUnitProgressData().getUnitProgresses();
    builder.unitProgressList(unitProgresses);
    if (CommandExecutionStatus.SUCCESS == response.getCommandExecutionStatus()) {
      builder.status(Status.SUCCEEDED);
      cloudformationConfigDAL.deleteCloudformationStack(ambiance,
          helper.generateFullIdentifier(
              ParameterFieldHelper.getParameterFieldValue(parameters.getProvisionerIdentifier()), ambiance));
    } else {
      builder.status(Status.FAILED);
    }
    return builder.build();
  }

  @Override
  public TaskRequest obtainTaskAfterRbac(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    CloudformationDeleteStackStepParameters parameters =
        (CloudformationDeleteStackStepParameters) stepParameters.getSpec();
    CloudformationDeleteStackStepConfigurationSpec configuration = parameters.getConfiguration().getSpec();
    log.info("Starting execution Obtain Task after Rbac for the DeleteStack Step");
    parameters.getConfiguration().getSpec().validateParams();
    String accountId = AmbianceUtils.getAccountId(ambiance);
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);

    String entityId = helper.generateFullIdentifier(
        ParameterFieldHelper.getParameterFieldValue(parameters.getProvisionerIdentifier()), ambiance);
    CloudformationTaskNGParametersBuilder builder = CloudformationTaskNGParameters.builder();
    ConnectorInfoDTO connectorIntoDTO = helper.getConnectorDTO(configuration.getConnectorRef().getValue(), ambiance);
    AwsConnectorDTO connectorDTO = (AwsConnectorDTO) connectorIntoDTO.getConnectorConfig();
    if (configuration.getRoleArn().getValue() != null) {
      builder.cloudFormationRoleArn(helper.renderValue(ambiance, configuration.getRoleArn().getValue()));
    }
    BaseNGAccess baseNGAccess = awsHelper.getBaseNGAccess(accountId, orgIdentifier, projectIdentifier);
    List<EncryptedDataDetail> encryptionDetails = awsHelper.getAwsEncryptionDetails(connectorDTO, baseNGAccess);
    builder.accountId(accountId)
        .entityId(entityId)
        .taskType(CloudformationTaskType.DELETE_STACK)
        .cfCommandUnit(CloudformationCommandUnit.DeleteStack)
        .awsConnector(connectorDTO)
        .region(configuration.getRegion().getValue())
        .encryptedDataDetails(encryptionDetails)
        .stackNameSuffix(accountId + orgIdentifier + projectIdentifier) // <===== TODO: This is a temporal hack
        .customStackName(configuration.getStackName().getValue());

    TaskData taskData =
        TaskData.builder()
            .async(true)
            .taskType(TaskType.CLOUDFORMATION_TASK_NG.name())
            .timeout(StepUtils.getTimeoutMillis(stepParameters.getTimeout(), CloudformationStepHelper.DEFAULT_TIMEOUT))
            .parameters(new Object[] {builder.build()})
            .build();

    return StepUtils.prepareCDTaskRequest(ambiance, taskData, kryoSerializer,
        Collections.singletonList(CloudformationCommandUnit.DeleteStack.name()),
        TaskType.CLOUDFORMATION_TASK_NG.getDisplayName(), StepUtils.getTaskSelectors(parameters.getDelegateSelectors()),
        stepHelper.getEnvironmentType(ambiance));
  }

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }
}
