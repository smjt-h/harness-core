/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.cloudformation.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Stack;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.AWSCloudformationClient;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.delegate.task.aws.AwsNgConfigMapper;
import io.harness.delegate.task.aws.AwsS3DelegateTaskHelper;
import io.harness.delegate.task.cloudformation.CloudformationBaseHelper;
import io.harness.delegate.task.cloudformation.CloudformationTaskNGParameters;
import io.harness.delegate.task.cloudformation.CloudformationTaskNGResponse;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import software.wings.service.intfc.aws.delegate.AwsCFHelperServiceDelegate;

import com.amazonaws.services.cloudformation.model.Parameter;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
public abstract class CloudformationAbstractTaskHandler {
  @Inject CloudformationBaseHelper cloudformationBaseHelper;
  @Inject AwsS3DelegateTaskHelper awsS3DelegateTaskHelper;
  @Inject AwsNgConfigMapper awsNgConfigMapper;
  @Inject protected AWSCloudformationClient awsCloudformationClient;
  @Inject protected AwsCFHelperServiceDelegate awsCFHelperServiceDelegate;

  public abstract CloudformationTaskNGResponse executeTaskInternal(
      CloudformationTaskNGParameters taskNGParameters, String delegateId, String taskId, LogCallback logCallback)
      throws IOException, TimeoutException, InterruptedException;

  public CloudformationTaskNGResponse executeTask(CloudformationTaskNGParameters taskNGParameters, String delegateId,
      String taskId, LogCallback logCallback) throws Exception {
    try {
      return executeTaskInternal(taskNGParameters, delegateId, taskId, logCallback);
    } catch (Exception e) {
      log.error(e.getMessage());
      return CloudformationTaskNGResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(e.getMessage())
          .build();
    }
  }

  protected CloudformationTaskNGResponse deleteStack(
      String stackId, String stackName, CloudformationTaskNGParameters taskNGParameters, LogCallback logCallback) {
    return null;
  }

  protected List<Parameter> getParameters(Map<String, String> parameters) {
    return parameters.entrySet()
        .stream()
        .map(stringStringEntry
            -> new Parameter()
                   .withParameterKey(stringStringEntry.getKey())
                   .withParameterValue(stringStringEntry.getValue()))
        .collect(Collectors.toList());
  }

  protected Optional<Stack> getIfStackExists(String stackName, AwsInternalConfig awsConfig, String region) {
    List<Stack> stacks = awsCloudformationClient.getAllStacks(region, new DescribeStacksRequest(), awsConfig);
    if (isEmpty(stacks)) {
      return Optional.empty();
    }
    return stacks.stream().filter(stack -> stack.getStackName().equals(stackName)).findFirst();
  }

}
