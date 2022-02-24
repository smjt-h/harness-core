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
import io.harness.delegate.beans.serverless.ServerlessAwsCloudFormationTemplateSchema;
import io.harness.delegate.beans.serverless.ServerlessAwsDeployResult;
import io.harness.delegate.beans.serverless.ServerlessAwsManifestSchema;
import io.harness.logging.LogCallback;
import io.harness.serializer.YamlUtils;
import io.harness.serverless.*;
import io.harness.serverless.model.ServerlessAwsConfig;
import io.harness.serverless.model.ServerlessDelegateTaskParams;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessResult;

@OwnedBy(CDP)
@Singleton
@Slf4j
public class ServerlessAwsCommandTaskHelper {
  @Inject private ServerlessCommandTaskHelper serverlessCommandTaskHelper;
  @Inject private ServerlessTaskPluginHelper serverlessTaskPluginHelper;

  private static String AWS_LAMBDA_FUNCTION_RESOURCE_TYPE = "AWS::Lambda::Function";
  private static String AWS_LAMBDA_FUNCTION_NAME_PROPERTY_KEY = "FunctionName";

  public boolean setServerlessAwsConfigCredentials(ServerlessClient serverlessClient,
      ServerlessAwsConfig serverlessAwsConfig, ServerlessDelegateTaskParams serverlessDelegateTaskParams,
      LogCallback executionLogCallback, boolean overwrite) throws Exception {
    ConfigCredentialCommand command = serverlessClient.configCredential()
                                          .provider(serverlessAwsConfig.getProvider())
                                          .key(serverlessAwsConfig.getAccessKey())
                                          .secret(serverlessAwsConfig.getSecretKey())
                                          .overwrite(overwrite);
    ProcessResult result = serverlessCommandTaskHelper.executeCommand(
        command, serverlessDelegateTaskParams.getWorkingDirectory(), executionLogCallback, true);
    if (result.getExitValue() == 0) {
      return true;
    }
    return false;
  }

  public ServerlessAwsDeployResult deploy(ServerlessClient serverlessClient,
      ServerlessDelegateTaskParams serverlessDelegateTaskParams, LogCallback executionLogCallback,
      ServerlessAwsDeployConfig serverlessAwsDeployConfig) throws Exception {
    DeployCommand command = serverlessClient.deploy()
                                .region(serverlessAwsDeployConfig.getRegion())
                                .stage(serverlessAwsDeployConfig.getStage())
                                .forceDeployment(serverlessAwsDeployConfig.isForceDeploymentFlag())
                                .awsS3Accelerate(serverlessAwsDeployConfig.isAwsS3AccelerateFlag())
                                .noAwsS3Accelerate(serverlessAwsDeployConfig.isNoAwsS3AccelerateFlag());
    // todo: add other options for deploy command
    ProcessResult result = serverlessCommandTaskHelper.executeCommand(
        command, serverlessDelegateTaskParams.getWorkingDirectory(), executionLogCallback, true);
    if (result.getExitValue() == 0) {
      // todo: parse result into java object
    }
    // todo: add error handling
    return null;
  }

  public ServerlessAwsManifestSchema parseServerlessManifest(ServerlessManifestConfig serverlessManifestConfig)
      throws IOException {
    String manifestContent = serverlessManifestConfig.getManifestContent();
    YamlUtils yamlUtils = new YamlUtils();
    ServerlessAwsManifestSchema serverlessAwsManifestSchema =
        yamlUtils.read(manifestContent, ServerlessAwsManifestSchema.class);
    return serverlessAwsManifestSchema;
  }

  public boolean installRequiredPlugins(ServerlessAwsManifestSchema serverlessAwsManifestSchema,
      ServerlessDelegateTaskParams serverlessDelegateTaskParams, LogCallback executionLogCallback,
      ServerlessClient serverlessClient) throws Exception {
    // todo: validate serverless manifest
    if (EmptyPredicate.isNotEmpty(serverlessAwsManifestSchema.getPlugins())) {
      List<String> plugins = serverlessAwsManifestSchema.getPlugins();
      for (String plugin : plugins) {
        // todo: print statement
        serverlessTaskPluginHelper.installServerlessPlugin(
            serverlessDelegateTaskParams, serverlessClient, plugin, executionLogCallback);
      }
      return true;
    }
    return false;
  }

  public List<String> fetchFunctionOutputFromCloudFormationTemplate(String cloudFormationTemplateContent)
      throws Exception {
    // todo: validate cloudformation content
    YamlUtils yamlUtils = new YamlUtils();
    if (EmptyPredicate.isNotEmpty(cloudFormationTemplateContent)) {
      ServerlessAwsCloudFormationTemplateSchema serverlessAwsCloudFormationTemplate =
          yamlUtils.read(cloudFormationTemplateContent, ServerlessAwsCloudFormationTemplateSchema.class);
      Collection<ServerlessAwsCloudFormationTemplateSchema.Resource> resources =
          serverlessAwsCloudFormationTemplate.getResources().values();
      List<Map<String, Object>> functionProperties =
          resources.stream()
              .filter(resource -> resource.getType().equals(AWS_LAMBDA_FUNCTION_RESOURCE_TYPE))
              .map(resource -> resource.getProperties())
              .collect(Collectors.toList());
      return functionProperties.stream()
          .filter(properties -> properties.containsKey(AWS_LAMBDA_FUNCTION_NAME_PROPERTY_KEY))
          .map(properties -> properties.get(AWS_LAMBDA_FUNCTION_NAME_PROPERTY_KEY).toString())
          .collect(Collectors.toList());
    }
    return Collections.emptyList();
  }
}
