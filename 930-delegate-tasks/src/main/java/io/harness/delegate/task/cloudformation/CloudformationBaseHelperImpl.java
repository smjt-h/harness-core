/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.cloudformation;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;

import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.AWSCloudformationClient;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.logging.LogCallback;

import software.wings.helpers.ext.cloudformation.response.ExistingStackInfo;
import software.wings.service.intfc.aws.delegate.AwsCFHelperServiceDelegate;

import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.amazonaws.services.cloudformation.model.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(CDP)
public class CloudformationBaseHelperImpl implements CloudformationBaseHelper {
  public static final String CLOUDFORMATION_STACK_CREATE_URL = "Create URL";
  public static final String CLOUDFORMATION_STACK_CREATE_BODY = "Create Body";
  public static final String CLOUDFORMATION_STACK_CREATE_GIT = "Create GIT";

  @Inject protected AWSCloudformationClient awsCloudformationClient;
  @Inject protected AwsCFHelperServiceDelegate awsCFHelperServiceDelegate;

  public List<Tag> getCloudformationTags(String tagsJson) throws IOException {
    List<Tag> tags = null;
    if (isNotEmpty(tagsJson)) {
      ObjectMapper mapper = new ObjectMapper();
      tags = Arrays.asList(mapper.readValue(tagsJson, Tag[].class));
    }
    return tags;
  }

  public Set<String> getCapabilities(AwsInternalConfig awsInternalConfig, String region, String data,
      List<String> userDefinedCapabilities, String templateType) {
    List<String> capabilities = awsCFHelperServiceDelegate.getCapabilities(awsInternalConfig, region, data, templateType);
    Set<String> allCapabilities = new HashSet<>();

    if (isNotEmpty(userDefinedCapabilities)) {
      allCapabilities.addAll(userDefinedCapabilities);
    }

    allCapabilities.addAll(capabilities);
    return allCapabilities;
  }

  public long printStackEvents(AwsInternalConfig awsInternalConfig, String region, long stackEventsTs, Stack stack,
      LogCallback executionLogCallback) {
    List<StackEvent> stackEvents = getStackEvents(awsInternalConfig, region, stack);
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

  public void printStackResources(
      AwsInternalConfig awsInternalConfig, String region, Stack stack, LogCallback executionLogCallback) {
    if (stack == null) {
      return;
    }
    List<StackResource> stackResources = getStackResources(awsInternalConfig, region, stack);
    executionLogCallback.saveExecutionLog("******************** Cloud Formation Resources ********************");
    executionLogCallback.saveExecutionLog("********[Status] [Type] [Logical Id] [Status Reason] ***********");
    stackResources.forEach(resource
        -> executionLogCallback.saveExecutionLog(format("[%s] [%s] [%s] [%s] [%s]", resource.getResourceStatus(),
            resource.getResourceType(), resource.getLogicalResourceId(),
            getStatusReason(resource.getResourceStatusReason()), resource.getPhysicalResourceId())));
  }

  public ExistingStackInfo getExistingStackInfo(
      AwsInternalConfig awsInternalConfig, String region, Stack originalStack) {
    ExistingStackInfo.ExistingStackInfoBuilder builder = ExistingStackInfo.builder();
    builder.stackExisted(true);
    builder.oldStackParameters(originalStack.getParameters().stream().collect(
        toMap(Parameter::getParameterKey, Parameter::getParameterValue)));
    builder.oldStackBody(
        awsCFHelperServiceDelegate.getStackBody(awsInternalConfig, region, originalStack.getStackId()));
    return builder.build();
  }

  private List<StackResource> getStackResources(AwsInternalConfig awsInternalConfig, String region, Stack stack) {
    return awsCloudformationClient.getAllStackResources(
        region, new DescribeStackResourcesRequest().withStackName(stack.getStackName()), awsInternalConfig);
  }

  private List<StackEvent> getStackEvents(AwsInternalConfig awsInternalConfig, String region, Stack stack) {
    return awsCloudformationClient.getAllStackEvents(
        region, new DescribeStackEventsRequest().withStackName(stack.getStackName()), awsInternalConfig);
  }

  private String getStatusReason(String reason) {
    return isNotEmpty(reason) ? reason : StringUtils.EMPTY;
  }
}
