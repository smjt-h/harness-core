/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.serverless;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaFunction;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaManifestSchema;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaRollbackResult;
import io.harness.delegate.task.serverless.*;
import io.harness.delegate.task.serverless.request.ServerlessCommandRequest;
import io.harness.delegate.task.serverless.request.ServerlessRollbackRequest;
import io.harness.delegate.task.serverless.response.ServerlessRollbackResponse;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;
import io.harness.serverless.ServerlessCliResponse;
import io.harness.serverless.ServerlessClient;
import io.harness.serverless.ServerlessCommandUnitConstants;
import io.harness.serverless.model.ServerlessAwsLambdaConfig;
import io.harness.serverless.model.ServerlessDelegateTaskParams;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.harness.rule.OwnerRule.ABOSII;
import static org.assertj.core.api.Assertions.assertThat;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doReturn;

@OwnedBy(CDP)
public class ServerlessAwsLambdaRollbackCommandTaskHandlerTest extends CategoryTest {

    @Mock
    private ServerlessTaskHelperBase serverlessTaskHelperBase;
    @Mock private ServerlessInfraConfigHelper serverlessInfraConfigHelper;
    @Mock private ServerlessAwsCommandTaskHelper serverlessAwsCommandTaskHelper;
    @Mock private ILogStreamingTaskClient iLogStreamingTaskClient;
    @Mock private LogCallback initLogCallback;
    @Mock private LogCallback rollbackLogCallback;

    @InjectMocks
    private ServerlessAwsLambdaRollbackCommandTaskHandler serverlessAwsLambdaRollbackCommandTaskHandler;

    private Integer timeout = 10;
    private String accountId = "account";
    private String output = "output";
    private String previousVersionTimeStamp = "123";
    private String service = "serv";
    private String region = "regi";
    private String stage = "stag";
    private String workingDir = "/asdf/";
    private ServerlessInfraConfig serverlessInfraConfig = ServerlessAwsLambdaInfraConfig.builder().region(region).stage(stage).build();
    private ServerlessManifestConfig serverlessManifestConfig = ServerlessAwsLambdaManifestConfig.builder().build();
    private ServerlessRollbackConfig serverlessRollbackConfig = ServerlessAwsLambdaRollbackConfig.builder().build();
    private ServerlessCommandRequest serverlessCommandRequest = ServerlessRollbackRequest.builder().timeoutIntervalInMin(timeout)
            .serverlessInfraConfig(serverlessInfraConfig).serverlessManifestConfig(serverlessManifestConfig)
            .serverlessRollbackConfig(serverlessRollbackConfig).accountId(accountId).build();
    private ServerlessDelegateTaskParams serverlessDelegateTaskParams = ServerlessDelegateTaskParams.builder()
            .serverlessClientPath("/qwer").workingDirectory(workingDir).build();
    private CommandUnitsProgress commandUnitsProgress = CommandUnitsProgress.builder().build();

    private ServerlessAwsLambdaConfig serverlessAwsLambdaConfig = ServerlessAwsLambdaConfig.builder().build();
    private ServerlessAwsLambdaManifestSchema serverlessAwsLambdaManifestSchema = ServerlessAwsLambdaManifestSchema.builder()
            .service(service).build();
    private ServerlessCliResponse intiServerlessCliResponse = ServerlessCliResponse.builder().commandExecutionStatus(
            CommandExecutionStatus.SUCCESS).output(output).build();
    private ServerlessCliResponse rollbackServerlessCliResponse = ServerlessCliResponse.builder().commandExecutionStatus(
            CommandExecutionStatus.SUCCESS).build();
    private List<ServerlessAwsLambdaFunction> serverlessAwsLambdaFunctionsList = new ArrayList<>();


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @Owner(developers = ABOSII)
    @Category(UnitTests.class)
    public void executeTaskInternalTest() throws Exception{

        doReturn(initLogCallback).when(serverlessTaskHelperBase).getLogCallback(iLogStreamingTaskClient, ServerlessCommandUnitConstants
                .init.toString(), true, commandUnitsProgress);
        doReturn(rollbackLogCallback).when(serverlessTaskHelperBase).getLogCallback(iLogStreamingTaskClient, ServerlessCommandUnitConstants
                .rollback.toString(), true, commandUnitsProgress);
        doReturn(serverlessAwsLambdaConfig).when(serverlessInfraConfigHelper).createServerlessConfig(serverlessInfraConfig);
        doReturn(serverlessAwsLambdaManifestSchema).when(serverlessAwsCommandTaskHelper).parseServerlessManifest(
                (ServerlessAwsLambdaManifestConfig) serverlessManifestConfig);

        ServerlessClient serverlessClient = ServerlessClient.client(serverlessDelegateTaskParams.getServerlessClientPath());

        doReturn(intiServerlessCliResponse).when(serverlessAwsCommandTaskHelper).deployList(serverlessClient,
                serverlessDelegateTaskParams, initLogCallback, (ServerlessAwsLambdaInfraConfig) serverlessInfraConfig,
                serverlessCommandRequest.getTimeoutIntervalInMin() * 60000);
        doReturn(Optional.of(previousVersionTimeStamp)).when(serverlessAwsCommandTaskHelper).getPreviousVersionTimeStamp(output);

        doReturn(rollbackServerlessCliResponse).when(serverlessAwsCommandTaskHelper).rollback(serverlessClient, serverlessDelegateTaskParams,
                rollbackLogCallback, (ServerlessAwsLambdaRollbackConfig) serverlessRollbackConfig, (ServerlessAwsLambdaInfraConfig) serverlessInfraConfig,
                serverlessCommandRequest.getTimeoutIntervalInMin() * 60000);

        doReturn(serverlessAwsLambdaFunctionsList).when(serverlessAwsCommandTaskHelper).fetchFunctionOutputFromCloudFormationTemplate(any());

        ServerlessAwsLambdaRollbackResult serverlessAwsLambdaRollbackResult =
                ServerlessAwsLambdaRollbackResult.builder().service(service).region(region).stage(stage)
                        .functions(serverlessAwsLambdaFunctionsList).build();

        ServerlessRollbackResponse serverlessRollbackResponse = (ServerlessRollbackResponse) serverlessAwsLambdaRollbackCommandTaskHandler.executeTaskInternal(serverlessCommandRequest,
                serverlessDelegateTaskParams, iLogStreamingTaskClient, commandUnitsProgress);

        assertThat(serverlessRollbackResponse.getServerlessRollbackResult()).isEqualTo(serverlessAwsLambdaRollbackResult);
        assertThat(serverlessRollbackResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    }


}
