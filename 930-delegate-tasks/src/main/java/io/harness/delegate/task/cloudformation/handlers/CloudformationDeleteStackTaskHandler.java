/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.cloudformation.handlers;

import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;

import io.harness.aws.beans.AwsInternalConfig;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.task.cloudformation.CloudformationBaseHelper;
import io.harness.delegate.task.cloudformation.CloudformationTaskNGParameters;
import io.harness.delegate.task.cloudformation.CloudformationTaskNGResponse;
import io.harness.logging.LogCallback;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

public class CloudformationDeleteStackTaskHandler extends CloudformationAbstractTaskHandler {
  @Inject CloudformationBaseHelper cloudformationBaseHelper;
  @Override
  public CloudformationTaskNGResponse executeTaskInternal(
      CloudformationTaskNGParameters taskNGParameters, String delegateId, String taskId, LogCallback logCallback)
      throws IOException, TimeoutException, InterruptedException {
    AwsConnectorDTO awsConnectorDTO = taskNGParameters.getAwsConnector();
    AwsInternalConfig awsInternalConfig = cloudformationBaseHelper.getAwsInternalConfig(
        awsConnectorDTO, taskNGParameters.getRegion(), taskNGParameters.getEncryptedDataDetails());
    Optional<Stack> existingStack = cloudformationBaseHelper.getIfStackExists(taskNGParameters.getCustomStackName(),
        taskNGParameters.getStackNameSuffix(), awsInternalConfig, taskNGParameters.getRegion());
    String stackId;
    if (existingStack.isPresent()) {
      stackId = existingStack.get().getStackId();
    } else {
      logCallback.saveExecutionLog("Stack does not exist", ERROR, CommandExecutionStatus.FAILURE);
      return CloudformationTaskNGResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage("Stack does not exist")
          .build();
    }
    try {
      cloudformationBaseHelper.deleteStack(taskNGParameters.getRegion(), awsInternalConfig, stackId,
          taskNGParameters.getCloudFormationRoleArn(), (int) taskNGParameters.getTimeoutInMs());
      cloudformationBaseHelper.waitForStackToBeDeleted(
          taskNGParameters.getRegion(), awsInternalConfig, stackId, logCallback);
    } catch (Exception e) {
      logCallback.saveExecutionLog("Stack deletion failed", ERROR, CommandExecutionStatus.FAILURE);
      return CloudformationTaskNGResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage("Stack deletion failed")
          .build();
    }
    logCallback.saveExecutionLog("Stack deleted", INFO, CommandExecutionStatus.SUCCESS);
    return CloudformationTaskNGResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
  }
}
