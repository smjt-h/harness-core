/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.cloudformation;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.TMACARI;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.AWSCloudformationClient;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.category.element.UnitTests;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.cloudformation.response.ExistingStackInfo;
import software.wings.service.intfc.aws.delegate.AwsCFHelperServiceDelegate;

import com.amazonaws.services.cloudformation.model.Parameter;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.amazonaws.services.cloudformation.model.Tag;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class CloudFormationBaseHelperImplTest extends CategoryTest {
  @Mock private AWSCloudformationClient awsCloudformationClient;
  @Mock protected AwsCFHelperServiceDelegate awsCFHelperServiceDelegate;
  @InjectMocks private CloudformationBaseHelperImpl cloudFormationBaseHelper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testPrintStackEvents() {
    String stackName = "HarnessStack-test";
    long stackEventTs = 1000;
    Date timeStamp = new Date();
    Stack testStack = new Stack().withStackStatus("CREATE_COMPLETE").withStackName(stackName);
    LogCallback logCallback = mock(ExecutionLogCallback.class);
    StackEvent stackEvent = new StackEvent()
                                .withStackName(stackName)
                                .withEventId("id")
                                .withResourceStatusReason("statusReason")
                                .withTimestamp(timeStamp);
    when(awsCloudformationClient.getAllStackEvents(any(), any(), any())).thenReturn(singletonList(stackEvent));

    long resStackEventTs = cloudFormationBaseHelper.printStackEvents(
        AwsInternalConfig.builder().build(), "us-east-1", stackEventTs, testStack, logCallback);
    String message =
        String.format("[%s] [%s] [%s] [%s] [%s]", stackEvent.getResourceStatus(), stackEvent.getResourceType(),
            stackEvent.getLogicalResourceId(), "statusReason", stackEvent.getPhysicalResourceId());
    verify(logCallback).saveExecutionLog(message);
    assertThat(resStackEventTs).isEqualTo(timeStamp.getTime());
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetCloudformationTags() throws IOException {
    String tagsJson = "[{\"key\":\"testKey\",\"value\":\"testValue\"}]";
    List<Tag> tags = cloudFormationBaseHelper.getCloudformationTags(tagsJson);
    assertThat(tags.size()).isEqualTo(1);
    List<Tag> emptyTags = cloudFormationBaseHelper.getCloudformationTags("");
    assertThat(emptyTags).isNull();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetCapabilities() {
    AwsInternalConfig awsInternalConfig = AwsInternalConfig.builder().build();
    doReturn(Arrays.asList("RETURNED_CAPABILITY"))
        .when(awsCFHelperServiceDelegate)
        .getCapabilities(any(), any(), any(), any());
    Set<String> capabilities = cloudFormationBaseHelper.getCapabilities(
        awsInternalConfig, "region", "templateUrl", Arrays.asList("USER_CAPABILITY"), "s3");
    verify(awsCFHelperServiceDelegate).getCapabilities(eq(awsInternalConfig), anyString(), anyString(), anyString());
    assertThat(capabilities.size()).isEqualTo(2);
    assertThat(capabilities).contains("RETURNED_CAPABILITY", "USER_CAPABILITY");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testPrintStackResources() {
    String stackName = "HarnessStack-test";
    Date timeStamp = new Date();
    Stack stack = new Stack().withStackStatus("CREATE_COMPLETE").withStackName(stackName);
    LogCallback logCallback = mock(ExecutionLogCallback.class);
    StackResource stackResource =
        new StackResource().withStackName(stackName).withResourceStatusReason("statusReason").withTimestamp(timeStamp);
    when(awsCloudformationClient.getAllStackResources(any(), any(), any())).thenReturn(singletonList(stackResource));

    cloudFormationBaseHelper.printStackResources(AwsInternalConfig.builder().build(), "us-east-1", stack, logCallback);
    String message = String.format("[%s] [%s] [%s] [%s] [%s]", stackResource.getResourceStatus(),
        stackResource.getResourceType(), stackResource.getLogicalResourceId(), stackResource.getResourceStatusReason(),
        stackResource.getPhysicalResourceId());
    verify(logCallback).saveExecutionLog(message);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetExistingStackInfo() {
    AwsInternalConfig awsInternalConfig = AwsInternalConfig.builder().build();
    Parameter parameter = new Parameter().withParameterKey("testKey").withParameterValue("testValue");
    Stack stack = new Stack()
                      .withStackStatus("CREATE_COMPLETE")
                      .withStackName("stackName")
                      .withStackId("stackId")
                      .withParameters(parameter);
    doReturn("stackBody").when(awsCFHelperServiceDelegate).getStackBody(any(), any(), any());
    ExistingStackInfo existingStackInfo =
        cloudFormationBaseHelper.getExistingStackInfo(awsInternalConfig, "region", stack);
    assertThat(existingStackInfo.getOldStackParameters().get("testKey")).endsWith("testValue");
    assertThat(existingStackInfo.getOldStackBody()).isEqualTo("stackBody");
    assertThat(existingStackInfo.isStackExisted()).isTrue();
  }
}
