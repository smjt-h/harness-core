/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.cloudformation.cloudformationtaskhandler;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.aws.AWSCloudformationClient;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.task.cloudformation.CloudformationBaseHelper;
import io.harness.exception.ExceptionUtils;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest;
import software.wings.helpers.ext.cloudformation.request.CloudformationTaskInternalRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse;
import software.wings.helpers.ext.cloudformation.response.CloudformationTaskInternalResponse;
import software.wings.service.intfc.aws.delegate.AwsCFHelperServiceDelegate;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.mappers.artifact.AwsConfigToInternalMapper;

import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Stack;
import com.google.inject.Inject;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public abstract class CloudFormationCommandTaskHandler {
  @Inject protected EncryptionService encryptionService;
  @Inject protected AWSCloudformationClient awsHelperService;
  @Inject protected AwsCFHelperServiceDelegate awsCFHelperServiceDelegate;
  @Inject private DelegateLogService delegateLogService;
  @Inject protected CloudformationBaseHelper cloudformationBaseHelper;

  protected static final String stackNamePrefix = "HarnessStack-";

  // ten minutes default timeout for polling stack operations
  static final int DEFAULT_TIMEOUT_MS = 10 * 60 * 1000;

  public CloudFormationCommandExecutionResponse execute(
      CloudFormationCommandRequest request, List<EncryptedDataDetail> details) {
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(delegateLogService, request.getAccountId(),
        request.getAppId(), request.getActivityId(), request.getCommandName());
    try {
      CloudFormationCommandExecutionResponse result;
      result = executeInternal(request, details, executionLogCallback);
      logStatusMessage(executionLogCallback, result);
      return result;
    } catch (Exception ex) {
      String errorMessage = format("Exception: %s while executing CF task.", ExceptionUtils.getMessage(ex));
      executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR, FAILURE);
      return CloudFormationCommandExecutionResponse.builder()
          .errorMessage(errorMessage)
          .commandExecutionStatus(FAILURE)
          .build();
    }
  }

  private void logStatusMessage(
      ExecutionLogCallback executionLogCallback, CloudFormationCommandExecutionResponse result) {
    final CommandExecutionStatus status = result.getCommandExecutionStatus();
    if (status == SUCCESS) {
      executionLogCallback.saveExecutionLog("Execution finished successfully.", LogLevel.INFO, status);
    } else if (status == FAILURE) {
      executionLogCallback.saveExecutionLog("Execution has been failed.", LogLevel.ERROR, status);
    }
  }

  protected CloudFormationCommandExecutionResponse deleteStack(String stackId, String stackName,
      CloudFormationCommandRequest request, ExecutionLogCallback executionLogCallback) {
    CloudformationTaskInternalRequest cloudformationTaskInternalRequest =
        CloudformationTaskInternalRequest.builder()
            .accountId(request.getAccountId())
            .activityId(request.getActivityId())
            .appId(request.getAppId())
            .awsConfig(AwsConfigToInternalMapper.toAwsInternalConfig(request.getAwsConfig()))
            .timeoutInMs(request.getTimeoutInMs())
            .region(request.getRegion())
            .cloudFormationRoleArn(request.getCloudFormationRoleArn())
            .skipWaitForResources(request.isSkipWaitForResources())
            .commandName(request.getCommandName())
            .build();

    CloudformationTaskInternalResponse cloudformationTaskInternalResponse = cloudformationBaseHelper.deleteStack(
        stackId, stackName, cloudformationTaskInternalRequest, executionLogCallback);

    return CloudFormationCommandExecutionResponse.builder()
        .errorMessage(cloudformationTaskInternalResponse.getErrorMessage())
        .commandExecutionStatus(cloudformationTaskInternalResponse.getCommandExecutionStatus())
        .build();
  }

  protected abstract CloudFormationCommandExecutionResponse executeInternal(CloudFormationCommandRequest request,
      List<EncryptedDataDetail> details, ExecutionLogCallback executionLogCallback);
}
