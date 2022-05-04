/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.serverless;

import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.instance.info.InstanceInfoService;
import io.harness.cdng.serverless.beans.ServerlessExecutionPassThroughData;
import io.harness.cdng.serverless.beans.ServerlessGitFetchFailurePassThroughData;
import io.harness.cdng.serverless.beans.ServerlessStepExceptionPassThroughData;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.delegate.beans.instancesync.info.ServerlessAwsLambdaServerInstanceInfo;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaDeployResult;
import io.harness.delegate.exception.ServerlessNGException;
import io.harness.delegate.task.serverless.ServerlessCommandType;
import io.harness.delegate.task.serverless.request.ServerlessDeployRequest;
import io.harness.delegate.task.serverless.response.ServerlessDeployResponse;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.CDP)
public class ServerlessAwsLambdaDeployStepTest extends AbstractServerlessStepExecutorTestBase {
  @Mock private ExecutionSweepingOutputService executionSweepingOutputService;
  @InjectMocks private ServerlessAwsLambdaDeployStep serverlessAwsLambdaDeployStep;
  @Mock private InstanceInfoService instanceInfoService;

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testExecuteTask() {
    ServerlessAwsLambdaDeployStepParameters stepParameters = new ServerlessAwsLambdaDeployStepParameters();

    final StepElementParameters stepElementParameters =
        StepElementParameters.builder().spec(stepParameters).timeout(ParameterField.createValueField("30m")).build();

    ServerlessDeployRequest request = executeTask(stepElementParameters, ServerlessDeployRequest.class);
    assertThat(request.getAccountId()).isEqualTo(accountId);
    assertThat(request.getServerlessInfraConfig()).isEqualTo(serverlessInfraConfig);
    assertThat(request.getServerlessManifestConfig()).isEqualTo(manifestDelegateConfig);
    assertThat(request.getServerlessCommandType()).isEqualTo(ServerlessCommandType.SERVERLESS_AWS_LAMBDA_DEPLOY);
    assertThat(request.getTimeoutIntervalInMin()).isEqualTo(30);
    assertThat(request.getCommandName()).isEqualTo(SERVERLESS_AWS_LAMBDA_DEPLOY_COMMAND_NAME);
    assertThat(request.getServerlessDeployConfig()).isEqualTo(serverlessDeployConfig);
  }

  @SneakyThrows
  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testOutcomesInResponseSuccessTest() {
    ServerlessAwsLambdaDeployStepParameters stepParameters = new ServerlessAwsLambdaDeployStepParameters();
    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(stepParameters).build();

    ServerlessDeployResponse serverlessDeployResponse =
        ServerlessDeployResponse.builder()
            .serverlessDeployResult(ServerlessAwsLambdaDeployResult.builder()
                                        .service("aws")
                                        .region("us-east-01")
                                        .stage("stg")
                                        .previousVersionTimeStamp("31242341")
                                        .functions(Arrays.asList())
                                        .build())
            .unitProgressData(UnitProgressData.builder().unitProgresses(Arrays.asList()).build())
            .commandExecutionStatus(SUCCESS)
            .build();

    StepOutcome stepOutcome = StepOutcome.builder().name("a").build();
    List<ServerInstanceInfo> serverInstanceInfoList =
        Arrays.asList(ServerlessAwsLambdaServerInstanceInfo.builder().build());
    doReturn(serverInstanceInfoList)
        .when(serverlessStepHelper)
        .getFunctionInstanceInfo(serverlessDeployResponse, serverlessAwsLambdaStepHelper);
    doReturn(stepOutcome)
        .when(instanceInfoService)
        .saveServerInstancesIntoSweepingOutput(ambiance, serverInstanceInfoList);

    StepResponse response = serverlessAwsLambdaDeployStep.finalizeExecutionWithSecurityContext(ambiance,
        stepElementParameters, ServerlessExecutionPassThroughData.builder().build(), () -> serverlessDeployResponse);
    assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);
    assertThat(response.getUnitProgressList()).isEqualTo(Arrays.asList());
    assertThat(response.getStepOutcomes()).hasSize(1);

    StepOutcome outcome = response.getStepOutcomes().stream().collect(Collectors.toList()).get(0);
    assertThat(outcome).isEqualTo(stepOutcome);
  }

  @SneakyThrows
  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testOutcomesInResponseWhenGitFetchFailureTest() {
    ServerlessAwsLambdaDeployStepParameters stepParameters = new ServerlessAwsLambdaDeployStepParameters();
    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(stepParameters).build();

    ServerlessDeployResponse serverlessDeployResponse =
        ServerlessDeployResponse.builder()
            .serverlessDeployResult(ServerlessAwsLambdaDeployResult.builder()
                                        .service("aws")
                                        .region("us-east-01")
                                        .stage("stg")
                                        .previousVersionTimeStamp("31242341")
                                        .build())
            .unitProgressData(UnitProgressData.builder().unitProgresses(Arrays.asList()).build())
            .commandExecutionStatus(SUCCESS)
            .build();

    PassThroughData passThroughData = ServerlessGitFetchFailurePassThroughData.builder().build();

    StepResponse stepResponse = StepResponse.builder().status(Status.FAILED).build();
    doReturn(stepResponse)
        .when(serverlessStepHelper)
        .handleGitTaskFailure((ServerlessGitFetchFailurePassThroughData) passThroughData);

    StepResponse response = serverlessAwsLambdaDeployStep.finalizeExecutionWithSecurityContext(
        ambiance, stepElementParameters, passThroughData, () -> serverlessDeployResponse);
    verify(serverlessStepHelper, times(1))
        .handleGitTaskFailure((ServerlessGitFetchFailurePassThroughData) passThroughData);
    assertThat(response).isEqualTo(stepResponse);
  }

  @SneakyThrows
  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testOutcomesInResponseWhenStepExceptionTest() {
    ServerlessAwsLambdaDeployStepParameters stepParameters = new ServerlessAwsLambdaDeployStepParameters();
    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(stepParameters).build();

    ServerlessDeployResponse serverlessDeployResponse =
        ServerlessDeployResponse.builder()
            .serverlessDeployResult(ServerlessAwsLambdaDeployResult.builder()
                                        .service("aws")
                                        .region("us-east-01")
                                        .stage("stg")
                                        .previousVersionTimeStamp("31242341")
                                        .build())
            .unitProgressData(UnitProgressData.builder().unitProgresses(Arrays.asList()).build())
            .commandExecutionStatus(SUCCESS)
            .build();

    PassThroughData passThroughData = ServerlessStepExceptionPassThroughData.builder().build();

    StepResponse stepResponse = StepResponse.builder().status(Status.FAILED).build();
    doReturn(stepResponse)
        .when(serverlessStepHelper)
        .handleStepExceptionFailure((ServerlessStepExceptionPassThroughData) passThroughData);

    StepResponse response = serverlessAwsLambdaDeployStep.finalizeExecutionWithSecurityContext(
        ambiance, stepElementParameters, passThroughData, () -> serverlessDeployResponse);
    verify(serverlessStepHelper, times(1))
        .handleStepExceptionFailure((ServerlessStepExceptionPassThroughData) passThroughData);
    assertThat(response).isEqualTo(stepResponse);
  }

  @SneakyThrows
  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testOutcomesInResponseWhenNoServerlessNGExceptionDuringResponseRetrievalTest() {
    ServerlessAwsLambdaDeployStepParameters stepParameters = new ServerlessAwsLambdaDeployStepParameters();
    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(stepParameters).build();

    StepResponse stepResponse = StepResponse.builder().status(Status.FAILED).build();
    PassThroughData passThroughData = ServerlessExecutionPassThroughData.builder().build();

    Exception e = new Exception();

    doReturn(stepResponse)
        .when(serverlessStepHelper)
        .handleTaskException(ambiance, (ServerlessExecutionPassThroughData) passThroughData, e);
    StepResponse response = serverlessAwsLambdaDeployStep.finalizeExecutionWithSecurityContext(
        ambiance, stepElementParameters, passThroughData, () -> { throw e; });
    verify(serverlessStepHelper, times(1))
        .handleTaskException(ambiance, (ServerlessExecutionPassThroughData) passThroughData, e);
    assertThat(response).isEqualTo(stepResponse);
  }

  @SneakyThrows
  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void testOutcomesInResponseWhenServerlessNGExceptionDuringResponseRetrievalTest() {
    ServerlessAwsLambdaDeployStepParameters stepParameters = new ServerlessAwsLambdaDeployStepParameters();
    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(stepParameters).build();

    StepResponse stepResponse = StepResponse.builder().status(Status.FAILED).build();
    PassThroughData passThroughData = ServerlessExecutionPassThroughData.builder().build();

    Exception e = new ServerlessNGException(new Exception(), "234");

    doReturn(stepResponse)
        .when(serverlessStepHelper)
        .handleTaskException(ambiance, (ServerlessExecutionPassThroughData) passThroughData, e);
    StepResponse response = serverlessAwsLambdaDeployStep.finalizeExecutionWithSecurityContext(
        ambiance, stepElementParameters, passThroughData, () -> { throw e; });
    verify(serverlessStepHelper, times(1))
        .handleTaskException(ambiance, (ServerlessExecutionPassThroughData) passThroughData, e);
    assertThat(response).isEqualTo(stepResponse);
  }

  @Override
  protected ServerlessStepExecutor getServerlessAwsLambdaStepExecutor() {
    return serverlessAwsLambdaDeployStep;
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void validateResources() {
    // no code written
  }
}
