/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.serverless;

import static io.harness.delegate.task.serverless.exception.ServerlessExceptionConstants.SERVERLESS_FETCH_DEPLOY_OUTPUT_EXPLANATION;
import static io.harness.delegate.task.serverless.exception.ServerlessExceptionConstants.SERVERLESS_FETCH_DEPLOY_OUTPUT_FAILED;
import static io.harness.delegate.task.serverless.exception.ServerlessExceptionConstants.SERVERLESS_FETCH_DEPLOY_OUTPUT_HINT;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaDeployResult;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaDeployResult.ServerlessAwsLambdaDeployResultBuilder;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaFunction;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaManifestSchema;
import io.harness.delegate.exception.ServerlessNGException;
import io.harness.delegate.task.serverless.ServerlessAwsCommandTaskHelper;
import io.harness.delegate.task.serverless.ServerlessAwsLambdaDeployConfig;
import io.harness.delegate.task.serverless.ServerlessAwsLambdaInfraConfig;
import io.harness.delegate.task.serverless.ServerlessAwsLambdaManifestConfig;
import io.harness.delegate.task.serverless.ServerlessInfraConfigHelper;
import io.harness.delegate.task.serverless.ServerlessTaskHelperBase;
import io.harness.delegate.task.serverless.request.ServerlessCommandRequest;
import io.harness.delegate.task.serverless.request.ServerlessDeployRequest;
import io.harness.delegate.task.serverless.response.ServerlessCommandResponse;
import io.harness.delegate.task.serverless.response.ServerlessDeployResponse;
import io.harness.delegate.task.serverless.response.ServerlessDeployResponse.ServerlessDeployResponseBuilder;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.runtime.serverless.ServerlessAwsLambdaRuntimeException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.serverless.ServerlessCliResponse;
import io.harness.serverless.ServerlessClient;
import io.harness.serverless.ServerlessCommandUnitConstants;
import io.harness.serverless.model.ServerlessAwsLambdaConfig;
import io.harness.serverless.model.ServerlessDelegateTaskParams;

import software.wings.beans.LogColor;
import software.wings.beans.LogWeight;
import software.wings.delegatetasks.ExceptionMessageSanitizer;

import com.google.inject.Inject;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(HarnessTeam.CDP)
@NoArgsConstructor
@Slf4j
public class ServerlessAwsLambdaDeployCommandTaskHandler extends ServerlessCommandTaskHandler {
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

    timeoutInMillis = serverlessDeployRequest.getTimeoutIntervalInMin() * 60000;
    serverlessAwsLambdaInfraConfig =
        (ServerlessAwsLambdaInfraConfig) serverlessDeployRequest.getServerlessInfraConfig();
    LogCallback initLogCallback = serverlessTaskHelperBase.getLogCallback(
        iLogStreamingTaskClient, ServerlessCommandUnitConstants.init.toString(), true, commandUnitsProgress);
    try {
      init(serverlessDeployRequest, initLogCallback, serverlessDelegateTaskParams);
    } catch (Exception ex) {
      initLogCallback.saveExecutionLog(color(format("%n Initialization failed."), LogColor.Red, LogWeight.Bold),
          LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      throw ex;
    }

    LogCallback deployLogCallback = serverlessTaskHelperBase.getLogCallback(
        iLogStreamingTaskClient, ServerlessCommandUnitConstants.deploy.toString(), true, commandUnitsProgress);
    try {
      return deploy(serverlessDeployRequest, deployLogCallback, serverlessDelegateTaskParams);
    } catch (Exception ex) {
      deployLogCallback.saveExecutionLog(color(format("%n Deployment failed."), LogColor.Red, LogWeight.Bold),
          LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      throw new ServerlessNGException(ex, previousDeployTimeStamp);
    }
  }

  private void init(ServerlessDeployRequest serverlessDeployRequest, LogCallback executionLogCallback,
      ServerlessDelegateTaskParams serverlessDelegateTaskParams) throws Exception {
    executionLogCallback.saveExecutionLog(format("Initializing..%n%n"));
    ServerlessCliResponse response;

    serverlessManifestConfig =
        (ServerlessAwsLambdaManifestConfig) serverlessDeployRequest.getServerlessManifestConfig();
    serverlessTaskHelperBase.fetchManifestFilesAndWriteToDirectory(serverlessManifestConfig,
        serverlessDeployRequest.getAccountId(), executionLogCallback, serverlessDelegateTaskParams);

    serverlessTaskHelperBase.fetchArtifact(serverlessDeployRequest.getServerlessArtifactConfig(), executionLogCallback,
        serverlessDelegateTaskParams.getWorkingDirectory());

    executionLogCallback.saveExecutionLog(format("Resolving expressions in serverless config file..%n"));
    serverlessManifestSchema = serverlessAwsCommandTaskHelper.parseServerlessManifest(
        executionLogCallback, serverlessDeployRequest.getManifestContent());
    serverlessTaskHelperBase.replaceManifestWithRenderedContent(serverlessDelegateTaskParams, serverlessManifestConfig,
        serverlessDeployRequest.getManifestContent(), serverlessManifestSchema);

    executionLogCallback.saveExecutionLog(color("Successfully resolved with config file content:", White, Bold));
    executionLogCallback.saveExecutionLog(serverlessDeployRequest.getManifestContent());

    serverlessAwsLambdaConfig = (ServerlessAwsLambdaConfig) serverlessInfraConfigHelper.createServerlessConfig(
        serverlessDeployRequest.getServerlessInfraConfig());
    serverlessClient = ServerlessClient.client(serverlessDelegateTaskParams.getServerlessClientPath());

    response = serverlessAwsCommandTaskHelper.configCredential(serverlessClient, serverlessAwsLambdaConfig,
        serverlessDelegateTaskParams, executionLogCallback, true, timeoutInMillis);

    if (response.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      executionLogCallback.saveExecutionLog(
          color(format("%nConfig Credential command executed successfully..%n"), LogColor.White, LogWeight.Bold), INFO);
    } else {
      executionLogCallback.saveExecutionLog(
          color(format("%nConfig Credential command failed..%n"), LogColor.Red, LogWeight.Bold), ERROR,
          CommandExecutionStatus.FAILURE);
      serverlessAwsCommandTaskHelper.handleCommandExecutionFailure(response, serverlessClient.configCredential());
    }

    serverlessAwsCommandTaskHelper.installPlugins(serverlessManifestSchema, serverlessDelegateTaskParams,
        executionLogCallback, serverlessClient, timeoutInMillis, serverlessManifestConfig);

    response = serverlessAwsCommandTaskHelper.deployList(serverlessClient, serverlessDelegateTaskParams,
        executionLogCallback, serverlessAwsLambdaInfraConfig, timeoutInMillis, serverlessManifestConfig);

    if (response.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      executionLogCallback.saveExecutionLog(
          color(format("%nDeploy List command executed successfully..%n"), LogColor.White, LogWeight.Bold), INFO);
      List<String> timeStamps = serverlessAwsCommandTaskHelper.getDeployListTimeStamps(response.getOutput());
      Optional<String> previousVersionTimeStamp = serverlessAwsCommandTaskHelper.getPreviousVersionTimeStamp(
          timeStamps, executionLogCallback, serverlessDeployRequest);
      previousDeployTimeStamp = previousVersionTimeStamp.orElse(null);
    } else {
      executionLogCallback.saveExecutionLog(
          color(format("%nDeploy List command failed..%n"), LogColor.Red, LogWeight.Bold), ERROR);
    }
    executionLogCallback.saveExecutionLog(format("Done..%n"), LogLevel.INFO, CommandExecutionStatus.SUCCESS);
  }

  private ServerlessDeployResponse deploy(ServerlessDeployRequest serverlessDeployRequest,
      LogCallback executionLogCallback, ServerlessDelegateTaskParams serverlessDelegateTaskParams) throws Exception {
    executionLogCallback.saveExecutionLog(format("Deploying..%n%n"));
    ServerlessCliResponse response;

    ServerlessAwsLambdaDeployConfig serverlessAwsLambdaDeployConfig =
        (ServerlessAwsLambdaDeployConfig) serverlessDeployRequest.getServerlessDeployConfig();

    response =
        serverlessAwsCommandTaskHelper.deploy(serverlessClient, serverlessDelegateTaskParams, executionLogCallback,
            serverlessAwsLambdaDeployConfig, serverlessAwsLambdaInfraConfig, timeoutInMillis, serverlessManifestConfig);

    ServerlessAwsLambdaDeployResultBuilder serverlessAwsLambdaDeployResultBuilder =
        ServerlessAwsLambdaDeployResult.builder();
    serverlessAwsLambdaDeployResultBuilder.service(serverlessManifestSchema.getService());
    serverlessAwsLambdaDeployResultBuilder.region(serverlessAwsLambdaInfraConfig.getRegion());
    serverlessAwsLambdaDeployResultBuilder.stage(serverlessAwsLambdaInfraConfig.getStage());
    serverlessAwsLambdaDeployResultBuilder.previousVersionTimeStamp(previousDeployTimeStamp);
    ServerlessDeployResponseBuilder serverlessDeployResponseBuilder = ServerlessDeployResponse.builder();

    if (response.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      String outputDirectory =
          Paths.get(serverlessDelegateTaskParams.getWorkingDirectory(), "/.serverless/").toString();

      try {
        List<ServerlessAwsLambdaFunction> serverlessAwsLambdaFunctions =
            serverlessAwsCommandTaskHelper.fetchFunctionOutputFromCloudFormationTemplate(outputDirectory);
        serverlessAwsLambdaDeployResultBuilder.functions(serverlessAwsLambdaFunctions);
      } catch (Exception e) {
        Exception sanitizedException = ExceptionMessageSanitizer.sanitizeException(e);
        log.error("Failure in fetching serverless deployment output", sanitizedException);
        executionLogCallback.saveExecutionLog(
            format("%nFailed to fetch serverless deployment output: %s", ExceptionUtils.getMessage(sanitizedException)),
            ERROR, CommandExecutionStatus.FAILURE);
        throw NestedExceptionUtils.hintWithExplanationException(SERVERLESS_FETCH_DEPLOY_OUTPUT_HINT,
            SERVERLESS_FETCH_DEPLOY_OUTPUT_EXPLANATION,
            new ServerlessAwsLambdaRuntimeException(SERVERLESS_FETCH_DEPLOY_OUTPUT_FAILED, sanitizedException));
      }

      executionLogCallback.saveExecutionLog(
          color(format("%nDeployment completed successfully..%n"), LogColor.White, LogWeight.Bold), LogLevel.INFO,
          CommandExecutionStatus.SUCCESS);
      executionLogCallback.saveExecutionLog(format("Done..%n"), LogLevel.INFO, CommandExecutionStatus.SUCCESS);
      serverlessDeployResponseBuilder.commandExecutionStatus(CommandExecutionStatus.SUCCESS);
    } else {
      serverlessAwsCommandTaskHelper.handleCommandExecutionFailure(response, serverlessClient.deploy());
    }

    serverlessDeployResponseBuilder.serverlessDeployResult(serverlessAwsLambdaDeployResultBuilder.build());
    return serverlessDeployResponseBuilder.build();
  }
}
