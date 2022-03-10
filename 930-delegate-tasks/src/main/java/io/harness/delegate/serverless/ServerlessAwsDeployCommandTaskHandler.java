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
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaDeployResult;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaDeployResult.ServerlessAwsLambdaDeployResultBuilder;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaManifestSchema;
import io.harness.delegate.task.serverless.*;
import io.harness.delegate.task.serverless.request.ServerlessCommandRequest;
import io.harness.delegate.task.serverless.request.ServerlessDeployRequest;
import io.harness.delegate.task.serverless.response.ServerlessCommandResponse;
import io.harness.delegate.task.serverless.response.ServerlessDeployResponse;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.serverless.ServerlessCliResponse;
import io.harness.serverless.ServerlessClient;
import io.harness.serverless.ServerlessCommandUnitConstants;
import io.harness.serverless.model.ServerlessAwsLambdaConfig;
import io.harness.serverless.model.ServerlessAwsLambdaFunction;
import io.harness.serverless.model.ServerlessDelegateTaskParams;

import com.google.inject.Inject;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
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
  private ServerlessAwsLambdaManifestSchema serverlessManifestSchema;
  private ServerlessAwsLambdaInfraConfig serverlessAwsLambdaInfraConfig;
  private long timeoutInMillis;
  private String previousDeployTimeStamp;
  private static final String HOME_DIRECTORY = "./repository/serverless/home";

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
    timeoutInMillis = serverlessDeployRequest.getTimeoutIntervalInMin() * 60000;
    serverlessAwsLambdaInfraConfig =
        (ServerlessAwsLambdaInfraConfig) serverlessDeployRequest.getServerlessInfraConfig();
    LogCallback initLogCallback = serverlessTaskHelperBase.getLogCallback(
        iLogStreamingTaskClient, ServerlessCommandUnitConstants.init.toString(), true, commandUnitsProgress);
    init(serverlessDeployRequest, initLogCallback, serverlessDelegateTaskParams);

    LogCallback deployLogCallback = serverlessTaskHelperBase.getLogCallback(
        iLogStreamingTaskClient, ServerlessCommandUnitConstants.deploy.toString(), true, commandUnitsProgress);
    ServerlessAwsLambdaDeployResult serverlessAwsLambdaDeployResult =
        deploy(serverlessDeployRequest, deployLogCallback, serverlessDelegateTaskParams);
    return ServerlessDeployResponse.builder()
        .serverlessDeployResult(serverlessAwsLambdaDeployResult)
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .build();
  }

  private void init(ServerlessDeployRequest serverlessDeployRequest, LogCallback executionLogCallback,
      ServerlessDelegateTaskParams serverlessDelegateTaskParams) throws Exception {
    executionLogCallback.saveExecutionLog("Initializing..\n");
    ServerlessCliResponse response;
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
    serverlessAwsCommandTaskHelper.configCredential(serverlessClient, serverlessAwsLambdaConfig,
        serverlessDelegateTaskParams, executionLogCallback, true, timeoutInMillis);
    serverlessManifestSchema = serverlessAwsCommandTaskHelper.parseServerlessManifest(serverlessManifestConfig);
    serverlessAwsCommandTaskHelper.installPlugins(serverlessManifestSchema, serverlessDelegateTaskParams,
        executionLogCallback, serverlessClient, timeoutInMillis);
    response = serverlessAwsCommandTaskHelper.deployList(serverlessClient, serverlessDelegateTaskParams,
        executionLogCallback, serverlessAwsLambdaInfraConfig, timeoutInMillis);
    if (response.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      Optional<String> previousVersionTimeStamp =
          serverlessAwsCommandTaskHelper.getPreviousVersionTimeStamp(response.getOutput());
      previousDeployTimeStamp = previousVersionTimeStamp.orElse(null);
    }
  }

  private ServerlessAwsLambdaDeployResult deploy(ServerlessDeployRequest serverlessDeployRequest,
      LogCallback executionLogCallback, ServerlessDelegateTaskParams serverlessDelegateTaskParams) throws Exception {
    executionLogCallback.saveExecutionLog("Deploying..\n");
    ServerlessCliResponse response;
    ServerlessAwsLambdaDeployConfig serverlessAwsLambdaDeployConfig =
        (ServerlessAwsLambdaDeployConfig) serverlessDeployRequest.getServerlessDeployConfig();
    response = serverlessAwsCommandTaskHelper.deploy(serverlessClient, serverlessDelegateTaskParams,
        executionLogCallback, serverlessAwsLambdaDeployConfig, serverlessAwsLambdaInfraConfig, timeoutInMillis);
    ServerlessAwsLambdaDeployResultBuilder serverlessAwsLambdaDeployResultBuilder =
        ServerlessAwsLambdaDeployResult.builder();
    serverlessAwsLambdaDeployResultBuilder.service(serverlessManifestSchema.getService());
    serverlessAwsLambdaDeployResultBuilder.region(serverlessAwsLambdaInfraConfig.getRegion());
    serverlessAwsLambdaDeployResultBuilder.stage(serverlessAwsLambdaInfraConfig.getStage());
    serverlessAwsLambdaDeployResultBuilder.previousVersionTimeStamp(previousDeployTimeStamp);
    if (response.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      String outputDirectory =
          Paths.get(serverlessDelegateTaskParams.getWorkingDirectory(), "/.serverless/").toString();
      List<ServerlessAwsLambdaFunction> serverlessAwsLambdaFunctions =
          serverlessAwsCommandTaskHelper.fetchFunctionOutputFromCloudFormationTemplate(outputDirectory);
      serverlessAwsLambdaDeployResultBuilder.functions(serverlessAwsLambdaFunctions);
    } else {
      // todo: set error message and error handling
    }
    return serverlessAwsLambdaDeployResultBuilder.build();
  }
}
