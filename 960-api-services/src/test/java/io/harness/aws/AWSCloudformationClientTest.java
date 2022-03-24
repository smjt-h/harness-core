/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.aws;

import static io.harness.rule.OwnerRule.NGONZALEZ;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.category.element.UnitTests;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import software.wings.service.impl.AwsApiHelperService;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.services.cloudformation.waiters.AmazonCloudFormationWaiters;
import com.amazonaws.waiters.Waiter;
import java.util.List;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AWSCloudformationClientTest extends CategoryTest {
  @Mock private AwsApiHelperService awsApiHelperService;
  @InjectMocks private AWSCloudformationClientImpl mockCFAWSClient;
  @Mock private LogCallback logCallback;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void shouldUpdateStack() {
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackName = "Stack Name";
    String region = "us-west-1";
    UpdateStackRequest request = new UpdateStackRequest().withStackName(stackName);
    AmazonCloudFormationClient mockClient = mock(AmazonCloudFormationClient.class);
    AWSCloudformationClientImpl service = spy(new AWSCloudformationClientImpl());
    doReturn(mockClient).when(service).getAmazonCloudFormationClient(any(), any());
    AwsCallTracker mockTracker = mock(AwsCallTracker.class);
    doNothing().when(mockTracker).trackCFCall(anyString());
    on(service).set("tracker", mockTracker);
    service.updateStack(region, request, AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    verify(mockClient).updateStack(request);
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void shouldDeleteStack() {
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackName = "Stack Name";
    String region = "us-west-1";
    DeleteStackRequest request = new DeleteStackRequest().withStackName(stackName);
    AmazonCloudFormationClient mockClient = mock(AmazonCloudFormationClient.class);
    AWSCloudformationClientImpl service = spy(new AWSCloudformationClientImpl());
    doReturn(mockClient).when(service).getAmazonCloudFormationClient(any(), any());
    AwsCallTracker mockTracker = mock(AwsCallTracker.class);
    doNothing().when(mockTracker).trackCFCall(anyString());
    on(service).set("tracker", mockTracker);
    service.deleteStack(region, request, AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    verify(mockClient).deleteStack(request);
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void shouldDescribeStack() {
    char[] accessKey = "qwer".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackName = "Stack Name";
    String region = "us-west-1";
    DescribeStacksRequest request = new DescribeStacksRequest().withStackName(stackName);
    AmazonCloudFormationClient mockClient = mock(AmazonCloudFormationClient.class);
    AWSCloudformationClientImpl service = spy(new AWSCloudformationClientImpl());
    doReturn(mockClient).when(service).getAmazonCloudFormationClient(any(), any());
    DescribeStacksResult result = new DescribeStacksResult().withStacks(new Stack().withStackName(stackName));
    doReturn(result).when(mockClient).describeStacks(request);
    AwsCallTracker mockTracker = mock(AwsCallTracker.class);
    doNothing().when(mockTracker).trackCFCall(anyString());
    on(service).set("tracker", mockTracker);
    DescribeStacksResult actual = service.describeStacks(
        region, request, AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    assertThat(actual).isNotNull();
    assertThat(actual.getStacks().size()).isEqualTo(1);
    assertThat(actual.getStacks().get(0).getStackName()).isEqualTo(stackName);
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void shouldGetAllEvents() {
    char[] accessKey = "qwer".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackName = "Stack Name";
    String region = "us-west-1";
    DescribeStackEventsRequest request = new DescribeStackEventsRequest().withStackName(stackName);
    AmazonCloudFormationClient mockClient = mock(AmazonCloudFormationClient.class);
    AWSCloudformationClientImpl service = spy(new AWSCloudformationClientImpl());
    doReturn(mockClient).when(service).getAmazonCloudFormationClient(any(), any());
    DescribeStackEventsResult result =
        new DescribeStackEventsResult().withStackEvents(new StackEvent().withStackName(stackName).withEventId("id"));
    doReturn(result).when(mockClient).describeStackEvents(request);
    AwsCallTracker mockTracker = mock(AwsCallTracker.class);
    doNothing().when(mockTracker).trackCFCall(anyString());
    on(service).set("tracker", mockTracker);
    List<StackEvent> events = service.getAllStackEvents(
        region, request, AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    assertThat(events).isNotNull();
    assertThat(events.size()).isEqualTo(1);
    assertThat(events.get(0).getStackName()).isEqualTo(stackName);
    assertThat(events.get(0).getEventId()).isEqualTo("id");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void shouldCreateStack() {
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackName = "Stack Name";
    String region = "us-west-1";
    CreateStackRequest request = new CreateStackRequest().withStackName(stackName);
    AmazonCloudFormationClient mockClient = mock(AmazonCloudFormationClient.class);
    AWSCloudformationClientImpl service = spy(new AWSCloudformationClientImpl());
    doReturn(mockClient).when(service).getAmazonCloudFormationClient(any(), any());
    AwsCallTracker mockTracker = mock(AwsCallTracker.class);
    doNothing().when(mockTracker).trackCFCall(anyString());
    on(service).set("tracker", mockTracker);
    service.createStack(region, request, AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    verify(mockClient).createStack(request);
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void shouldListStacks() {
    char[] accessKey = "qwer".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackName = "Stack Name";
    String region = "us-west-1";
    DescribeStacksRequest request = new DescribeStacksRequest().withStackName(stackName);
    AmazonCloudFormationClient mockClient = mock(AmazonCloudFormationClient.class);
    AWSCloudformationClientImpl service = spy(new AWSCloudformationClientImpl());
    doReturn(mockClient).when(service).getAmazonCloudFormationClient(any(), any());
    DescribeStacksResult result = new DescribeStacksResult().withStacks(new Stack().withStackName(stackName));
    doReturn(result).when(mockClient).describeStacks(request);
    AwsCallTracker mockTracker = mock(AwsCallTracker.class);
    doNothing().when(mockTracker).trackCFCall(anyString());
    on(service).set("tracker", mockTracker);
    List<Stack> stacks = service.getAllStacks(
        region, request, AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    assertThat(stacks).isNotNull();
    assertThat(stacks.size()).isEqualTo(1);
    assertThat(stacks.get(0).getStackName()).isEqualTo(stackName);
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void getAllStackResources() {
    char[] accessKey = "qwer".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackName = "Stack Name";
    String region = "us-west-1";
    DescribeStackResourcesRequest request = new DescribeStackResourcesRequest().withStackName(stackName);
    AmazonCloudFormationClient mockClient = mock(AmazonCloudFormationClient.class);
    AWSCloudformationClientImpl service = spy(new AWSCloudformationClientImpl());
    doReturn(mockClient).when(service).getAmazonCloudFormationClient(any(), any());
    DescribeStackResourcesResult result =
        new DescribeStackResourcesResult().withStackResources(new StackResource().withStackName(stackName));
    doReturn(result).when(mockClient).describeStackResources(request);
    AwsCallTracker mockTracker = mock(AwsCallTracker.class);
    doNothing().when(mockTracker).trackCFCall(anyString());
    on(service).set("tracker", mockTracker);
    List<StackResource> stacks = service.getAllStackResources(
        region, request, AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    assertThat(stacks).isNotNull();
    assertThat(stacks.size()).isEqualTo(1);
    assertThat(stacks.get(0).getStackName()).isEqualTo(stackName);
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void shouldWaitForDeleteStackCompletion() {
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackName = "Stack Name";
    String region = "us-west-1";
    DescribeStacksRequest request = new DescribeStacksRequest().withStackName(stackName);
    AmazonCloudFormationClient mockClient = mock(AmazonCloudFormationClient.class);
    AmazonCloudFormationWaiters mockWaiter = mock(AmazonCloudFormationWaiters.class);
    Future future = mock(Future.class);

    Waiter<DescribeStacksRequest> mockWaiterStack = mock(Waiter.class);

    AWSCloudformationClientImpl service = spy(new AWSCloudformationClientImpl());
    doReturn(mockClient).when(service).getAmazonCloudFormationClient(any(), any());
    doReturn(mockWaiter).when(service).getAmazonCloudFormationWaiter(any());
    doReturn(mockWaiterStack).when(mockWaiter).stackDeleteComplete();
    doReturn(true).when(future).isDone();
    doReturn(future).when(mockWaiterStack).runAsync(any(), any());
    service.stackDeletionCompleted(
        request, AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build(), region, logCallback);
    verify(mockWaiterStack).runAsync(any(), any());
    verify(mockWaiter).stackDeleteComplete();
  }
  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void checkServiceException() {
    char[] accessKey = "qwer".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackName = "Stack Name";
    String region = "us-west-1";
    DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(stackName);
    DeleteStackRequest deleteStackRequest = new DeleteStackRequest().withStackName(stackName);
    DescribeStackEventsRequest describeStackEventsRequest = new DescribeStackEventsRequest().withStackName(stackName);
    CreateStackRequest createStackRequest = new CreateStackRequest().withStackName(stackName);
    UpdateStackRequest updateStackRequest = new UpdateStackRequest().withStackName(stackName);
    AWSCloudformationClientImpl service = spy(mockCFAWSClient);
    doThrow(AmazonServiceException.class).when(service).getAmazonCloudFormationClient(any(), any());
    service.getAllStacks(
        region, describeStacksRequest, AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    service.deleteStack(
        region, deleteStackRequest, AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    service.getAllStackEvents(region, describeStackEventsRequest,
        AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    service.createStack(
        region, createStackRequest, AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    service.updateStack(
        region, updateStackRequest, AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    service.describeStacks(
        region, describeStacksRequest, AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    verify(awsApiHelperService, times(6)).handleAmazonServiceException(any());
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void checkClientException() {
    char[] accessKey = "qwer".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackName = "Stack Name";
    String region = "us-west-1";
    DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(stackName);
    DeleteStackRequest deleteStackRequest = new DeleteStackRequest().withStackName(stackName);
    DescribeStackEventsRequest describeStackEventsRequest = new DescribeStackEventsRequest().withStackName(stackName);
    CreateStackRequest createStackRequest = new CreateStackRequest().withStackName(stackName);
    UpdateStackRequest updateStackRequest = new UpdateStackRequest().withStackName(stackName);

    AWSCloudformationClientImpl service = spy(mockCFAWSClient);
    doThrow(AmazonClientException.class).when(service).getAmazonCloudFormationClient(any(), any());
    service.getAllStacks(
        region, describeStacksRequest, AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    service.deleteStack(
        region, deleteStackRequest, AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    service.getAllStackEvents(region, describeStackEventsRequest,
        AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    service.createStack(
        region, createStackRequest, AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    service.updateStack(
        region, updateStackRequest, AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    service.describeStacks(
        region, describeStacksRequest, AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
    verify(awsApiHelperService, times(6)).handleAmazonClientException(any());
  }
  @Test(expected = Exception.class)
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void checkGeneralExceptionGetAllStacks() {
    char[] accessKey = "qwer".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackName = "Stack Name";
    String region = "us-west-1";
    DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(stackName);
    AWSCloudformationClientImpl service = spy(mockCFAWSClient);
    doThrow(Exception.class).when(service).getAmazonCloudFormationClient(any(), any());
    service.getAllStacks(
        region, describeStacksRequest, AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
  }
  @Test(expected = Exception.class)
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void checkGeneralExceptionDeleteStacks() {
    char[] accessKey = "qwer".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackName = "Stack Name";
    String region = "us-west-1";
    DeleteStackRequest deleteStackRequest = new DeleteStackRequest().withStackName(stackName);
    AWSCloudformationClientImpl service = spy(mockCFAWSClient);
    doThrow(Exception.class).when(service).getAmazonCloudFormationClient(any(), any());
    service.deleteStack(
        region, deleteStackRequest, AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build());
  }
}
