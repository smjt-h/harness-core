/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.serverless;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaDeployResult;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaFunction;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaManifestSchema;
import io.harness.delegate.task.serverless.ServerlessAwsCommandTaskHelper;
import io.harness.delegate.task.serverless.ServerlessAwsLambdaDeployConfig;
import io.harness.delegate.task.serverless.ServerlessAwsLambdaInfraConfig;
import io.harness.delegate.task.serverless.ServerlessAwsLambdaManifestConfig;
import io.harness.delegate.task.serverless.ServerlessDeployConfig;
import io.harness.delegate.task.serverless.ServerlessInfraConfig;
import io.harness.delegate.task.serverless.ServerlessInfraConfigHelper;
import io.harness.delegate.task.serverless.ServerlessManifestConfig;
import io.harness.delegate.task.serverless.ServerlessTaskHelperBase;
import io.harness.delegate.task.serverless.request.ServerlessCommandRequest;
import io.harness.delegate.task.serverless.request.ServerlessDeployRequest;
import io.harness.delegate.task.serverless.response.ServerlessDeployResponse;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;
import io.harness.serverless.ServerlessCliResponse;
import io.harness.serverless.ServerlessClient;
import io.harness.serverless.ServerlessCommandUnitConstants;
import io.harness.serverless.model.ServerlessAwsLambdaConfig;
import io.harness.serverless.model.ServerlessDelegateTaskParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class ServerlessAwsLambdaDeployCommandTaskHandlerTest extends CategoryTest {
  @Mock private ServerlessTaskHelperBase serverlessTaskHelperBase;
  @Mock private ServerlessInfraConfigHelper serverlessInfraConfigHelper;
  @Mock private ServerlessAwsCommandTaskHelper serverlessAwsCommandTaskHelper;
  @Mock private ILogStreamingTaskClient iLogStreamingTaskClient;
  @Mock private LogCallback initLogCallback;
  @Mock private LogCallback deployLogCallback;

  @InjectMocks private ServerlessAwsLambdaDeployCommandTaskHandler serverlessAwsLambdaDeployCommandTaskHandler;

  private Integer timeout = 10;
  private String accountId = "account";
  private String output = "output";
  private String previousVersionTimeStamp = "123";
  private String service = "serv";
  private String region = "regi";
  private String stage = "stag";
  private String workingDir = "/asdf/";
  private ServerlessInfraConfig serverlessInfraConfig =
      ServerlessAwsLambdaInfraConfig.builder().region(region).stage(stage).build();
  private ServerlessManifestConfig serverlessManifestConfig = ServerlessAwsLambdaManifestConfig.builder().build();
  private ServerlessDeployConfig serverlessDeployConfig = ServerlessAwsLambdaDeployConfig.builder().build();
  private ServerlessCommandRequest serverlessCommandRequest = ServerlessDeployRequest.builder()
                                                                  .timeoutIntervalInMin(timeout)
                                                                  .serverlessInfraConfig(serverlessInfraConfig)
                                                                  .serverlessManifestConfig(serverlessManifestConfig)
                                                                  .serverlessDeployConfig(serverlessDeployConfig)
                                                                  .accountId(accountId)
                                                                  .build();
  private ServerlessDelegateTaskParams serverlessDelegateTaskParams =
      ServerlessDelegateTaskParams.builder().serverlessClientPath("/qwer").workingDirectory(workingDir).build();
  private CommandUnitsProgress commandUnitsProgress = CommandUnitsProgress.builder().build();

  private ServerlessAwsLambdaConfig serverlessAwsLambdaConfig = ServerlessAwsLambdaConfig.builder().build();
  private ServerlessAwsLambdaManifestSchema serverlessAwsLambdaManifestSchema =
      ServerlessAwsLambdaManifestSchema.builder().service(service).build();
  private ServerlessCliResponse intiServerlessCliResponse =
      ServerlessCliResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).output(output).build();
  private ServerlessCliResponse deployServerlessCliResponse =
      ServerlessCliResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
  private List<ServerlessAwsLambdaFunction> serverlessAwsLambdaFunctionsList = new ArrayList<>();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void executeTaskInternalTest() throws Exception {
    doReturn(initLogCallback)
        .when(serverlessTaskHelperBase)
        .getLogCallback(
            iLogStreamingTaskClient, ServerlessCommandUnitConstants.init.toString(), true, commandUnitsProgress);
    doReturn(deployLogCallback)
        .when(serverlessTaskHelperBase)
        .getLogCallback(
            iLogStreamingTaskClient, ServerlessCommandUnitConstants.deploy.toString(), true, commandUnitsProgress);
    doReturn(serverlessAwsLambdaConfig).when(serverlessInfraConfigHelper).createServerlessConfig(serverlessInfraConfig);
    doReturn(serverlessAwsLambdaManifestSchema)
        .when(serverlessAwsCommandTaskHelper)
        .parseServerlessManifest(
            initLogCallback, ((ServerlessDeployRequest) serverlessCommandRequest).getManifestContent());

    ServerlessClient serverlessClient = ServerlessClient.client(serverlessDelegateTaskParams.getServerlessClientPath());

    doReturn(intiServerlessCliResponse)
        .when(serverlessAwsCommandTaskHelper)
        .configCredential(serverlessClient, serverlessAwsLambdaConfig, serverlessDelegateTaskParams, initLogCallback,
            true, (long) (serverlessCommandRequest.getTimeoutIntervalInMin() * 60000));

    doReturn(intiServerlessCliResponse)
        .when(serverlessAwsCommandTaskHelper)
        .deployList(serverlessClient, serverlessDelegateTaskParams, initLogCallback,
            (ServerlessAwsLambdaInfraConfig) serverlessInfraConfig,
            (long) (serverlessCommandRequest.getTimeoutIntervalInMin() * 60000),
            (ServerlessAwsLambdaManifestConfig) serverlessManifestConfig);
    doReturn(Optional.of(previousVersionTimeStamp))
        .when(serverlessAwsCommandTaskHelper)
        .getPreviousVersionTimeStamp(any(), any(), any());

    doReturn(deployServerlessCliResponse)
        .when(serverlessAwsCommandTaskHelper)
        .deploy(serverlessClient, serverlessDelegateTaskParams, deployLogCallback,
            (ServerlessAwsLambdaDeployConfig) serverlessDeployConfig,
            (ServerlessAwsLambdaInfraConfig) serverlessInfraConfig,
            (long) (serverlessCommandRequest.getTimeoutIntervalInMin() * 60000),
            (ServerlessAwsLambdaManifestConfig) serverlessManifestConfig);

    doReturn(serverlessAwsLambdaFunctionsList)
        .when(serverlessAwsCommandTaskHelper)
        .fetchFunctionOutputFromCloudFormationTemplate(any());

    ServerlessAwsLambdaDeployResult serverlessAwsLambdaDeployResult =
        ServerlessAwsLambdaDeployResult.builder()
            .service(service)
            .region(region)
            .stage(stage)
            .previousVersionTimeStamp(previousVersionTimeStamp)
            .functions(serverlessAwsLambdaFunctionsList)
            .build();

    ServerlessDeployResponse serverlessDeployResponse =
        (ServerlessDeployResponse) serverlessAwsLambdaDeployCommandTaskHandler.executeTaskInternal(
            serverlessCommandRequest, serverlessDelegateTaskParams, iLogStreamingTaskClient, commandUnitsProgress);

    assertThat(serverlessDeployResponse.getServerlessDeployResult()).isEqualTo(serverlessAwsLambdaDeployResult);
    assertThat(serverlessDeployResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }
}