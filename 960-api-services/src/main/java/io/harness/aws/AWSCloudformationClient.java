/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.aws;

import io.harness.aws.beans.AwsInternalConfig;
import io.harness.logging.LogCallback;

import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.ParameterDeclaration;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.services.cloudformation.model.UpdateStackResult;
import java.util.List;

public interface AWSCloudformationClient {
  List<Stack> getAllStacks(String region, DescribeStacksRequest describeStacksRequest, AwsInternalConfig awsConfig);

  void deleteStack(String region, DeleteStackRequest deleteStackRequest, AwsInternalConfig awsConfig);

  List<StackResource> getAllStackResources(
      String region, DescribeStackResourcesRequest describeStackResourcesRequest, AwsInternalConfig awsConfig);

  List<StackEvent> getAllStackEvents(String region, DescribeStackEventsRequest describeStackEventsRequest,
      AwsInternalConfig awsConfig, long lastStackEventsTs);

  CreateStackResult createStack(String region, CreateStackRequest createStackRequest, AwsInternalConfig awsConfig);

  UpdateStackResult updateStack(String region, UpdateStackRequest updateStackRequest, AwsInternalConfig awsConfig);

  DescribeStacksResult describeStacks(
      String region, DescribeStacksRequest describeStacksRequest, AwsInternalConfig awsConfig);

  void waitForStackDeletionCompleted(DescribeStacksRequest describeStacksRequest, AwsInternalConfig awsConfig,
      String region, LogCallback logCallback, long stackEventsTs);

  List<ParameterDeclaration> getParamsData(
      AwsInternalConfig awsConfig, String region, String data, AwsCFTemplatesType awsCFTemplatesType);
}
