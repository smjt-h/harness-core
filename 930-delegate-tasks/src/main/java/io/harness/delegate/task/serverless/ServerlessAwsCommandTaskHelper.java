/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaCloudFormationSchema;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaFunction;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaFunction.ServerlessAwsLambdaFunctionBuilder;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaManifestSchema;
import io.harness.filesystem.FileIo;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.serializer.YamlUtils;
import io.harness.serverless.*;
import io.harness.serverless.model.ServerlessAwsLambdaConfig;
import io.harness.serverless.model.ServerlessDelegateTaskParams;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(CDP)
@Singleton
@Slf4j
public class ServerlessAwsCommandTaskHelper {
  @Inject private ServerlessTaskPluginHelper serverlessTaskPluginHelper;

  private static String AWS_LAMBDA_FUNCTION_RESOURCE_TYPE = "AWS::Lambda::Function";
  private static String AWS_LAMBDA_FUNCTION_NAME_PROPERTY_KEY = "FunctionName";
  private static String AWS_LAMBDA_FUNCTION_HANDLER_PROPERTY_KEY = "Handler";
  private static String AWS_LAMBDA_FUNCTION_MEMORY_PROPERTY_KEY = "MemorySize";
  private static String AWS_LAMBDA_FUNCTION_RUNTIME_PROPERTY_KEY = "Runtime";
  private static String AWS_LAMBDA_FUNCTION_TIMEOUT_PROPERTY_KEY = "Timeout";
  private static String CLOUDFORMATION_CREATE_FILE = "cloudformation-template-create-stack.json";
  private static String CLOUDFORMATION_UPDATE_FILE = "cloudformation-template-update-stack.json";
  private static String NEW_LINE_REGEX = "\\r?\\n";
  private static String WHITESPACE_REGEX = "[\\s]";
  private static String DEPLOY_TIMESTAMP_REGEX = ".*Timestamp:\\s([0-9])*";

  public ServerlessCliResponse configCredential(ServerlessClient serverlessClient,
      ServerlessAwsLambdaConfig serverlessAwsLambdaConfig, ServerlessDelegateTaskParams serverlessDelegateTaskParams,
      LogCallback executionLogCallback, boolean overwrite, long timeoutInMillis) throws Exception {
    ConfigCredentialCommand command = serverlessClient.configCredential()
                                          .provider(serverlessAwsLambdaConfig.getProvider())
                                          .key(serverlessAwsLambdaConfig.getAccessKey())
                                          .secret(serverlessAwsLambdaConfig.getSecretKey())
                                          .overwrite(overwrite);
    return ServerlessCommandTaskHelper.executeCommand(
        command, serverlessDelegateTaskParams.getWorkingDirectory(), executionLogCallback, true, timeoutInMillis);
  }

  public ServerlessCliResponse deploy(ServerlessClient serverlessClient,
      ServerlessDelegateTaskParams serverlessDelegateTaskParams, LogCallback executionLogCallback,
      ServerlessAwsLambdaDeployConfig serverlessAwsLambdaDeployConfig,
      ServerlessAwsLambdaInfraConfig serverlessAwsLambdaInfraConfig, long timeoutInMillis) throws Exception {
    DeployCommand command = serverlessClient.deploy()
                                .options(serverlessAwsLambdaDeployConfig.getCommandOptions())
                                .region(serverlessAwsLambdaInfraConfig.getRegion())
                                .stage(serverlessAwsLambdaInfraConfig.getStage());
    return ServerlessCommandTaskHelper.executeCommand(
        command, serverlessDelegateTaskParams.getWorkingDirectory(), executionLogCallback, true, timeoutInMillis);
  }

  public ServerlessCliResponse deployList(ServerlessClient serverlessClient,
      ServerlessDelegateTaskParams serverlessDelegateTaskParams, LogCallback executionLogCallback,
      ServerlessAwsLambdaInfraConfig serverlessAwsLambdaInfraConfig, long timeoutInMillis) throws Exception {
    DeployListCommand command = serverlessClient.deployList()
                                    .region(serverlessAwsLambdaInfraConfig.getRegion())
                                    .stage(serverlessAwsLambdaInfraConfig.getStage());
    return ServerlessCommandTaskHelper.executeCommand(
        command, serverlessDelegateTaskParams.getWorkingDirectory(), executionLogCallback, true, timeoutInMillis);
  }

  public ServerlessCliResponse rollback(ServerlessClient serverlessClient,
      ServerlessDelegateTaskParams serverlessDelegateTaskParams, LogCallback executionLogCallback,
      ServerlessAwsLambdaRollbackConfig serverlessAwsLambdaRollbackConfig,
      ServerlessAwsLambdaInfraConfig serverlessAwsLambdaInfraConfig, long timeoutInMillis) throws Exception {
    RollbackCommand command =
        serverlessClient.rollback().timeStamp(serverlessAwsLambdaRollbackConfig.getPreviousVersionTimeStamp());
    return ServerlessCommandTaskHelper.executeCommand(
        command, serverlessDelegateTaskParams.getWorkingDirectory(), executionLogCallback, true, timeoutInMillis);
  }

  public ServerlessAwsLambdaManifestSchema parseServerlessManifest(
      ServerlessAwsLambdaManifestConfig serverlessManifestConfig) throws IOException {
    String manifestContent = serverlessManifestConfig.getManifestContent();
    YamlUtils yamlUtils = new YamlUtils();
    return yamlUtils.read(manifestContent, ServerlessAwsLambdaManifestSchema.class);
  }

  public boolean installPlugins(ServerlessAwsLambdaManifestSchema serverlessAwsLambdaManifestSchema,
      ServerlessDelegateTaskParams serverlessDelegateTaskParams, LogCallback executionLogCallback,
      ServerlessClient serverlessClient, long timeoutInMillis) throws Exception {
    ServerlessCliResponse response;
    if (EmptyPredicate.isNotEmpty(serverlessAwsLambdaManifestSchema.getPlugins())) {
      List<String> plugins = serverlessAwsLambdaManifestSchema.getPlugins();
      for (String plugin : plugins) {
        response = serverlessTaskPluginHelper.installServerlessPlugin(
            serverlessDelegateTaskParams, serverlessClient, plugin, executionLogCallback, timeoutInMillis);
        if (response.getCommandExecutionStatus() != CommandExecutionStatus.SUCCESS) {
          // todo: error handling
        }
      }
      return true;
    }
    return false;
  }

  public List<ServerlessAwsLambdaFunction> fetchFunctionOutputFromCloudFormationTemplate(
      String cloudFormationTemplateDirectory) throws Exception {
    String cloudFormationTemplatePath =
        Paths.get(cloudFormationTemplateDirectory, CLOUDFORMATION_CREATE_FILE).toString();
    String cloudFormationTemplateContent =
        FileIo.getFileContentsWithSharedLockAcrossProcesses(cloudFormationTemplatePath);
    if (EmptyPredicate.isEmpty(cloudFormationTemplateContent)) {
      cloudFormationTemplatePath = Paths.get(cloudFormationTemplateDirectory, CLOUDFORMATION_UPDATE_FILE).toString();
      cloudFormationTemplateContent = FileIo.getFileContentsWithSharedLockAcrossProcesses(cloudFormationTemplatePath);
    }
    if (EmptyPredicate.isEmpty(cloudFormationTemplateContent)) {
      return Collections.emptyList();
    }
    YamlUtils yamlUtils = new YamlUtils();
    ServerlessAwsLambdaCloudFormationSchema serverlessAwsCloudFormationTemplate =
        yamlUtils.read(cloudFormationTemplateContent, ServerlessAwsLambdaCloudFormationSchema.class);
    Collection<ServerlessAwsLambdaCloudFormationSchema.Resource> resources =
        serverlessAwsCloudFormationTemplate.getResources().values();
    List<Map<String, Object>> functionPropertyMaps =
        resources.stream()
            .filter(resource -> resource.getType().equals(AWS_LAMBDA_FUNCTION_RESOURCE_TYPE))
            .map(ServerlessAwsLambdaCloudFormationSchema.Resource::getProperties)
            .collect(Collectors.toList());
    List<ServerlessAwsLambdaFunction> serverlessAwsLambdaFunctions = new ArrayList<>();
    for (Map<String, Object> functionPropertyMap : functionPropertyMaps) {
      if (!functionPropertyMap.containsKey(AWS_LAMBDA_FUNCTION_NAME_PROPERTY_KEY)) {
        continue;
      }
      ServerlessAwsLambdaFunctionBuilder serverlessAwsLambdaFunctionBuilder = ServerlessAwsLambdaFunction.builder();
      serverlessAwsLambdaFunctionBuilder.functionName(
          functionPropertyMap.get(AWS_LAMBDA_FUNCTION_NAME_PROPERTY_KEY).toString());
      if (functionPropertyMap.containsKey(AWS_LAMBDA_FUNCTION_MEMORY_PROPERTY_KEY)) {
        serverlessAwsLambdaFunctionBuilder.memorySize(
            functionPropertyMap.get(AWS_LAMBDA_FUNCTION_MEMORY_PROPERTY_KEY).toString());
      }
      if (functionPropertyMap.containsKey(AWS_LAMBDA_FUNCTION_HANDLER_PROPERTY_KEY)) {
        serverlessAwsLambdaFunctionBuilder.handler(
            functionPropertyMap.get(AWS_LAMBDA_FUNCTION_HANDLER_PROPERTY_KEY).toString());
      }
      if (functionPropertyMap.containsKey(AWS_LAMBDA_FUNCTION_RUNTIME_PROPERTY_KEY)) {
        serverlessAwsLambdaFunctionBuilder.runTime(
            functionPropertyMap.get(AWS_LAMBDA_FUNCTION_RUNTIME_PROPERTY_KEY).toString());
      }
      if (functionPropertyMap.containsKey(AWS_LAMBDA_FUNCTION_TIMEOUT_PROPERTY_KEY)) {
        serverlessAwsLambdaFunctionBuilder.timeout(
            functionPropertyMap.get(AWS_LAMBDA_FUNCTION_TIMEOUT_PROPERTY_KEY).toString());
      }
      serverlessAwsLambdaFunctions.add(serverlessAwsLambdaFunctionBuilder.build());
    }
    return serverlessAwsLambdaFunctions;
  }

  public Optional<String> getPreviousVersionTimeStamp(String deployListOutput) {
    if (EmptyPredicate.isEmpty(deployListOutput)) {
      return Optional.empty();
    }
    Pattern deployTimeOutPattern = Pattern.compile(DEPLOY_TIMESTAMP_REGEX);
    List<String> outputLines = Arrays.asList(deployListOutput.split(NEW_LINE_REGEX));
    List<String> filteredOutputLines = outputLines.stream()
                                           .filter(outputLine -> deployTimeOutPattern.matcher(outputLine).matches())
                                           .collect(Collectors.toList());
    String lastOutputLine = Iterables.getLast(filteredOutputLines, " ");
    String lastVersionTimeStamp = Iterables.getLast(Arrays.asList(lastOutputLine.split(WHITESPACE_REGEX)), "");
    if (StringUtils.isNotBlank(lastVersionTimeStamp)) {
      return Optional.of(lastVersionTimeStamp);
    }
    return Optional.empty();
  }
}
