/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.serverless;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.instance.info.InstanceInfoService;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.serverless.ServerlessRollbackDataOutcome.ServerlessRollbackDataOutcomeBuilder;
import io.harness.cdng.serverless.beans.ServerlessExecutionPassThroughData;
import io.harness.cdng.serverless.beans.ServerlessGitFetchResponsePassThroughData;
import io.harness.cdng.serverless.beans.ServerlessStepExceptionPassThroughData;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.delegate.exception.ServerlessNGException;
import io.harness.delegate.task.serverless.ServerlessCommandType;
import io.harness.delegate.task.serverless.ServerlessDeployConfig;
import io.harness.delegate.task.serverless.ServerlessInfraConfig;
import io.harness.delegate.task.serverless.ServerlessManifestConfig;
import io.harness.delegate.task.serverless.request.ServerlessDeployRequest;
import io.harness.delegate.task.serverless.response.ServerlessDeployResponse;
import io.harness.exception.ExceptionUtils;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskChainExecutableWithRollbackAndRbac;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class ServerlessAwsLambdaDeployAwsLambdaStep
    extends TaskChainExecutableWithRollbackAndRbac implements ServerlessAwsLambdaStepExecutor {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.SERVERLESS_AWS_LAMBDA_DEPLOY.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();
  private final String SERVERLESS_DEPLOY_COMMAND_NAME = "Deploy";
  @Inject private ServerlessStepHelper serverlessStepHelper;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;
  @Inject private InstanceInfoService instanceInfoService;

  @Override
  public void validateResources(Ambiance ambiance, StepElementParameters stepParameters) {
    // nothing
  }

  @Override
  public TaskChainResponse startChainLinkAfterRbac(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    return serverlessStepHelper.startChainLink(this, ambiance, stepParameters);
  }

  @Override
  public TaskChainResponse executeServerlessTask(ManifestOutcome serverlessManifestOutcome, Ambiance ambiance,
      StepElementParameters stepElementParameters, Pair<String, String> manifestFilePathContent,
      ServerlessExecutionPassThroughData executionPassThroughData, boolean shouldOpenFetchFilesLogStream,
      UnitProgressData unitProgressData) {
    InfrastructureOutcome infrastructureOutcome = executionPassThroughData.getInfrastructure();
    ServerlessAwsLambdaDeployStepParameters serverlessDeployStepParameters =
        (ServerlessAwsLambdaDeployStepParameters) stepElementParameters.getSpec();
    String manifestFileOverrideContent =
        serverlessStepHelper.renderManifestContent(ambiance, manifestFilePathContent.getValue());
    final String accountId = AmbianceUtils.getAccountId(ambiance);
    ServerlessCommandType serverlessDeployCommandType =
        serverlessStepHelper.getServerlessDeployCommandType(infrastructureOutcome);
    ServerlessInfraConfig serverlessInfraConfig =
        serverlessStepHelper.getServerlessInfraConfig(infrastructureOutcome, ambiance);
    ServerlessDeployConfig serverlessDeployConfig =
        serverlessStepHelper.getServerlessDeployConfig(serverlessDeployCommandType, serverlessDeployStepParameters);
    ServerlessManifestConfig serverlessManifestConfig = serverlessStepHelper.getServerlessManifestConfig(
        manifestFilePathContent, manifestFileOverrideContent, serverlessManifestOutcome, ambiance);
    ServerlessDeployRequest serverlessDeployRequest =
        ServerlessDeployRequest.builder()
            .commandName(SERVERLESS_DEPLOY_COMMAND_NAME)
            .accountId(accountId)
            .serverlessCommandType(serverlessDeployCommandType)
            .serverlessInfraConfig(serverlessInfraConfig)
            .serverlessDeployConfig(serverlessDeployConfig)
            .serverlessManifestConfig(serverlessManifestConfig)
            .commandUnitsProgress(UnitProgressDataMapper.toCommandUnitsProgress(unitProgressData))
            .build();
    // todo: need to add artifact config and others
    return serverlessStepHelper.queueServerlessTask(
        stepElementParameters, serverlessDeployRequest, ambiance, executionPassThroughData);
  }

  @Override
  public TaskChainResponse executeNextLinkWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseSupplier)
      throws Exception {
    log.info("Calling executeNextLink");
    return serverlessStepHelper.executeNextLink(this, ambiance, stepParameters, passThroughData, responseSupplier);
  }

  @Override
  public StepResponse finalizeExecutionWithSecurityContext(Ambiance ambiance, StepElementParameters stepParameters,
      PassThroughData passThroughData, ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    if (passThroughData instanceof ServerlessGitFetchResponsePassThroughData) {
      return serverlessStepHelper.handleGitTaskFailure((ServerlessGitFetchResponsePassThroughData) passThroughData);
    } else if (passThroughData instanceof ServerlessStepExceptionPassThroughData) {
      return serverlessStepHelper.handleStepExceptionFailure((ServerlessStepExceptionPassThroughData) passThroughData);
    }

    log.info("Finalizing execution with passThroughData: " + passThroughData.getClass().getName());
    ServerlessExecutionPassThroughData serverlessExecutionPassThroughData =
        (ServerlessExecutionPassThroughData) passThroughData;
    ServerlessDeployResponse serverlessDeployResponse;
    ServerlessRollbackDataOutcomeBuilder serverlessDeployOutcomeBuilder = ServerlessRollbackDataOutcome.builder();
    try {
      serverlessDeployResponse = (ServerlessDeployResponse) responseDataSupplier.get();
    } catch (Exception e) {
      ServerlessNGException serverlessException = ExceptionUtils.cause(ServerlessNGException.class, e);
      if (serverlessException == null) {
        log.error("Error while processing serverless task response: {}", e.getMessage(), e);
        return serverlessStepHelper.handleTaskException(ambiance, serverlessExecutionPassThroughData, e);
      }
      serverlessDeployOutcomeBuilder.previousVersionTimeStamp(serverlessException.getPreviousVersionTimeStamp());
      executionSweepingOutputService.consume(ambiance, OutcomeExpressionConstants.SERVERLESS_DEPLOY_OUTCOME,
          serverlessDeployOutcomeBuilder.build(), StepOutcomeGroup.STEP.name());
      log.error("Error while processing serverless task response: {}", e.getMessage(), e);
      return serverlessStepHelper.handleTaskException(ambiance, serverlessExecutionPassThroughData, e);
    }
    serverlessDeployOutcomeBuilder.previousVersionTimeStamp(
        serverlessStepHelper.getPreviousVersionStamp(serverlessDeployResponse));
    StepResponse.StepResponseBuilder stepResponseBuilder =
        StepResponse.builder().unitProgressList(serverlessDeployResponse.getUnitProgressData().getUnitProgresses());
    if (serverlessDeployResponse.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
      executionSweepingOutputService.consume(ambiance, OutcomeExpressionConstants.SERVERLESS_DEPLOY_OUTCOME,
          serverlessDeployOutcomeBuilder.build(), StepOutcomeGroup.STEP.name());
      return ServerlessStepHelper.getFailureResponseBuilder(serverlessDeployResponse, stepResponseBuilder).build();
    }

    serverlessDeployOutcomeBuilder.serviceName(serverlessStepHelper.getServiceName(serverlessDeployResponse));

    List<ServerInstanceInfo> functionInstanceInfos =
        serverlessStepHelper.getFunctionInstanceInfo(serverlessDeployResponse);
    StepResponse.StepOutcome stepOutcome =
        instanceInfoService.saveServerInstancesIntoSweepingOutput(ambiance, functionInstanceInfos);

    return stepResponseBuilder.status(Status.SUCCEEDED)
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(OutcomeExpressionConstants.OUTPUT)
                         .outcome(serverlessDeployOutcomeBuilder.build())
                         .build())
        .stepOutcome(stepOutcome)
        .build();
  }

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }
}
