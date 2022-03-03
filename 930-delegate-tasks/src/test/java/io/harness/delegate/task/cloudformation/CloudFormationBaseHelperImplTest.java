/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.cloudformation;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.task.cloudformation.CloudformationBaseHelperImpl.CLOUDFORMATION_STACK_CREATE_BODY;
import static io.harness.delegate.task.cloudformation.CloudformationBaseHelperImpl.CLOUDFORMATION_STACK_CREATE_GIT;
import static io.harness.delegate.task.cloudformation.CloudformationBaseHelperImpl.CLOUDFORMATION_STACK_CREATE_URL;
import static io.harness.rule.OwnerRule.PRAKHAR;
import static io.harness.rule.OwnerRule.RAGHVENDRA;
import static io.harness.rule.OwnerRule.TMACARI;

import static com.amazonaws.services.cloudformation.model.StackStatus.UPDATE_ROLLBACK_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.UPDATE_ROLLBACK_FAILED;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.aws.AWSCloudformationClient;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.category.element.UnitTests;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.cloudformation.request.CloudformationTaskInternalRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackInternalResponse;
import software.wings.helpers.ext.cloudformation.response.CloudformationTaskInternalResponse;
import software.wings.helpers.ext.cloudformation.response.ExistingStackInfo;
import software.wings.service.intfc.aws.delegate.AwsCFHelperServiceDelegate;
import software.wings.service.intfc.security.EncryptionService;

import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.cloudformation.model.UpdateStackResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class CloudFormationBaseHelperImplTest extends CategoryTest {
  private static final String ACCOUNT_ID = "accountId";
  private static final String APP_ID = "appId";
  private static final String ACTIVITY_ID = "activityId";

  @Mock private AwsCFHelperServiceDelegate awsCFHelperServiceDelegate;
  @Mock private AWSCloudformationClient awsHelperService;
  @Mock protected EncryptionService encryptionService;
  @Mock private ExecutionLogCallback executionLogCallback;
  @InjectMocks private CloudformationBaseHelperImpl cloudFormationBaseHelper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PRAKHAR)
  @Category(UnitTests.class)
  public void testGetCloudformationTags() throws IOException {
    CloudformationTaskInternalRequest cloudFormationCreateStackRequest =
        CloudformationTaskInternalRequest.builder().build();
    assertThat(cloudFormationBaseHelper.getCloudformationTags(cloudFormationCreateStackRequest)).isNull();

    cloudFormationCreateStackRequest.setTags("");
    assertThat(cloudFormationBaseHelper.getCloudformationTags(cloudFormationCreateStackRequest)).isNull();

    cloudFormationCreateStackRequest.setTags("[]");
    assertThat(cloudFormationBaseHelper.getCloudformationTags(cloudFormationCreateStackRequest))
        .isEqualTo(new ArrayList<Tag>());

    cloudFormationCreateStackRequest.setTags(
        "[{\r\n\t\"key\": \"tagKey1\",\r\n\t\"value\": \"tagValue1\"\r\n}, {\r\n\t\"key\": \"tagKey2\",\r\n\t\"value\": \"tagValue2\"\r\n}]");
    List<Tag> expectedTags = Arrays.asList(
        new Tag().withKey("tagKey1").withValue("tagValue1"), new Tag().withKey("tagKey2").withValue("tagValue2"));
    assertThat(cloudFormationBaseHelper.getCloudformationTags(cloudFormationCreateStackRequest))
        .isEqualTo(expectedTags);
  }

  @Test
  @Owner(developers = PRAKHAR)
  @Category(UnitTests.class)
  public void testGetCapabilities() throws IOException {
    List<String> capabilitiesByTemplateSummary = Arrays.asList("CAPABILITY_IAM", "CAPABILITY_AUTO_EXPAND");
    List<String> userDefinedCapabilities = singletonList("CAPABILITY_AUTO_EXPAND");
    doReturn(capabilitiesByTemplateSummary)
        .when(awsCFHelperServiceDelegate)
        .getCapabilities(any(AwsInternalConfig.class), anyString(), anyString(), anyString());

    List<String> expectedCapabilities = Arrays.asList("CAPABILITY_IAM", "CAPABILITY_AUTO_EXPAND");
    assertThat(cloudFormationBaseHelper.getCapabilities(
                   AwsInternalConfig.builder().build(), "us-east-2", "data", userDefinedCapabilities, "type"))
        .hasSameElementsAs(expectedCapabilities);

    userDefinedCapabilities = null;
    assertThat(cloudFormationBaseHelper.getCapabilities(
                   AwsInternalConfig.builder().build(), "us-east-2", "data", userDefinedCapabilities, "type"))
        .hasSameElementsAs(expectedCapabilities);

    userDefinedCapabilities = singletonList("CAPABILITY_AUTO_EXPAND");
    expectedCapabilities = singletonList("CAPABILITY_AUTO_EXPAND");
    doReturn(Collections.emptyList())
        .when(awsCFHelperServiceDelegate)
        .getCapabilities(any(AwsInternalConfig.class), anyString(), anyString(), anyString());
    assertThat(cloudFormationBaseHelper.getCapabilities(
                   AwsInternalConfig.builder().build(), "us-east-2", "data", userDefinedCapabilities, "type"))
        .hasSameElementsAs(expectedCapabilities);
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testPrintStackEvents() {
    String stackName = "HarnessStack-test";
    long stackEventTs = 1000;
    String roleArn = "roleArn";
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
    Date timeStamp = new Date();
    CloudformationTaskInternalRequest request =
        CloudformationTaskInternalRequest.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .cloudFormationRoleArn(roleArn)
            .commandName("Create Stack")
            .awsConfig(AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .createType(CLOUDFORMATION_STACK_CREATE_BODY)
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    Stack testStack = new Stack().withStackStatus("CREATE_COMPLETE").withStackName(stackName + stackNameSuffix);
    ExecutionLogCallback logCallback = mock(ExecutionLogCallback.class);
    StackEvent stackEvent = new StackEvent()
                                .withStackName(stackName)
                                .withEventId("id")
                                .withResourceStatusReason("statusReason")
                                .withTimestamp(timeStamp);
    when(awsHelperService.getAllStackEvents(any(), any(), any())).thenReturn(singletonList(stackEvent));

    long resStackEventTs = cloudFormationBaseHelper.printStackEvents(request, stackEventTs, testStack, logCallback);
    String message = format("[%s] [%s] [%s] [%s] [%s]", stackEvent.getResourceStatus(), stackEvent.getResourceType(),
        stackEvent.getLogicalResourceId(), "statusReason", stackEvent.getPhysicalResourceId());
    verify(logCallback).saveExecutionLog(message);
    assertThat(resStackEventTs).isEqualTo(timeStamp.getTime());
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testCreateStack() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
    String roleArn = "roleArn";
    CloudformationTaskInternalRequest request =
        CloudformationTaskInternalRequest.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .cloudFormationRoleArn(roleArn)
            .commandName("Create Stack")
            .awsConfig(AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .createType(CLOUDFORMATION_STACK_CREATE_BODY)
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    String stackId = "Stack Id 00";
    CreateStackResult createStackResult = new CreateStackResult().withStackId(stackId);
    doReturn(createStackResult).when(awsHelperService).createStack(anyString(), any(), any());
    List<Stack> createProgressList = singletonList(new Stack().withStackStatus("CREATE_IN_PROGRESS"));
    List<Stack> createCompleteList =
        singletonList(new Stack()
                          .withStackStatus("CREATE_COMPLETE")
                          .withOutputs(new Output().withOutputKey("vpcs").withOutputValue("vpcs"),
                              new Output().withOutputKey("subnets").withOutputValue("subnets"),
                              new Output().withOutputKey("securityGroups").withOutputValue("sgs")));
    doReturn(createProgressList)
        .doReturn(createCompleteList)
        .when(awsHelperService)
        .getAllStacks(anyString(), any(), any());

    CloudformationTaskInternalResponse response = cloudFormationBaseHelper.createStack(request, executionLogCallback);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    CloudFormationCreateStackInternalResponse formationCommandResponse =
        response.getCloudFormationCreateStackInternalResponse();
    assertThat(formationCommandResponse).isNotNull();
    Map<String, Object> outputMap = formationCommandResponse.getCloudFormationOutputMap();
    assertThat(3).isEqualTo(outputMap.size());
    validateMapContents(outputMap, "vpcs", "vpcs");
    validateMapContents(outputMap, "subnets", "subnets");
    validateMapContents(outputMap, "securityGroups", "sgs");
    ExistingStackInfo existingStackInfo = formationCommandResponse.getExistingStackInfo();
    assertThat(existingStackInfo).isNotNull();
    assertThat(existingStackInfo.isStackExisted()).isFalse();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testCreateStackWithNoStacksFound() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
    String roleArn = "roleArn";
    CloudformationTaskInternalRequest request =
        CloudformationTaskInternalRequest.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .cloudFormationRoleArn(roleArn)
            .commandName("Create Stack")
            .awsConfig(AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .createType(CLOUDFORMATION_STACK_CREATE_BODY)
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    String stackId = "Stack Id 00";
    CreateStackResult createStackResult = new CreateStackResult().withStackId(stackId);
    doReturn(createStackResult).when(awsHelperService).createStack(anyString(), any(), any());
    List<Stack> createProgressList = emptyList();
    doReturn(createProgressList).when(awsHelperService).getAllStacks(anyString(), any(), any());

    CloudformationTaskInternalResponse response = cloudFormationBaseHelper.createStack(request, executionLogCallback);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    CloudFormationCreateStackInternalResponse formationCommandResponse =
        response.getCloudFormationCreateStackInternalResponse();
    assertThat(formationCommandResponse).isNull();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testCreateStackGit() {
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String data = "data";
    String stackNameSuffix = "Stack Name 00";
    CloudformationTaskInternalRequest request =
        CloudformationTaskInternalRequest.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .commandName("Create Stack")
            .awsConfig(AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .data(data)
            .createType(CLOUDFORMATION_STACK_CREATE_GIT)
            .stackNameSuffix(stackNameSuffix)
            .build();
    String stackId = "Stack Id 00";
    CreateStackResult createStackResult = new CreateStackResult().withStackId(stackId);
    doReturn(createStackResult).when(awsHelperService).createStack(anyString(), any(), any());
    List<Stack> createProgressList = singletonList(new Stack().withStackStatus("CREATE_IN_PROGRESS"));
    List<Stack> createCompleteList =
        singletonList(new Stack()
                          .withStackStatus("CREATE_COMPLETE")
                          .withOutputs(new Output().withOutputKey("vpcs").withOutputValue("vpcs"),
                              new Output().withOutputKey("subnets").withOutputValue("subnets"),
                              new Output().withOutputKey("securityGroups").withOutputValue("sgs")));
    doReturn(createProgressList)
        .doReturn(createCompleteList)
        .when(awsHelperService)
        .getAllStacks(anyString(), any(), any());

    CloudformationTaskInternalResponse response = cloudFormationBaseHelper.createStack(request, executionLogCallback);

    assertThat(response).isNotNull();
    assertThat(data).isEqualTo(request.getData());
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    CloudFormationCreateStackInternalResponse formationCommandResponse =
        response.getCloudFormationCreateStackInternalResponse();
    assertThat(formationCommandResponse).isNotNull();
    Map<String, Object> outputMap = formationCommandResponse.getCloudFormationOutputMap();
    assertThat(3).isEqualTo(outputMap.size());
    validateMapContents(outputMap, "vpcs", "vpcs");
    validateMapContents(outputMap, "subnets", "subnets");
    validateMapContents(outputMap, "securityGroups", "sgs");
    ExistingStackInfo existingStackInfo = formationCommandResponse.getExistingStackInfo();
    assertThat(existingStackInfo).isNotNull();
    assertThat(existingStackInfo.isStackExisted()).isFalse();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testUpdateStack() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
    CloudformationTaskInternalRequest request =
        CloudformationTaskInternalRequest.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .commandName("Create Stack")
            .awsConfig(AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .createType(CLOUDFORMATION_STACK_CREATE_BODY)
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    Stack exitingStack =
        new Stack().withStackStatus("CREATE_COMPLETE").withStackName("HarnessStack-" + stackNameSuffix);
    List<Stack> updateProgressList = singletonList(new Stack().withStackStatus("UPDATE_IN_PROGRESS"));
    List<Stack> updateCompleteList = singletonList(new Stack().withStackStatus("UPDATE_COMPLETE"));
    doReturn(updateProgressList)
        .doReturn(updateCompleteList)
        .when(awsHelperService)
        .getAllStacks(anyString(), any(), any());
    UpdateStackResult updateStackResult = new UpdateStackResult();
    updateStackResult.setStackId("StackId1");
    doReturn(updateStackResult).when(awsHelperService).updateStack(anyString(), any(), any());
    doReturn("Body").when(awsCFHelperServiceDelegate).getStackBody(any(), anyString(), anyString());

    CloudformationTaskInternalResponse response =
        cloudFormationBaseHelper.updateStack(request, exitingStack, executionLogCallback);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    CloudFormationCreateStackInternalResponse formationCommandResponse =
        response.getCloudFormationCreateStackInternalResponse();
    assertThat(formationCommandResponse).isNotNull();
    ExistingStackInfo existingStackInfo = formationCommandResponse.getExistingStackInfo();
    assertThat(existingStackInfo).isNotNull();
    assertThat(existingStackInfo.isStackExisted()).isTrue();
    assertThat("Body").isEqualTo(existingStackInfo.getOldStackBody());
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testUpdateStackWithExistingStackInUpdateRollbackCompleteState() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
    CloudformationTaskInternalRequest request =
        CloudformationTaskInternalRequest.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .commandName("Create Stack")
            .awsConfig(AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .createType(CLOUDFORMATION_STACK_CREATE_BODY)
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    Stack exitingStack =
        new Stack().withStackStatus("CREATE_COMPLETE").withStackName("HarnessStack-" + stackNameSuffix);
    List<Stack> updateCompleteList =
        singletonList(new Stack().withStackStatus("UPDATE_ROLLBACK_COMPLETE").withStackId("stackId1"));
    doReturn(updateCompleteList).when(awsHelperService).getAllStacks(anyString(), any(), any());
    UpdateStackResult updateStackResult = new UpdateStackResult();
    doReturn(updateStackResult).when(awsHelperService).updateStack(anyString(), any(), any());
    doReturn("Body").when(awsCFHelperServiceDelegate).getStackBody(any(), anyString(), anyString());

    CloudformationTaskInternalResponse response =
        cloudFormationBaseHelper.updateStack(request, exitingStack, executionLogCallback);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    CloudFormationCreateStackInternalResponse formationCommandResponse =
        response.getCloudFormationCreateStackInternalResponse();
    assertThat(formationCommandResponse).isNotNull();
    assertThat(formationCommandResponse.getStackId()).isEqualTo("stackId1");
    ExistingStackInfo existingStackInfo = formationCommandResponse.getExistingStackInfo();
    assertThat(existingStackInfo).isNotNull();
    assertThat(existingStackInfo.isStackExisted()).isTrue();
    assertThat("Body").isEqualTo(existingStackInfo.getOldStackBody());
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testUpdateStackWithExistingStackInUpdateRollbackFailedState() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
    CloudformationTaskInternalRequest request =
        CloudformationTaskInternalRequest.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .commandName("Create Stack")
            .awsConfig(AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .createType(CLOUDFORMATION_STACK_CREATE_BODY)
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    Stack exitingStack =
        new Stack().withStackStatus("CREATE_COMPLETE").withStackName("HarnessStack-" + stackNameSuffix);
    List<Stack> updateCompleteList =
        singletonList(new Stack().withStackStatus("UPDATE_ROLLBACK_FAILED").withStackId("stackId1"));
    doReturn(updateCompleteList).when(awsHelperService).getAllStacks(anyString(), any(), any());
    UpdateStackResult updateStackResult = new UpdateStackResult();
    doReturn(updateStackResult).when(awsHelperService).updateStack(anyString(), any(), any());
    doReturn("Body").when(awsCFHelperServiceDelegate).getStackBody(any(), anyString(), anyString());

    CloudformationTaskInternalResponse response =
        cloudFormationBaseHelper.updateStack(request, exitingStack, executionLogCallback);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    assertThat(response.getErrorMessage())
        .isEqualTo(
            "# Existing stack with name null is already in status: UPDATE_ROLLBACK_FAILED, therefore exiting with failure");
    CloudFormationCreateStackInternalResponse formationCommandResponse =
        response.getCloudFormationCreateStackInternalResponse();
    assertThat(formationCommandResponse).isNotNull();
    assertThat(formationCommandResponse.getStackId()).isEqualTo("stackId1");
    assertThat(formationCommandResponse.getStackStatus()).isEqualTo(UPDATE_ROLLBACK_FAILED.name());
    ExistingStackInfo existingStackInfo = formationCommandResponse.getExistingStackInfo();
    assertThat(existingStackInfo).isNotNull();
    assertThat(existingStackInfo.isStackExisted()).isTrue();
    assertThat("Body").isEqualTo(existingStackInfo.getOldStackBody());
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testUpdateStackWithStackStatusToMarkAsSuccess() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
    CloudformationTaskInternalRequest request =
        CloudformationTaskInternalRequest.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .commandName("Create Stack")
            .awsConfig(AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .stackStatusesToMarkAsSuccess(singletonList(UPDATE_ROLLBACK_COMPLETE))
            .createType(CLOUDFORMATION_STACK_CREATE_BODY)
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    Stack exitingStack =
        new Stack().withStackStatus("CREATE_COMPLETE").withStackName("HarnessStack-" + stackNameSuffix);
    List<Stack> updateProgressList = singletonList(new Stack().withStackStatus("UPDATE_IN_PROGRESS"));
    List<Stack> updateCompleteList = singletonList(new Stack().withStackStatus("UPDATE_ROLLBACK_COMPLETE"));
    doReturn(updateProgressList)
        .doReturn(updateCompleteList)
        .when(awsHelperService)
        .getAllStacks(anyString(), any(), any());
    UpdateStackResult updateStackResult = new UpdateStackResult();
    updateStackResult.setStackId("StackId1");
    doReturn(updateStackResult).when(awsHelperService).updateStack(anyString(), any(), any());
    doReturn("Body").when(awsCFHelperServiceDelegate).getStackBody(any(), anyString(), anyString());

    CloudformationTaskInternalResponse response =
        cloudFormationBaseHelper.updateStack(request, exitingStack, executionLogCallback);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    CloudFormationCreateStackInternalResponse formationCommandResponse =
        response.getCloudFormationCreateStackInternalResponse();
    assertThat(formationCommandResponse).isNotNull();
    ExistingStackInfo existingStackInfo = formationCommandResponse.getExistingStackInfo();
    assertThat(existingStackInfo).isNotNull();
    assertThat(existingStackInfo.isStackExisted()).isTrue();
    assertThat("Body").isEqualTo(existingStackInfo.getOldStackBody());
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testUpdateStackGit() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
    CloudformationTaskInternalRequest request =
        CloudformationTaskInternalRequest.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .commandName("Create Stack")
            .awsConfig(AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .createType(CLOUDFORMATION_STACK_CREATE_GIT)
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    Stack exitingStack =
        new Stack().withStackStatus("CREATE_COMPLETE").withStackName("HarnessStack-" + stackNameSuffix);
    List<Stack> updateProgressList = singletonList(new Stack().withStackStatus("UPDATE_IN_PROGRESS"));
    List<Stack> updateCompleteList = singletonList(new Stack().withStackStatus("UPDATE_COMPLETE"));
    doReturn(updateProgressList)
        .doReturn(updateCompleteList)
        .when(awsHelperService)
        .getAllStacks(anyString(), any(), any());
    UpdateStackResult updateStackResult = new UpdateStackResult();
    updateStackResult.setStackId("StackId1");
    doReturn(updateStackResult).when(awsHelperService).updateStack(anyString(), any(), any());
    doReturn("Body").when(awsCFHelperServiceDelegate).getStackBody(any(), anyString(), anyString());

    CloudformationTaskInternalResponse response =
        cloudFormationBaseHelper.updateStack(request, exitingStack, executionLogCallback);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    CloudFormationCreateStackInternalResponse formationCommandResponse =
        response.getCloudFormationCreateStackInternalResponse();
    assertThat(formationCommandResponse).isNotNull();
    ExistingStackInfo existingStackInfo = formationCommandResponse.getExistingStackInfo();
    assertThat(existingStackInfo).isNotNull();
    assertThat(existingStackInfo.isStackExisted()).isTrue();
    assertThat("Body").isEqualTo(existingStackInfo.getOldStackBody());
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testDeleteStack() {
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 01";
    String roleArn = "RoleArn";
    CloudformationTaskInternalRequest request =
        CloudformationTaskInternalRequest.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .commandName("Delete Stack")
            .cloudFormationRoleArn(roleArn)
            .awsConfig(AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .stackNameSuffix(stackNameSuffix)
            .build();
    String stackId = "Stack Id 01";
    List<Stack> deleteInProgressList =
        singletonList(new Stack().withStackId(stackId).withStackStatus("DELETE_IN_PROGRESS"));
    List<Stack> deleteCompleteList = singletonList(new Stack().withStackId(stackId).withStackStatus("DELETE_COMPLETE"));
    doReturn(deleteInProgressList)
        .doReturn(deleteCompleteList)
        .when(awsHelperService)
        .getAllStacks(anyString(), any(), any());
    CloudformationTaskInternalResponse response =
        cloudFormationBaseHelper.deleteStack(stackId, "HarnessStack-" + stackNameSuffix, request, executionLogCallback);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    verify(awsHelperService).deleteStack(anyString(), any(), any());
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testDeleteStackWithException() {
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 01";
    String roleArn = "RoleArn";
    CloudformationTaskInternalRequest request =
        CloudformationTaskInternalRequest.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .commandName("Delete Stack")
            .cloudFormationRoleArn(roleArn)
            .awsConfig(AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .stackNameSuffix(stackNameSuffix)
            .build();
    Exception ex = new RuntimeException("This is an exception");
    String stackId = "Stack Id 01";
    doThrow(ex).when(awsHelperService).deleteStack(anyString(), any(), any());
    CloudformationTaskInternalResponse response =
        cloudFormationBaseHelper.deleteStack(stackId, "HarnessStack-" + stackNameSuffix, request, executionLogCallback);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testDeleteStackWithTimeout() {
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 01";
    String roleArn = "RoleArn";
    CloudformationTaskInternalRequest request =
        CloudformationTaskInternalRequest.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .commandName("Delete Stack")
            .cloudFormationRoleArn(roleArn)
            .awsConfig(AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build())
            .timeoutInMs(1)
            .stackNameSuffix(stackNameSuffix)
            .build();
    String stackId = "Stack Id 01";
    List<Stack> stackList = singletonList(new Stack().withStackId(stackId).withStackStatus("DELETE_IN_PROGRESS"));
    doReturn(stackList).when(awsHelperService).getAllStacks(anyString(), any(), any());
    CloudformationTaskInternalResponse response =
        cloudFormationBaseHelper.deleteStack(stackId, "HarnessStack-" + stackNameSuffix, request, executionLogCallback);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    verify(awsHelperService).deleteStack(anyString(), any(), any());
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testDeleteStackStatusDeleteFailed() {
    testFailureForDeleteStackStatus("DELETE_FAILED");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testDeleteStackStatusUnknown() {
    testFailureForDeleteStackStatus("Unknown");
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testCreateStackCreateTypeUrl() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
    String roleArn = "roleArn";
    CloudformationTaskInternalRequest request =
        CloudformationTaskInternalRequest.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .cloudFormationRoleArn(roleArn)
            .commandName("Create Stack")
            .awsConfig(AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .createType(CLOUDFORMATION_STACK_CREATE_URL)
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    String stackId = "Stack Id 00";
    CreateStackResult createStackResult = new CreateStackResult().withStackId(stackId);
    doReturn(createStackResult).when(awsHelperService).createStack(anyString(), any(), any());
    List<Stack> createProgressList = singletonList(new Stack().withStackStatus("CREATE_IN_PROGRESS"));
    List<Stack> createCompleteList =
        singletonList(new Stack()
                          .withStackStatus("CREATE_COMPLETE")
                          .withOutputs(new Output().withOutputKey("vpcs").withOutputValue("vpcs"),
                              new Output().withOutputKey("subnets").withOutputValue("subnets"),
                              new Output().withOutputKey("securityGroups").withOutputValue("sgs")));
    doReturn(createProgressList)
        .doReturn(createCompleteList)
        .when(awsHelperService)
        .getAllStacks(anyString(), any(), any());

    CloudformationTaskInternalResponse response = cloudFormationBaseHelper.createStack(request, executionLogCallback);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    CloudFormationCreateStackInternalResponse formationCommandResponse =
        response.getCloudFormationCreateStackInternalResponse();
    assertThat(formationCommandResponse).isNotNull();
    Map<String, Object> outputMap = formationCommandResponse.getCloudFormationOutputMap();
    assertThat(3).isEqualTo(outputMap.size());
    validateMapContents(outputMap, "vpcs", "vpcs");
    validateMapContents(outputMap, "subnets", "subnets");
    validateMapContents(outputMap, "securityGroups", "sgs");
    ExistingStackInfo existingStackInfo = formationCommandResponse.getExistingStackInfo();
    assertThat(existingStackInfo).isNotNull();
    assertThat(existingStackInfo.isStackExisted()).isFalse();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testCreateStackCreateTypeUnknown() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
    String roleArn = "roleArn";
    CloudformationTaskInternalRequest request =
        CloudformationTaskInternalRequest.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .cloudFormationRoleArn(roleArn)
            .commandName("Create Stack")
            .awsConfig(AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .createType("Unknown")
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    String stackId = "Stack Id 00";
    CreateStackResult createStackResult = new CreateStackResult().withStackId(stackId);
    doReturn(createStackResult).when(awsHelperService).createStack(anyString(), any(), any());
    List<Stack> createProgressList = singletonList(new Stack().withStackStatus("CREATE_IN_PROGRESS"));
    List<Stack> createCompleteList =
        singletonList(new Stack()
                          .withStackStatus("CREATE_COMPLETE")
                          .withOutputs(new Output().withOutputKey("vpcs").withOutputValue("vpcs"),
                              new Output().withOutputKey("subnets").withOutputValue("subnets"),
                              new Output().withOutputKey("securityGroups").withOutputValue("sgs")));
    doReturn(createProgressList)
        .doReturn(createCompleteList)
        .when(awsHelperService)
        .getAllStacks(anyString(), any(), any());

    CloudformationTaskInternalResponse response = cloudFormationBaseHelper.createStack(request, executionLogCallback);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    CloudFormationCreateStackInternalResponse formationCommandResponse =
        response.getCloudFormationCreateStackInternalResponse();
    assertThat(formationCommandResponse).isNull();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testUpdateStackUpdateTypeUnknown() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
    CloudformationTaskInternalRequest request =
        CloudformationTaskInternalRequest.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .commandName("Create Stack")
            .awsConfig(AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .createType("Unknown")
            .data(templateBody)
            .cloudFormationRoleArn("testRole")
            .stackNameSuffix(stackNameSuffix)
            .build();
    Stack exitingStack =
        new Stack().withStackStatus("CREATE_COMPLETE").withStackName("HarnessStack-" + stackNameSuffix);
    List<Stack> updateProgressList = singletonList(new Stack().withStackStatus("UPDATE_IN_PROGRESS"));
    List<Stack> updateCompleteList = singletonList(new Stack().withStackStatus("UPDATE_COMPLETE"));
    doReturn(updateProgressList)
        .doReturn(updateCompleteList)
        .when(awsHelperService)
        .getAllStacks(anyString(), any(), any());
    doReturn("Body").when(awsCFHelperServiceDelegate).getStackBody(any(), anyString(), anyString());

    CloudformationTaskInternalResponse response =
        cloudFormationBaseHelper.updateStack(request, exitingStack, executionLogCallback);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    CloudFormationCreateStackInternalResponse formationCommandResponse =
        response.getCloudFormationCreateStackInternalResponse();
    assertThat(formationCommandResponse).isNull();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testUpdateStackUpdateTypeUrl() {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
    CloudformationTaskInternalRequest request =
        CloudformationTaskInternalRequest.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .commandName("Create Stack")
            .awsConfig(AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .createType(CLOUDFORMATION_STACK_CREATE_URL)
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    Stack exitingStack =
        new Stack().withStackStatus("CREATE_COMPLETE").withStackName("HarnessStack-" + stackNameSuffix);
    List<Stack> updateProgressList = singletonList(new Stack().withStackStatus("UPDATE_IN_PROGRESS"));
    List<Stack> updateCompleteList = singletonList(new Stack().withStackStatus("UPDATE_COMPLETE"));
    doReturn(updateProgressList)
        .doReturn(updateCompleteList)
        .when(awsHelperService)
        .getAllStacks(anyString(), any(), any());

    UpdateStackResult updateStackResult = new UpdateStackResult();
    updateStackResult.setStackId("StackId1");
    doReturn(updateStackResult).when(awsHelperService).updateStack(anyString(), any(), any());
    doReturn("Body").when(awsCFHelperServiceDelegate).getStackBody(any(), anyString(), anyString());

    CloudformationTaskInternalResponse response =
        cloudFormationBaseHelper.updateStack(request, exitingStack, executionLogCallback);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    CloudFormationCreateStackInternalResponse formationCommandResponse =
        response.getCloudFormationCreateStackInternalResponse();
    assertThat(formationCommandResponse).isNotNull();
    ExistingStackInfo existingStackInfo = formationCommandResponse.getExistingStackInfo();
    assertThat(existingStackInfo).isNotNull();
    assertThat(existingStackInfo.isStackExisted()).isTrue();
    assertThat("Body").isEqualTo(existingStackInfo.getOldStackBody());
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testCreateStackRollbackProgressStatus() {
    testFailureForCreateStackStatus("ROLLBACK_IN_PROGRESS");
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testCreateStackCreateFailedStatus() {
    testFailureForCreateStackStatus("CREATE_FAILED");
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testCreateStackRollbackFailedStatus() {
    testFailureForCreateStackStatus("ROLLBACK_FAILED");
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testCreateStackRollbackCompleteStatus() {
    testFailureForCreateStackStatus("ROLLBACK_COMPLETE");
  }

  @Test
  @Owner(developers = RAGHVENDRA)
  @Category(UnitTests.class)
  public void testCreateStackDefaultStatus() {
    testFailureForCreateStackStatus("Unknown");
  }

  private void testFailureForDeleteStackStatus(String status) {
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 01";
    String roleArn = "RoleArn";
    CloudformationTaskInternalRequest request =
        CloudformationTaskInternalRequest.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .commandName("Delete Stack")
            .cloudFormationRoleArn(roleArn)
            .awsConfig(AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .stackNameSuffix(stackNameSuffix)
            .build();
    String stackId = "Stack Id 01";

    List<Stack> stackList = singletonList(new Stack().withStackId(stackId).withStackStatus(status));
    doReturn(stackList).when(awsHelperService).getAllStacks(anyString(), any(), any());
    CloudformationTaskInternalResponse response =
        cloudFormationBaseHelper.deleteStack(stackId, "HarnessStack-" + stackNameSuffix, request, executionLogCallback);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    verify(awsHelperService).deleteStack(anyString(), any(), any());
  }

  private void testFailureForCreateStackStatus(String status) {
    String templateBody = "Template Body";
    char[] accessKey = "abcd".toCharArray();
    char[] secretKey = "pqrs".toCharArray();
    String stackNameSuffix = "Stack Name 00";
    String roleArn = "roleArn";
    CloudformationTaskInternalRequest request =
        CloudformationTaskInternalRequest.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .activityId(ACTIVITY_ID)
            .cloudFormationRoleArn(roleArn)
            .commandName("Create Stack")
            .awsConfig(AwsInternalConfig.builder().accessKey(accessKey).secretKey(secretKey).build())
            .timeoutInMs(10 * 60 * 1000)
            .createType(CLOUDFORMATION_STACK_CREATE_BODY)
            .data(templateBody)
            .stackNameSuffix(stackNameSuffix)
            .build();
    String stackId = "Stack Id 00";
    CreateStackResult createStackResult = new CreateStackResult().withStackId(stackId);
    doReturn(createStackResult).when(awsHelperService).createStack(anyString(), any(), any());
    List<Stack> rollbackList =
        singletonList(new Stack().withStackStatus(status).withStackName("HarnessStack-" + stackNameSuffix));
    List<Stack> rollbackCompleteList = singletonList(
        new Stack().withStackStatus("ROLLBACK_COMPLETE").withStackName("HarnessStack-" + stackNameSuffix));

    if ("ROLLBACK_IN_PROGRESS".equalsIgnoreCase(status)) {
      doReturn(rollbackList)
          .doReturn(rollbackCompleteList)
          .when(awsHelperService)
          .getAllStacks(anyString(), any(), any());
    } else {
      doReturn(rollbackList).when(awsHelperService).getAllStacks(anyString(), any(), any());
    }

    CloudformationTaskInternalResponse response = cloudFormationBaseHelper.createStack(request, executionLogCallback);

    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    CloudFormationCreateStackInternalResponse formationCommandResponse =
        response.getCloudFormationCreateStackInternalResponse();
    CloudFormationCreateStackInternalResponse expectedCreateStackResponse =
        CloudFormationCreateStackInternalResponse.builder().stackStatus(status).build();
    if ("ROLLBACK_IN_PROGRESS".equalsIgnoreCase(status)) {
      expectedCreateStackResponse =
          CloudFormationCreateStackInternalResponse.builder().stackStatus("ROLLBACK_COMPLETE").build();
    }

    assertThat(formationCommandResponse).isEqualTo(expectedCreateStackResponse);
  }

  private void validateMapContents(Map<String, Object> map, String key, String value) {
    assertThat(map.containsKey(key)).isTrue();
    assertThat(value).isEqualTo((String) map.get(key));
  }
}
