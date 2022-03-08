/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.serverless;

import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.serverless.ServerlessAwsDeployResult;
import io.harness.delegate.task.serverless.*;
import io.harness.delegate.task.serverless.request.ServerlessCommandRequest;
import io.harness.delegate.task.serverless.request.ServerlessDeployRequest;
import io.harness.delegate.task.serverless.response.ServerlessCommandResponse;
import io.harness.delegate.task.serverless.response.ServerlessDeployResponse;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.serverless.ServerlessClient;
import io.harness.serverless.ServerlessCommandUnitConstants;
import io.harness.serverless.model.ServerlessAwsLambdaConfig;
import io.harness.serverless.model.ServerlessDelegateTaskParams;

import com.google.inject.Inject;
import java.nio.file.Paths;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
@NoArgsConstructor
public class ServerlessAwsDeployCommandTaskHandler extends ServerlessCommandTaskHandler {
  @Inject private ServerlessTaskHelperBase serverlessTaskHelperBase;
  @Inject private ServerlessInfraConfigHelper serverlessInfraConfigHelper;
  @Inject private ServerlessAwsCommandTaskHelper serverlessAwsCommandTaskHelper;

  private ServerlessAwsLambdaConfig serverlessAwsLambdaConfig;
  private ServerlessClient serverlessClient;
  private ServerlessAwsLambdaManifestConfig serverlessManifestConfig;

  private static final String HOME_DIRECTORY = "./repository/serverless/home";
  // todo: need to move to constants file
  @Override
  protected ServerlessCommandResponse executeTaskInternal(ServerlessCommandRequest serverlessCommandRequest,
      ServerlessDelegateTaskParams serverlessDelegateTaskParams, ILogStreamingTaskClient iLogStreamingTaskClient,
      CommandUnitsProgress commandUnitsProgress) throws Exception {
    if (!(serverlessCommandRequest instanceof ServerlessDeployRequest)) {
      throw new InvalidArgumentsException(
          Pair.of("serverlessCommandRequest", "Must be instance of ServerlessDeployRequest"));
    }

    ServerlessDeployRequest serverlessDeployRequest = (ServerlessDeployRequest) serverlessCommandRequest;
    if (!(serverlessDeployRequest.getServerlessInfraConfig() instanceof ServerlessAwsLambdaInfraConfig)) {
      throw new InvalidArgumentsException(
          Pair.of("ServerlessInfraConfig", "Must be instance of ServerlessAwsLambdaInfraConfig"));
    }
    if (!(serverlessDeployRequest.getServerlessManifestConfig() instanceof ServerlessAwsLambdaManifestConfig)) {
      throw new InvalidArgumentsException(
          Pair.of("ServerlessManifestConfig", "Must be instance of ServerlessAwsLambdaManifestConfig"));
    }
    if (!(serverlessDeployRequest.getServerlessDeployConfig() instanceof ServerlessAwsLambdaDeployConfig)) {
      throw new InvalidArgumentsException(
          Pair.of("ServerlessDeployConfig", "Must be instance of ServerlessAwsLambdaDeployConfig"));
    }
    // todo: instance check for other configs
    LogCallback initLogCallback = serverlessTaskHelperBase.getLogCallback(
        iLogStreamingTaskClient, ServerlessCommandUnitConstants.init.toString(), true, commandUnitsProgress);
    init(serverlessDeployRequest, initLogCallback, serverlessDelegateTaskParams);

    LogCallback deployLogCallback = serverlessTaskHelperBase.getLogCallback(
        iLogStreamingTaskClient, ServerlessCommandUnitConstants.deploy.toString(), true, commandUnitsProgress);
    ServerlessAwsDeployResult serverlessAwsDeployResult =
        deploy(serverlessDeployRequest, deployLogCallback, serverlessDelegateTaskParams);
    return ServerlessDeployResponse.builder()
        .serverlessDeployResult(serverlessAwsDeployResult)
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .build();
  }

  private void init(ServerlessDeployRequest serverlessDeployRequest, LogCallback executionLogCallback,
      ServerlessDelegateTaskParams serverlessDelegateTaskParams) throws Exception {
    executionLogCallback.saveExecutionLog("Initializing..\n");
    String homeDirectory = Paths.get(HOME_DIRECTORY, convertBase64UuidToCanonicalForm(generateUuid()))
                               .normalize()
                               .toAbsolutePath()
                               .toString();
    serverlessTaskHelperBase.createHomeDirectory(homeDirectory);
    serverlessManifestConfig =
        (ServerlessAwsLambdaManifestConfig) serverlessDeployRequest.getServerlessManifestConfig();
    serverlessTaskHelperBase.fetchManifestFilesAndWriteToDirectory(serverlessManifestConfig,
        serverlessDeployRequest.getAccountId(), executionLogCallback, serverlessDelegateTaskParams);
    serverlessTaskHelperBase.replaceManifestWithRenderedContent(serverlessDelegateTaskParams, serverlessManifestConfig);
    serverlessAwsLambdaConfig = (ServerlessAwsLambdaConfig) serverlessInfraConfigHelper.createServerlessConfig(
        serverlessDeployRequest.getServerlessInfraConfig());
    serverlessClient = ServerlessClient.client(serverlessDelegateTaskParams.getServerlessClientPath(), homeDirectory);
    boolean success = serverlessAwsCommandTaskHelper.configCredential(
        serverlessClient, serverlessAwsLambdaConfig, serverlessDelegateTaskParams, executionLogCallback, true);
    if (success == false) {
      // todo: handle failure case
    }
  }

  private ServerlessAwsDeployResult deploy(ServerlessDeployRequest serverlessDeployRequest,
      LogCallback executionLogCallback, ServerlessDelegateTaskParams serverlessDelegateTaskParams) throws Exception {
    executionLogCallback.saveExecutionLog("Deploying..\n");
    ServerlessAwsLambdaDeployConfig serverlessAwsDeployConfig =
        (ServerlessAwsLambdaDeployConfig) serverlessDeployRequest.getServerlessDeployConfig();
    return serverlessAwsCommandTaskHelper.deploy(
        serverlessClient, serverlessDelegateTaskParams, executionLogCallback, serverlessAwsDeployConfig);
  }
}
