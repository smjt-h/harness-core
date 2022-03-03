/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.cloudformation.cloudformationtaskhandler;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.cloudformation.CloudformationBaseHelperImpl.CLOUDFORMATION_STACK_CREATE_URL;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.beans.NameValuePair;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCreateStackRequest;
import software.wings.helpers.ext.cloudformation.request.CloudformationTaskInternalRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackInternalResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackResponse.CloudFormationCreateStackResponseBuilder;
import software.wings.helpers.ext.cloudformation.response.CloudFormationRollbackInfo;
import software.wings.helpers.ext.cloudformation.response.CloudFormationRollbackInfo.CloudFormationRollbackInfoBuilder;
import software.wings.helpers.ext.cloudformation.response.CloudformationTaskInternalResponse;
import software.wings.service.mappers.artifact.AwsConfigToInternalMapper;

import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;

@Singleton
@NoArgsConstructor
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class CloudFormationCreateStackHandler extends CloudFormationCommandTaskHandler {
  @Override
  protected CloudFormationCommandExecutionResponse executeInternal(CloudFormationCommandRequest request,
      List<EncryptedDataDetail> details, ExecutionLogCallback executionLogCallback) {
    AwsConfig awsConfig = request.getAwsConfig();
    encryptionService.decrypt(awsConfig, details, false);

    CloudFormationCreateStackRequest upsertRequest = (CloudFormationCreateStackRequest) request;
    executionLogCallback.saveExecutionLog("# Checking if stack already exists...");
    Optional<Stack> stackOptional =
        getIfStackExists(upsertRequest.getCustomStackName(), upsertRequest.getStackNameSuffix(),
            AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig), request.getRegion());

    if (!stackOptional.isPresent()) {
      executionLogCallback.saveExecutionLog("# Stack does not exist, creating new stack");
      return createStack(upsertRequest, executionLogCallback);
    } else {
      Stack stack = stackOptional.get();
      if (StackStatus.ROLLBACK_COMPLETE.name().equals(stack.getStackStatus())) {
        executionLogCallback.saveExecutionLog(
            format("# Stack already exists and is in %s state.", stack.getStackStatus()));
        executionLogCallback.saveExecutionLog(format("# Deleting stack %s", stack.getStackName()));
        CloudFormationCommandExecutionResponse deleteStackCommandExecutionResponse =
            deleteStack(stack.getStackId(), stack.getStackName(), request, executionLogCallback);
        if (SUCCESS.equals(deleteStackCommandExecutionResponse.getCommandExecutionStatus())) {
          executionLogCallback.saveExecutionLog(
              format("# Stack %s deleted successfully now creating a new stack", stack.getStackName()));
          return createStack(upsertRequest, executionLogCallback);
        }
        executionLogCallback.saveExecutionLog(format(
            "# Stack %s deletion failed, stack creation/updation will not proceed.\n Go to Aws Console and delete the stack",
            stack.getStackName()));
        return deleteStackCommandExecutionResponse;
      } else {
        executionLogCallback.saveExecutionLog("# Stack already exist, updating stack");
        return updateStack(upsertRequest, stack, executionLogCallback);
      }
    }
  }

  private CloudFormationCommandExecutionResponse updateStack(
      CloudFormationCreateStackRequest updateRequest, Stack stack, ExecutionLogCallback executionLogCallback) {
    CloudformationTaskInternalRequest cloudformationTaskInternalRequest =
        getCloudformationTaskInternalRequest(updateRequest);
    CloudformationTaskInternalResponse cloudformationTaskInternalResponse =
        cloudformationBaseHelper.updateStack(cloudformationTaskInternalRequest, stack, executionLogCallback);
    return getCloudFormationCommandExecutionResponse(updateRequest, cloudformationTaskInternalResponse);
  }

  private CloudFormationCommandExecutionResponse createStack(
      CloudFormationCreateStackRequest createRequest, ExecutionLogCallback executionLogCallback) {
    CloudformationTaskInternalRequest cloudformationTaskInternalRequest =
        getCloudformationTaskInternalRequest(createRequest);
    CloudformationTaskInternalResponse cloudformationTaskInternalResponse =
        cloudformationBaseHelper.createStack(cloudformationTaskInternalRequest, executionLogCallback);
    return getCloudFormationCommandExecutionResponse(createRequest, cloudformationTaskInternalResponse);
  }

  private CloudformationTaskInternalRequest getCloudformationTaskInternalRequest(
      CloudFormationCreateStackRequest updateRequest) {
    return CloudformationTaskInternalRequest.builder()
        .accountId(updateRequest.getAccountId())
        .activityId(updateRequest.getActivityId())
        .appId(updateRequest.getAppId())
        .awsConfig(AwsConfigToInternalMapper.toAwsInternalConfig(updateRequest.getAwsConfig()))
        .createType(updateRequest.getCreateType())
        .cloudFormationRoleArn(updateRequest.getCloudFormationRoleArn())
        .commandName(updateRequest.getCommandName())
        .timeoutInMs(updateRequest.getTimeoutInMs())
        .capabilities(updateRequest.getCapabilities())
        .customStackName(updateRequest.getCustomStackName())
        .data(updateRequest.getData())
        .stackNameSuffix(updateRequest.getStackNameSuffix())
        .encryptedVariables(updateRequest.getEncryptedVariables())
        .region(updateRequest.getRegion())
        .skipWaitForResources(updateRequest.isSkipWaitForResources())
        .tags(updateRequest.getTags())
        .stackStatusesToMarkAsSuccess(updateRequest.getStackStatusesToMarkAsSuccess())
        .variables(updateRequest.getVariables())
        .build();
  }

  private CloudFormationCommandExecutionResponse getCloudFormationCommandExecutionResponse(
      CloudFormationCreateStackRequest createRequest,
      CloudformationTaskInternalResponse cloudformationTaskInternalResponse) {
    return CloudFormationCommandExecutionResponse.builder()
        .commandExecutionStatus(cloudformationTaskInternalResponse.getCommandExecutionStatus())
        .commandResponse(getCloudFormationCreateStackResponse(
            createRequest, cloudformationTaskInternalResponse.getCloudFormationCreateStackInternalResponse()))
        .errorMessage(cloudformationTaskInternalResponse.getErrorMessage())
        .commandExecutionStatus(cloudformationTaskInternalResponse.getCommandExecutionStatus())
        .build();
  }

  private CloudFormationCreateStackResponse getCloudFormationCreateStackResponse(
      CloudFormationCreateStackRequest createRequest,
      CloudFormationCreateStackInternalResponse cloudFormationCreateStackInternalResponse) {
    CloudFormationCreateStackResponseBuilder builder =
        CloudFormationCreateStackResponse.builder()
            .stackId(cloudFormationCreateStackInternalResponse.getStackId())
            .cloudFormationOutputMap(cloudFormationCreateStackInternalResponse.getCloudFormationOutputMap())
            .commandExecutionStatus(cloudFormationCreateStackInternalResponse.getCommandExecutionStatus())
            .existingStackInfo(cloudFormationCreateStackInternalResponse.getExistingStackInfo())
            .output(cloudFormationCreateStackInternalResponse.getOutput())
            .stackStatus(cloudFormationCreateStackInternalResponse.getStackStatus());
    if (SUCCESS.equals(cloudFormationCreateStackInternalResponse.getCommandExecutionStatus())) {
      builder.rollbackInfo(getRollbackInfo(createRequest));
    }
    return builder.build();
  }

  private CloudFormationRollbackInfo getRollbackInfo(
      CloudFormationCreateStackRequest cloudFormationCreateStackRequest) {
    CloudFormationRollbackInfoBuilder builder = CloudFormationRollbackInfo.builder();

    builder.cloudFormationRoleArn(cloudFormationCreateStackRequest.getCloudFormationRoleArn());
    if (CLOUDFORMATION_STACK_CREATE_URL.equals(cloudFormationCreateStackRequest.getCreateType())) {
      cloudFormationCreateStackRequest.setData(
          awsCFHelperServiceDelegate.normalizeS3TemplatePath(cloudFormationCreateStackRequest.getData()));
      builder.url(cloudFormationCreateStackRequest.getData());
    } else {
      // handles the case of both Git and body
      builder.body(cloudFormationCreateStackRequest.getData());
    }
    builder.region(cloudFormationCreateStackRequest.getRegion());
    builder.customStackName(cloudFormationCreateStackRequest.getCustomStackName());
    List<NameValuePair> variables = newArrayList();
    if (isNotEmpty(cloudFormationCreateStackRequest.getVariables())) {
      for (Entry<String, String> variable : cloudFormationCreateStackRequest.getVariables().entrySet()) {
        variables.add(new NameValuePair(variable.getKey(), variable.getValue(), Type.TEXT.name()));
      }
    }
    if (isNotEmpty(cloudFormationCreateStackRequest.getEncryptedVariables())) {
      for (Entry<String, EncryptedDataDetail> encVariable :
          cloudFormationCreateStackRequest.getEncryptedVariables().entrySet()) {
        variables.add(new NameValuePair(
            encVariable.getKey(), encVariable.getValue().getEncryptedData().getUuid(), Type.ENCRYPTED_TEXT.name()));
      }
    }

    if (isNotEmpty(cloudFormationCreateStackRequest.getStackStatusesToMarkAsSuccess())) {
      builder.skipBasedOnStackStatus(true);
      builder.stackStatusesToMarkAsSuccess(cloudFormationCreateStackRequest.getStackStatusesToMarkAsSuccess()
                                               .stream()
                                               .map(status -> status.name())
                                               .collect(Collectors.toList()));
    } else {
      builder.skipBasedOnStackStatus(false);
      builder.stackStatusesToMarkAsSuccess(new ArrayList<>());
    }

    builder.variables(variables);
    return builder.build();
  }
}
