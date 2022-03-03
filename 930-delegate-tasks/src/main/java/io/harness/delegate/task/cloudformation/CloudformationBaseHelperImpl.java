/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.cloudformation;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.threading.Morpheus.sleep;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toMap;

import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.AWSCloudformationClient;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ExceptionUtils;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.cloudformation.request.CloudformationTaskInternalRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackInternalResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackInternalResponse.CloudFormationCreateStackInternalResponseBuilder;
import software.wings.helpers.ext.cloudformation.response.CloudformationTaskInternalResponse;
import software.wings.helpers.ext.cloudformation.response.CloudformationTaskInternalResponse.CloudformationTaskInternalResponseBuilder;
import software.wings.helpers.ext.cloudformation.response.ExistingStackInfo;
import software.wings.helpers.ext.cloudformation.response.ExistingStackInfo.ExistingStackInfoBuilder;
import software.wings.service.intfc.aws.delegate.AwsCFHelperServiceDelegate;
import software.wings.service.intfc.security.EncryptionService;

import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.services.cloudformation.model.UpdateStackResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(CDP)
public class CloudformationBaseHelperImpl implements CloudformationBaseHelper {
  private static final String STACK_NAME_PREFIX = "HarnessStack-";
  public static final String CLOUDFORMATION_STACK_CREATE_URL = "Create URL";
  public static final String CLOUDFORMATION_STACK_CREATE_BODY = "Create Body";
  public static final String CLOUDFORMATION_STACK_CREATE_GIT = "Create GIT";

  @Inject protected EncryptionService encryptionService;
  @Inject protected AWSCloudformationClient awsHelperService;
  @Inject protected AwsCFHelperServiceDelegate awsCFHelperServiceDelegate;

  private final int DEFAULT_TIMEOUT_MS = 10 * 60 * 1000;

  @Override
  public CloudformationTaskInternalResponse createStack(
      CloudformationTaskInternalRequest createRequest, ExecutionLogCallback executionLogCallback) {
    CloudformationTaskInternalResponseBuilder builder = CloudformationTaskInternalResponse.builder();

    String stackName;
    if (isNotEmpty(createRequest.getCustomStackName())) {
      stackName = createRequest.getCustomStackName();
    } else {
      stackName = STACK_NAME_PREFIX + createRequest.getStackNameSuffix();
    }
    try {
      executionLogCallback.saveExecutionLog(format("# Creating stack with name: %s", stackName));
      CreateStackRequest createStackRequest = new CreateStackRequest()
                                                  .withStackName(stackName)
                                                  .withParameters(getCfParams(createRequest))
                                                  .withCapabilities(createRequest.getCapabilities())
                                                  .withTags(getCloudformationTags(createRequest));
      if (EmptyPredicate.isNotEmpty(createRequest.getCloudFormationRoleArn())) {
        createStackRequest.withRoleARN(createRequest.getCloudFormationRoleArn());
      } else {
        executionLogCallback.saveExecutionLog(
            "No specific cloudformation role provided will use the default permissions on delegate.");
      }
      switch (createRequest.getCreateType()) {
        case CLOUDFORMATION_STACK_CREATE_GIT: {
          executionLogCallback.saveExecutionLog("# Using Git Template Body to Create Stack");
          createRequest.setCreateType(CLOUDFORMATION_STACK_CREATE_BODY);
          createStackRequest.withTemplateBody(createRequest.getData());
          createStackRequest.withCapabilities(getCapabilities(createRequest.getAwsConfig(), createRequest.getRegion(),
              createRequest.getData(), createRequest.getCapabilities(), "body"));
          createStackAndWaitWithEvents(createRequest, createStackRequest, builder, executionLogCallback);
          break;
        }
        case CLOUDFORMATION_STACK_CREATE_BODY: {
          executionLogCallback.saveExecutionLog("# Using Template Body to create Stack");
          createStackRequest.withTemplateBody(createRequest.getData());
          createStackRequest.withCapabilities(getCapabilities(createRequest.getAwsConfig(), createRequest.getRegion(),
              createRequest.getData(), createRequest.getCapabilities(), "body"));
          createStackAndWaitWithEvents(createRequest, createStackRequest, builder, executionLogCallback);
          break;
        }
        case CLOUDFORMATION_STACK_CREATE_URL: {
          createRequest.setData(awsCFHelperServiceDelegate.normalizeS3TemplatePath(createRequest.getData()));
          executionLogCallback.saveExecutionLog(
              format("# Using Template Url: [%s] to Create Stack", createRequest.getData()));
          createStackRequest.withTemplateURL(createRequest.getData());
          createStackRequest.withCapabilities(getCapabilities(createRequest.getAwsConfig(), createRequest.getRegion(),
              createRequest.getData(), createRequest.getCapabilities(), "s3"));
          createStackAndWaitWithEvents(createRequest, createStackRequest, builder, executionLogCallback);
          break;
        }
        default: {
          String errorMessage = format("Unsupported stack create type: %s", createRequest.getCreateType());
          executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
          builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
        }
      }
    } catch (Exception ex) {
      String errorMessage = format("Exception: %s while creating stack: %s", ExceptionUtils.getMessage(ex), stackName);
      executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
    }
    return builder.build();
  }

  @Override
  public CloudformationTaskInternalResponse updateStack(
      CloudformationTaskInternalRequest updateRequest, Stack stack, ExecutionLogCallback executionLogCallback) {
    CloudformationTaskInternalResponseBuilder builder = CloudformationTaskInternalResponse.builder();
    try {
      executionLogCallback.saveExecutionLog(format("# Starting to Update stack with name: %s", stack.getStackName()));
      UpdateStackRequest updateStackRequest = new UpdateStackRequest()
                                                  .withStackName(stack.getStackName())
                                                  .withParameters(getCfParams(updateRequest))
                                                  .withCapabilities(updateRequest.getCapabilities())
                                                  .withTags(getCloudformationTags(updateRequest));
      if (EmptyPredicate.isNotEmpty(updateRequest.getCloudFormationRoleArn())) {
        updateStackRequest.withRoleARN(updateRequest.getCloudFormationRoleArn());
      } else {
        executionLogCallback.saveExecutionLog(
            "No specific cloudformation role provided will use the default permissions on delegate.");
      }
      switch (updateRequest.getCreateType()) {
        case CLOUDFORMATION_STACK_CREATE_GIT: {
          executionLogCallback.saveExecutionLog("# Using Git Template Body to Update Stack");
          updateRequest.setCreateType(CLOUDFORMATION_STACK_CREATE_BODY);
          updateStackRequest.withTemplateBody(updateRequest.getData());
          updateStackRequest.withCapabilities(getCapabilities(updateRequest.getAwsConfig(), updateRequest.getRegion(),
              updateRequest.getData(), updateRequest.getCapabilities(), "body"));
          updateStackAndWaitWithEvents(updateRequest, updateStackRequest, builder, stack, executionLogCallback);
          break;
        }
        case CLOUDFORMATION_STACK_CREATE_BODY: {
          executionLogCallback.saveExecutionLog("# Using Template Body to Update Stack");
          updateStackRequest.withTemplateBody(updateRequest.getData());
          updateStackRequest.withCapabilities(getCapabilities(updateRequest.getAwsConfig(), updateRequest.getRegion(),
              updateRequest.getData(), updateRequest.getCapabilities(), "body"));
          updateStackAndWaitWithEvents(updateRequest, updateStackRequest, builder, stack, executionLogCallback);
          break;
        }
        case CLOUDFORMATION_STACK_CREATE_URL: {
          updateRequest.setData(awsCFHelperServiceDelegate.normalizeS3TemplatePath(updateRequest.getData()));
          executionLogCallback.saveExecutionLog(
              format("# Using Template Url: [%s] to Update Stack", updateRequest.getData()));
          updateStackRequest.withTemplateURL(updateRequest.getData());
          updateStackRequest.withCapabilities(getCapabilities(updateRequest.getAwsConfig(), updateRequest.getRegion(),
              updateRequest.getData(), updateRequest.getCapabilities(), "s3"));
          updateStackAndWaitWithEvents(updateRequest, updateStackRequest, builder, stack, executionLogCallback);
          break;
        }
        default: {
          String errorMessage = format("# Unsupported stack create type: %s", updateRequest.getCreateType());
          executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
          builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
        }
      }
    } catch (Exception ex) {
      String errorMessage =
          format("# Exception: %s while Updating stack: %s", ExceptionUtils.getMessage(ex), stack.getStackName());
      executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
    }
    CloudformationTaskInternalResponse cloudformationTaskInternalResponse = builder.build();
    if (!SUCCESS.equals(cloudformationTaskInternalResponse.getCommandExecutionStatus())
        && cloudformationTaskInternalResponse.getCloudFormationCreateStackInternalResponse() != null) {
      String responseStackStatus =
          cloudformationTaskInternalResponse.getCloudFormationCreateStackInternalResponse().getStackStatus();
      if (responseStackStatus != null && isNotEmpty(updateRequest.getStackStatusesToMarkAsSuccess())) {
        boolean hasMatchingStatusToBeTreatedAsSuccess =
            updateRequest.getStackStatusesToMarkAsSuccess().stream().anyMatch(
                status -> status.name().equals(responseStackStatus));
        if (hasMatchingStatusToBeTreatedAsSuccess) {
          builder.commandExecutionStatus(SUCCESS);
          cloudformationTaskInternalResponse.getCloudFormationCreateStackInternalResponse().setCommandExecutionStatus(
              SUCCESS);
          builder.cloudFormationCreateStackInternalResponse(
              cloudformationTaskInternalResponse.getCloudFormationCreateStackInternalResponse());
        }
      }
    }
    return builder.build();
  }

  @Override
  public CloudformationTaskInternalResponse deleteStack(String stackId, String stackName,
      CloudformationTaskInternalRequest request, ExecutionLogCallback executionLogCallback) {
    CloudformationTaskInternalResponseBuilder builder = CloudformationTaskInternalResponse.builder();
    Stack stack;
    try {
      long stackEventsTs = System.currentTimeMillis();
      executionLogCallback.saveExecutionLog(String.format("# Starting to delete stack: %s", stackName));
      DeleteStackRequest deleteStackRequest = new DeleteStackRequest().withStackName(stackId);
      if (EmptyPredicate.isNotEmpty(request.getCloudFormationRoleArn())) {
        deleteStackRequest.withRoleARN(request.getCloudFormationRoleArn());
      } else {
        executionLogCallback.saveExecutionLog(
            "No specific cloudformation role provided will use the default permissions on delegate.");
      }
      awsHelperService.deleteStack(request.getRegion(), deleteStackRequest, request.getAwsConfig());
      if (!request.isSkipWaitForResources()) {
        sleep(ofSeconds(30));
      }

      executionLogCallback.saveExecutionLog(
          String.format("# Request to delete stack: %s submitted. Now beginning to poll.", stackName));
      int timeOutMs = request.getTimeoutInMs() > 0 ? request.getTimeoutInMs() : DEFAULT_TIMEOUT_MS;
      long endTime = System.currentTimeMillis() + timeOutMs;
      boolean done = false;

      while (System.currentTimeMillis() < endTime && !done) {
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(stackId);
        List<Stack> stacks =
            awsHelperService.getAllStacks(request.getRegion(), describeStacksRequest, request.getAwsConfig());
        if (stacks.size() < 1) {
          String message = String.format(
              "# Did not get any stacks with id: %s while querying stacks list. Deletion may have completed",
              stackName);
          executionLogCallback.saveExecutionLog(message);
          builder.commandExecutionStatus(CommandExecutionStatus.SUCCESS);
          done = true;
          break;
        }
        stack = stacks.get(0);
        stackEventsTs =
            printStackEvents(request.getRegion(), stackId, request.getAwsConfig(), executionLogCallback, stackEventsTs);

        switch (stack.getStackStatus()) {
          case "DELETE_COMPLETE": {
            executionLogCallback.saveExecutionLog("# Completed deletion of stack");
            builder.commandExecutionStatus(CommandExecutionStatus.SUCCESS);
            done = true;
            break;
          }
          case "DELETE_FAILED": {
            String errorMessage = String.format("# Error: %s when deleting stack", stack.getStackStatusReason());
            executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
            builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
            done = true;
            break;
          }
          case "DELETE_IN_PROGRESS": {
            break;
          }
          default: {
            String errorMessage = String.format(
                "# Unexpected status: %s while deleting stack: %s ", stack.getStackStatus(), stack.getStackName());
            executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
            builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
            done = true;
            break;
          }
        }
        sleep(ofSeconds(10));
      }
      if (!done) {
        String errorMessage = String.format("# Timing out while deleting stack: %s", stackName);
        executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
        builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
      } else {
        executionLogCallback.saveExecutionLog(
            "Completed deletion of stack", LogLevel.INFO, CommandExecutionStatus.SUCCESS);
      }
    } catch (Exception ex) {
      String errorMessage =
          String.format("# Exception: %s while deleting stack: %s", ExceptionUtils.getMessage(ex), stackName);
      executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
    }

    printStackResources(request.getRegion(), stackId, request.getAwsConfig(), executionLogCallback);
    return builder.build();
  }

  private void createStackAndWaitWithEvents(CloudformationTaskInternalRequest createRequest,
      CreateStackRequest createStackRequest, CloudformationTaskInternalResponseBuilder builder,
      ExecutionLogCallback executionLogCallback) {
    int remainingTimeoutMs = createRequest.getTimeoutInMs() > 0 ? createRequest.getTimeoutInMs() : DEFAULT_TIMEOUT_MS;
    executionLogCallback.saveExecutionLog(
        format("# Calling Aws API to Create stack: %s", createStackRequest.getStackName()));
    long stackEventsTs = System.currentTimeMillis();
    CreateStackResult result =
        awsHelperService.createStack(createRequest.getRegion(), createStackRequest, createRequest.getAwsConfig());
    executionLogCallback.saveExecutionLog(format(
        "# Create Stack request submitted for stack: %s. Now polling for status.", createStackRequest.getStackName()));
    int timeOutMs = remainingTimeoutMs;
    long endTime = System.currentTimeMillis() + timeOutMs;
    String errorMsg;
    Stack stack = null;
    while (System.currentTimeMillis() < endTime) {
      DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(result.getStackId());
      List<Stack> stacks =
          awsHelperService.getAllStacks(createRequest.getRegion(), describeStacksRequest, createRequest.getAwsConfig());
      if (stacks.size() < 1) {
        String errorMessage = "# Error: received empty stack list from AWS";
        executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
        builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
        return;
      }

      stack = stacks.get(0);
      stackEventsTs = printStackEvents(createRequest, stackEventsTs, stack, executionLogCallback);

      switch (stack.getStackStatus()) {
        case "CREATE_COMPLETE": {
          executionLogCallback.saveExecutionLog("# Stack creation Successful");
          populateInfraMappingPropertiesFromStack(
              builder, stack, ExistingStackInfo.builder().stackExisted(false).build());
          if (!createRequest.isSkipWaitForResources()) {
            executionLogCallback.saveExecutionLog("# Waiting 30 seconds for resources to come up");
            sleep(ofSeconds(30));
          }
          printStackResources(createRequest, stack, executionLogCallback);
          return;
        }
        case "CREATE_FAILED": {
          errorMsg = format("# Error: %s while creating stack: %s", stack.getStackStatusReason(), stack.getStackName());
          executionLogCallback.saveExecutionLog(errorMsg, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
          builder.errorMessage(errorMsg).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          builder.cloudFormationCreateStackInternalResponse(
              CloudFormationCreateStackInternalResponse.builder().stackStatus(stack.getStackStatus()).build());
          printStackResources(createRequest, stack, executionLogCallback);
          return;
        }
        case "CREATE_IN_PROGRESS": {
          break;
        }
        case "ROLLBACK_IN_PROGRESS": {
          errorMsg = format("Creation of stack failed, Rollback in progress. Stack Name: %s : Reason: %s",
              stack.getStackName(), stack.getStackStatusReason());
          executionLogCallback.saveExecutionLog(errorMsg, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
          break;
        }
        case "ROLLBACK_FAILED": {
          errorMsg = format("# Creation of stack: %s failed, Rollback failed as well. Reason: %s", stack.getStackName(),
              stack.getStackStatusReason());
          executionLogCallback.saveExecutionLog(errorMsg, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
          builder.errorMessage(errorMsg).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          builder.cloudFormationCreateStackInternalResponse(
              CloudFormationCreateStackInternalResponse.builder().stackStatus(stack.getStackStatus()).build());
          printStackResources(createRequest, stack, executionLogCallback);
          return;
        }
        case "ROLLBACK_COMPLETE": {
          errorMsg = format("# Creation of stack: %s failed, Rollback complete", stack.getStackName());
          executionLogCallback.saveExecutionLog(errorMsg);
          builder.errorMessage(errorMsg).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          builder.cloudFormationCreateStackInternalResponse(
              CloudFormationCreateStackInternalResponse.builder().stackStatus(stack.getStackStatus()).build());
          printStackResources(createRequest, stack, executionLogCallback);
          return;
        }
        default: {
          String errorMessage = format("# Unexpected status: %s while Creating stack, Status reason: %s",
              stack.getStackStatus(), stack.getStackStatusReason());
          executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
          builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          builder.cloudFormationCreateStackInternalResponse(
              CloudFormationCreateStackInternalResponse.builder().stackStatus(stack.getStackStatus()).build());
          printStackResources(createRequest, stack, executionLogCallback);
          return;
        }
      }
      sleep(ofSeconds(10));
    }
    String errorMessage = format("# Timing out while Creating stack: %s", createStackRequest.getStackName());
    executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
    builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
    printStackResources(createRequest, stack, executionLogCallback);
  }

  private void updateStackAndWaitWithEvents(CloudformationTaskInternalRequest request,
      UpdateStackRequest updateStackRequest, CloudformationTaskInternalResponseBuilder builder, Stack originalStack,
      ExecutionLogCallback executionLogCallback) {
    ExistingStackInfo existingStackInfo =
        getExistingStackInfo(request.getAwsConfig(), request.getRegion(), originalStack);
    executionLogCallback.saveExecutionLog(
        format("# Calling Aws API to Update stack: %s", originalStack.getStackName()));
    long stackEventsTs = System.currentTimeMillis();

    AwsInternalConfig awsInternalConfig = request.getAwsConfig();
    UpdateStackResult updateStackResult =
        awsHelperService.updateStack(request.getRegion(), updateStackRequest, awsInternalConfig);
    executionLogCallback.saveExecutionLog(
        format("# Update Stack Request submitted for stack: %s. Now polling for status", originalStack.getStackName()));

    boolean noStackUpdated = false;
    if (updateStackResult == null || updateStackResult.getStackId() == null) {
      noStackUpdated = true;
      executionLogCallback.saveExecutionLog(
          format("# Update Stack Request Failed. There is nothing to be updated in the stack with name: %s",
              originalStack.getStackName()));
    }

    int timeOutMs = request.getTimeoutInMs() > 0 ? request.getTimeoutInMs() : DEFAULT_TIMEOUT_MS;
    long endTime = System.currentTimeMillis() + timeOutMs;
    Stack stack = null;
    while (System.currentTimeMillis() < endTime) {
      DescribeStacksRequest describeStacksRequest =
          new DescribeStacksRequest().withStackName(originalStack.getStackId());
      List<Stack> stacks = awsHelperService.getAllStacks(request.getRegion(), describeStacksRequest, awsInternalConfig);
      if (stacks.size() < 1) {
        String errorMessage = "# Error: received empty stack list from AWS";
        executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
        builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
        return;
      }
      stack = stacks.get(0);

      if (noStackUpdated) {
        switch (stack.getStackStatus()) {
          case "CREATE_COMPLETE":
          case "UPDATE_COMPLETE": {
            executionLogCallback.saveExecutionLog(format("# Stack is already in %s state.", stack.getStackStatus()));
            populateInfraMappingPropertiesFromStack(builder, stack, existingStackInfo);
            CloudFormationCreateStackInternalResponse cloudFormationCreateStackResponse =
                getCloudFormationCreateStackResponse(builder, stack, existingStackInfo);
            builder.cloudFormationCreateStackInternalResponse(cloudFormationCreateStackResponse);
            printStackResources(request, stack, executionLogCallback);
            return;
          }
          case "UPDATE_ROLLBACK_COMPLETE": {
            executionLogCallback.saveExecutionLog(format("# Stack is already in %s state.", stack.getStackStatus()));
            CloudFormationCreateStackInternalResponse cloudFormationCreateStackResponse =
                getCloudFormationCreateStackResponse(builder, stack, existingStackInfo);
            builder.cloudFormationCreateStackInternalResponse(cloudFormationCreateStackResponse);
            builder.commandExecutionStatus(SUCCESS);
            printStackResources(request, stack, executionLogCallback);
            return;
          }
          default: {
            String errorMessage =
                format("# Existing stack with name %s is already in status: %s, therefore exiting with failure",
                    stack.getStackName(), stack.getStackStatus());
            executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
            builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
            CloudFormationCreateStackInternalResponse cloudFormationCreateStackResponse =
                getCloudFormationCreateStackResponse(builder, stack, existingStackInfo);
            builder.cloudFormationCreateStackInternalResponse(cloudFormationCreateStackResponse);
            printStackResources(request, stack, executionLogCallback);
            return;
          }
        }
      }

      stackEventsTs = printStackEvents(request, stackEventsTs, stack, executionLogCallback);

      switch (stack.getStackStatus()) {
        case "CREATE_COMPLETE":
        case "UPDATE_COMPLETE": {
          executionLogCallback.saveExecutionLog("# Update Successful for stack");
          populateInfraMappingPropertiesFromStack(builder, stack, existingStackInfo);
          if (!request.isSkipWaitForResources()) {
            executionLogCallback.saveExecutionLog("# Waiting 30 seconds for resources to come up");
            sleep(ofSeconds(30));
          }
          CloudFormationCreateStackInternalResponse cloudFormationCreateStackResponse =
              getCloudFormationCreateStackResponse(builder, stack, existingStackInfo);
          builder.cloudFormationCreateStackInternalResponse(cloudFormationCreateStackResponse);
          printStackResources(request, stack, executionLogCallback);
          return;
        }
        case "UPDATE_COMPLETE_CLEANUP_IN_PROGRESS": {
          executionLogCallback.saveExecutionLog("Update completed, cleanup in progress");
          break;
        }
        case "UPDATE_ROLLBACK_FAILED": {
          String errorMessage = format("# Error: %s when updating stack: %s, Rolling back stack update failed",
              stack.getStackStatusReason(), stack.getStackName());
          executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
          builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          CloudFormationCreateStackInternalResponse cloudFormationCreateStackResponse =
              getCloudFormationCreateStackResponse(builder, stack, existingStackInfo);
          builder.cloudFormationCreateStackInternalResponse(cloudFormationCreateStackResponse);
          printStackResources(request, stack, executionLogCallback);
          return;
        }
        case "UPDATE_IN_PROGRESS": {
          break;
        }
        case "UPDATE_ROLLBACK_IN_PROGRESS": {
          executionLogCallback.saveExecutionLog("Update of stack failed, , Rollback in progress");
          builder.commandExecutionStatus(CommandExecutionStatus.FAILURE);
          break;
        }
        case "UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS": {
          executionLogCallback.saveExecutionLog(
              format("Rollback of stack update: %s completed, cleanup in progress", stack.getStackName()));
          break;
        }
        case "UPDATE_ROLLBACK_COMPLETE": {
          String errorMsg = format("# Rollback of stack update: %s completed", stack.getStackName());
          executionLogCallback.saveExecutionLog(errorMsg);
          builder.errorMessage(errorMsg).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          CloudFormationCreateStackInternalResponse cloudFormationCreateStackResponse =
              getCloudFormationCreateStackResponse(builder, stack, existingStackInfo);
          builder.cloudFormationCreateStackInternalResponse(cloudFormationCreateStackResponse);
          printStackResources(request, stack, executionLogCallback);
          return;
        }
        default: {
          String errorMessage =
              format("# Unexpected status: %s while creating stack: %s ", stack.getStackStatus(), stack.getStackName());
          executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
          builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
          CloudFormationCreateStackInternalResponse cloudFormationCreateStackResponse =
              getCloudFormationCreateStackResponse(builder, stack, existingStackInfo);
          builder.cloudFormationCreateStackInternalResponse(cloudFormationCreateStackResponse);
          printStackResources(request, stack, executionLogCallback);
          return;
        }
      }
      sleep(ofSeconds(10));
    }
    String errorMessage = format("# Timing out while Updating stack: %s", originalStack.getStackName());
    executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, CommandExecutionStatus.FAILURE);
    CloudFormationCreateStackInternalResponse cloudFormationCreateStackResponse =
        getCloudFormationCreateStackResponse(builder, stack, existingStackInfo);
    builder.cloudFormationCreateStackInternalResponse(cloudFormationCreateStackResponse);
    builder.errorMessage(errorMessage).commandExecutionStatus(CommandExecutionStatus.FAILURE);
    printStackResources(request, stack, executionLogCallback);
  }

  private ExistingStackInfo getExistingStackInfo(AwsInternalConfig awsConfig, String region, Stack originalStack) {
    ExistingStackInfoBuilder builder = ExistingStackInfo.builder();
    builder.stackExisted(true);
    builder.oldStackParameters(originalStack.getParameters().stream().collect(
        toMap(Parameter::getParameterKey, Parameter::getParameterValue)));
    builder.oldStackBody(awsCFHelperServiceDelegate.getStackBody(awsConfig, region, originalStack.getStackId()));
    return builder.build();
  }

  private void populateInfraMappingPropertiesFromStack(
      CloudformationTaskInternalResponseBuilder builder, Stack stack, ExistingStackInfo existingStackInfo) {
    CloudFormationCreateStackInternalResponse cloudFormationCreateStackResponse =
        createCloudFormationCreateStackResponse(stack, existingStackInfo);
    builder.commandExecutionStatus(SUCCESS).cloudFormationCreateStackInternalResponse(
        cloudFormationCreateStackResponse);
  }

  private CloudFormationCreateStackInternalResponse getCloudFormationCreateStackResponse(
      CloudformationTaskInternalResponseBuilder builder, Stack stack, ExistingStackInfo existingStackInfo) {
    CloudFormationCreateStackInternalResponse cloudFormationCreateStackResponse =
        builder.build().getCloudFormationCreateStackInternalResponse();
    if (cloudFormationCreateStackResponse == null) {
      cloudFormationCreateStackResponse = createCloudFormationCreateStackResponse(stack, existingStackInfo);
    } else {
      cloudFormationCreateStackResponse.setStackStatus(stack.getStackStatus());
    }
    return cloudFormationCreateStackResponse;
  }

  private CloudFormationCreateStackInternalResponse createCloudFormationCreateStackResponse(
      Stack stack, ExistingStackInfo existingStackInfo) {
    CloudFormationCreateStackInternalResponseBuilder createBuilder =
        CloudFormationCreateStackInternalResponse.builder();
    createBuilder.existingStackInfo(existingStackInfo);
    createBuilder.stackId(stack.getStackId());
    createBuilder.stackStatus(stack.getStackStatus());
    List<Output> outputs = stack.getOutputs();
    if (isNotEmpty(outputs)) {
      createBuilder.cloudFormationOutputMap(
          outputs.stream().collect(toMap(Output::getOutputKey, Output::getOutputValue)));
    }
    createBuilder.commandExecutionStatus(SUCCESS);
    return createBuilder.build();
  }

  @VisibleForTesting
  Set<String> getCapabilities(
      AwsInternalConfig awsConfig, String region, String data, List<String> userDefinedCapabilities, String type) {
    List<String> capabilities = awsCFHelperServiceDelegate.getCapabilities(awsConfig, region, data, type);
    Set<String> allCapabilities = new HashSet<>();

    if (isNotEmpty(userDefinedCapabilities)) {
      allCapabilities.addAll(userDefinedCapabilities);
    }

    allCapabilities.addAll(capabilities);
    return allCapabilities;
  }

  private List<Parameter> getCfParams(CloudformationTaskInternalRequest cloudFormationCreateStackRequest) {
    List<Parameter> allParams = newArrayList();
    if (isNotEmpty(cloudFormationCreateStackRequest.getVariables())) {
      cloudFormationCreateStackRequest.getVariables().forEach(
          (key, value) -> allParams.add(new Parameter().withParameterKey(key).withParameterValue(value)));
    }
    if (isNotEmpty(cloudFormationCreateStackRequest.getEncryptedVariables())) {
      for (Map.Entry<String, EncryptedDataDetail> entry :
          cloudFormationCreateStackRequest.getEncryptedVariables().entrySet()) {
        allParams.add(
            new Parameter()
                .withParameterKey(entry.getKey())
                .withParameterValue(String.valueOf(encryptionService.getDecryptedValue(entry.getValue(), false))));
      }
    }
    return allParams;
  }

  @VisibleForTesting
  List<Tag> getCloudformationTags(CloudformationTaskInternalRequest cloudFormationCreateStackRequest)
      throws IOException {
    List<Tag> tags = null;
    if (isNotEmpty(cloudFormationCreateStackRequest.getTags())) {
      ObjectMapper mapper = new ObjectMapper();
      tags = Arrays.asList(mapper.readValue(cloudFormationCreateStackRequest.getTags(), Tag[].class));
    }
    return tags;
  }

  @VisibleForTesting
  protected long printStackEvents(CloudformationTaskInternalRequest request, long stackEventsTs, Stack stack,
      ExecutionLogCallback executionLogCallback) {
    List<StackEvent> stackEvents = getStackEvents(request, stack);
    boolean printed = false;
    long currentLatestTs = -1;
    for (StackEvent event : stackEvents) {
      long tsForEvent = event.getTimestamp().getTime();
      if (tsForEvent > stackEventsTs) {
        if (!printed) {
          executionLogCallback.saveExecutionLog("******************** Cloud Formation Events ********************");
          executionLogCallback.saveExecutionLog("********[Status] [Type] [Logical Id] [Status Reason] ***********");
          printed = true;
        }
        executionLogCallback.saveExecutionLog(format("[%s] [%s] [%s] [%s] [%s]", event.getResourceStatus(),
            event.getResourceType(), event.getLogicalResourceId(), getStatusReason(event.getResourceStatusReason()),
            event.getPhysicalResourceId()));
        if (currentLatestTs == -1) {
          currentLatestTs = tsForEvent;
        }
      }
    }
    if (currentLatestTs != -1) {
      stackEventsTs = currentLatestTs;
    }
    return stackEventsTs;
  }

  @VisibleForTesting
  protected long printStackEvents(String region, String stackId, AwsInternalConfig awsConfig,
      ExecutionLogCallback executionLogCallback, long stackEventsTs) {
    List<StackEvent> stackEvents = getStackEvents(region, stackId, awsConfig);
    boolean printed = false;
    long currentLatestTs = -1;
    for (StackEvent event : stackEvents) {
      long tsForEvent = event.getTimestamp().getTime();
      if (tsForEvent > stackEventsTs) {
        if (!printed) {
          executionLogCallback.saveExecutionLog("******************** Cloud Formation Events ********************");
          executionLogCallback.saveExecutionLog("********[Status] [Type] [Logical Id] [Status Reason] ***********");
          printed = true;
        }
        executionLogCallback.saveExecutionLog(format("[%s] [%s] [%s] [%s] [%s]", event.getResourceStatus(),
            event.getResourceType(), event.getLogicalResourceId(), getStatusReason(event.getResourceStatusReason()),
            event.getPhysicalResourceId()));
        if (currentLatestTs == -1) {
          currentLatestTs = tsForEvent;
        }
      }
    }
    if (currentLatestTs != -1) {
      stackEventsTs = currentLatestTs;
    }
    return stackEventsTs;
  }

  @VisibleForTesting
  protected void printStackResources(
      CloudformationTaskInternalRequest request, Stack stack, ExecutionLogCallback executionLogCallback) {
    if (stack == null) {
      return;
    }
    List<StackResource> stackResources = getStackResources(request, stack);
    executionLogCallback.saveExecutionLog("******************** Cloud Formation Resources ********************");
    executionLogCallback.saveExecutionLog("********[Status] [Type] [Logical Id] [Status Reason] ***********");
    stackResources.forEach(resource
        -> executionLogCallback.saveExecutionLog(format("[%s] [%s] [%s] [%s] [%s]", resource.getResourceStatus(),
            resource.getResourceType(), resource.getLogicalResourceId(),
            getStatusReason(resource.getResourceStatusReason()), resource.getPhysicalResourceId())));
  }

  @VisibleForTesting
  protected void printStackResources(
      String region, String stackId, AwsInternalConfig awsConfig, ExecutionLogCallback executionLogCallback) {
    if (isEmpty(stackId)) {
      return;
    }
    List<StackResource> stackResources = getStackResources(region, stackId, awsConfig);
    executionLogCallback.saveExecutionLog("******************** Cloud Formation Resources ********************");
    executionLogCallback.saveExecutionLog("********[Status] [Type] [Logical Id] [Status Reason] ***********");
    stackResources.forEach(resource
        -> executionLogCallback.saveExecutionLog(format("[%s] [%s] [%s] [%s] [%s]", resource.getResourceStatus(),
            resource.getResourceType(), resource.getLogicalResourceId(),
            getStatusReason(resource.getResourceStatusReason()), resource.getPhysicalResourceId())));
  }

  private List<StackResource> getStackResources(CloudformationTaskInternalRequest request, Stack stack) {
    return awsHelperService.getAllStackResources(request.getRegion(),
        new DescribeStackResourcesRequest().withStackName(stack.getStackName()), request.getAwsConfig());
  }

  private List<StackResource> getStackResources(String region, String stackId, AwsInternalConfig awsConfig) {
    return awsHelperService.getAllStackResources(
        region, new DescribeStackResourcesRequest().withStackName(stackId), awsConfig);
  }

  private List<StackEvent> getStackEvents(CloudformationTaskInternalRequest request, Stack stack) {
    return awsHelperService.getAllStackEvents(request.getRegion(),
        new DescribeStackEventsRequest().withStackName(stack.getStackName()), request.getAwsConfig());
  }

  private List<StackEvent> getStackEvents(String region, String stackName, AwsInternalConfig awsConfig) {
    return awsHelperService.getAllStackEvents(
        region, new DescribeStackEventsRequest().withStackName(stackName), awsConfig);
  }

  private String getStatusReason(String reason) {
    return isNotEmpty(reason) ? reason : StringUtils.EMPTY;
  }
}
