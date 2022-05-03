/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.cloudformation.handlers;

import static io.harness.delegate.task.cloudformation.CloudformationTaskNGParameters.CloudformationTaskNGParametersBuilder;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.rule.OwnerRule.VLICA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.AWSCloudformationClient;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.task.aws.AwsNgConfigMapper;
import io.harness.delegate.task.cloudformation.CloudformationBaseHelper;
import io.harness.delegate.task.cloudformation.CloudformationTaskNGParameters;
import io.harness.delegate.task.cloudformation.CloudformationTaskNGResponse;
import io.harness.delegate.task.cloudformation.CloudformationTaskType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.rule.Owner;
import io.harness.security.encryption.SecretDecryptionService;

import software.wings.helpers.ext.cloudformation.response.ExistingStackInfo;
import software.wings.service.intfc.aws.delegate.AwsCFHelperServiceDelegate;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.UpdateStackResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.CDP)
public class CloudformationCreateStackTaskHandlerTest {
  @Mock private CloudformationBaseHelper cloudformationBaseHelper;
  @Mock private AWSCloudformationClient awsCloudformationClient;
  @Mock private AwsCFHelperServiceDelegate awsCFHelperServiceDelegate;
  @Mock private SecretDecryptionService encryptionService;
  @Mock private LogCallback logCallback;
  @Mock private AwsNgConfigMapper awsNgConfigMapper;

  @InjectMocks CloudformationCreateStackTaskHandler createStackTaskHandler;

  CloudformationTaskNGParametersBuilder parameters;
  private CreateStackResult createStackResult;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    parameters = CloudformationTaskNGParameters.builder()
                     .awsConnector(AwsConnectorDTO.builder()
                                       .credential(AwsCredentialDTO.builder()
                                                       .awsCredentialType(AwsCredentialType.MANUAL_CREDENTIALS)
                                                       .config(AwsManualConfigSpecDTO.builder().build())
                                                       .build())
                                       .build())
                     .region("test-region")
                     .cloudFormationRoleArn("test-role-arn")
                     .taskType(CloudformationTaskType.CREATE_STACK)
                     .encryptedDataDetails(new ArrayList<>())
                     .region("region")
                     .stackName("stackName")
                     .accountId("123456789012")
                     .parameters(new HashMap<String, String>() {
                       { put("parameterName", "parameterValue"); }
                     })
                     .timeoutInMs(100000)
                     .capabilities(Collections.singletonList("capability-1"));

    doReturn(AwsInternalConfig.builder().build()).when(awsNgConfigMapper).createAwsInternalConfig(any());
    doReturn(new ArrayList<>()).when(cloudformationBaseHelper).getCloudformationTags(anyString());
    doReturn(Collections.<String>emptySet())
        .when(cloudformationBaseHelper)
        .getCapabilities(any(), anyString(), anyString(), any(), anyString());

    createStackResult = new CreateStackResult();
    createStackResult.setStackId("stackId-123");
  }

  @Test
  @Owner(developers = VLICA)
  @Category(UnitTests.class)
  public void testCreateNewStackIsSuccessUsingTemplateBody()
      throws IOException, InterruptedException, TimeoutException {
    doReturn(createStackResult).when(awsCloudformationClient).createStack(anyString(), any(), any());

    Stack createdStack = new Stack();
    createdStack.setStackStatus("CREATE_COMPLETE");
    when(awsCloudformationClient.getAllStacks(anyString(), any(), any()))
        .thenReturn(new ArrayList<>(), Collections.singletonList(createdStack));

    parameters.templateBody("templateBody");

    CloudformationTaskNGResponse response =
        createStackTaskHandler.executeTaskInternal(parameters.build(), "delegateId", "task-Id", logCallback);

    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    verify(cloudformationBaseHelper, times(1)).getCloudformationTags(anyString());
    verify(cloudformationBaseHelper, times(1)).getCapabilities(any(), anyString(), anyString(), any(), any());
    verify(awsCloudformationClient, times(1)).createStack(anyString(), any(), any());
    verify(awsCloudformationClient, times(2)).getAllStacks(anyString(), any(), any());
    verify(cloudformationBaseHelper, times(1)).printStackEvents(any(), anyString(), anyLong(), any(), any());
    verify(cloudformationBaseHelper, times(1)).printStackResources(any(), anyString(), any(), any());

    ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
    verify(logCallback, atLeastOnce()).saveExecutionLog(logCaptor.capture());
    assertThat(logCaptor.getAllValues().contains("# Create Successful for stack"));
  }

  @Test
  @Owner(developers = VLICA)
  @Category(UnitTests.class)
  public void testCreateNewStackIsSuccessUsingTemplateURL() throws IOException, InterruptedException, TimeoutException {
    doReturn("normalizedTemplateUrl").when(awsCFHelperServiceDelegate).normalizeS3TemplatePath(anyString());
    doReturn(createStackResult).when(awsCloudformationClient).createStack(anyString(), any(), any());

    Stack createdStack = new Stack();
    createdStack.setStackStatus("CREATE_COMPLETE");
    when(awsCloudformationClient.getAllStacks(anyString(), any(), any()))
        .thenReturn(new ArrayList<>(), Collections.singletonList(createdStack));

    parameters.templateUrl("templateURL");
    CloudformationTaskNGResponse response =
        createStackTaskHandler.executeTaskInternal(parameters.build(), "delegateId", "task-Id", logCallback);

    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    verify(cloudformationBaseHelper, times(1)).getCloudformationTags(anyString());
    verify(cloudformationBaseHelper, times(1)).getCapabilities(any(), anyString(), anyString(), any(), any());
    verify(awsCloudformationClient, times(1)).createStack(anyString(), any(), any());
    verify(awsCloudformationClient, times(2)).getAllStacks(anyString(), any(), any());
    verify(cloudformationBaseHelper, times(1)).printStackEvents(any(), anyString(), anyLong(), any(), any());
    verify(cloudformationBaseHelper, times(1)).printStackResources(any(), anyString(), any(), any());
    verify(awsCFHelperServiceDelegate, times(1)).normalizeS3TemplatePath(anyString());

    ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
    verify(logCallback, atLeastOnce()).saveExecutionLog(logCaptor.capture());
    assertThat(logCaptor.getAllValues().contains("# Create Successful for stack"));
  }

  @Test
  @Owner(developers = VLICA)
  @Category(UnitTests.class)
  public void testUpdateStackIsSuccessUsingTemplateBody() throws IOException, InterruptedException, TimeoutException {
    doReturn(createStackResult).when(awsCloudformationClient).createStack(anyString(), any(), any());

    Stack createdStack = new Stack();
    createdStack.setStackStatus("CREATE_COMPLETE");
    createdStack.setStackName("stackName");
    when(awsCloudformationClient.getAllStacks(anyString(), any(), any()))
        .thenReturn(Collections.singletonList(createdStack), Collections.singletonList(createdStack));

    UpdateStackResult updatedStackResult = new UpdateStackResult();
    updatedStackResult.setStackId("stackId-123");
    when(awsCloudformationClient.updateStack(anyString(), any(), any())).thenReturn(updatedStackResult);

    parameters.templateBody("templateBody");
    CloudformationTaskNGResponse response =
        createStackTaskHandler.executeTaskInternal(parameters.build(), "delegateId", "task-Id", logCallback);

    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    verify(cloudformationBaseHelper, times(1)).getCloudformationTags(anyString());
    verify(cloudformationBaseHelper, times(1)).getCapabilities(any(), anyString(), anyString(), any(), any());
    verify(awsCloudformationClient, times(1)).updateStack(anyString(), any(), any());
    verify(awsCloudformationClient, times(2)).getAllStacks(anyString(), any(), any());
    verify(cloudformationBaseHelper, times(1)).printStackEvents(any(), anyString(), anyLong(), any(), any());
    verify(cloudformationBaseHelper, times(1)).printStackResources(any(), anyString(), any(), any());
    ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
    verify(logCallback, atLeastOnce()).saveExecutionLog(logCaptor.capture());
    assertThat(logCaptor.getAllValues().contains("# Update Successful for stack"));
  }

  @Test
  @Owner(developers = VLICA)
  @Category(UnitTests.class)
  public void testUpdateStackIsSuccessUsingTemplateUrl() throws IOException, InterruptedException, TimeoutException {
    doReturn(createStackResult).when(awsCloudformationClient).createStack(anyString(), any(), any());

    Stack createdStack = new Stack();
    createdStack.setStackStatus("CREATE_COMPLETE");
    createdStack.setStackName("stackName");
    when(awsCloudformationClient.getAllStacks(anyString(), any(), any()))
        .thenReturn(Collections.singletonList(createdStack), Collections.singletonList(createdStack));

    UpdateStackResult updatedStackResult = new UpdateStackResult();
    updatedStackResult.setStackId("stackId-123");
    when(awsCloudformationClient.updateStack(anyString(), any(), any())).thenReturn(updatedStackResult);

    parameters.templateUrl("templateURL");
    CloudformationTaskNGResponse response =
        createStackTaskHandler.executeTaskInternal(parameters.build(), "delegateId", "task-Id", logCallback);

    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    verify(cloudformationBaseHelper, times(1)).getCloudformationTags(anyString());
    verify(cloudformationBaseHelper, times(1)).getCapabilities(any(), anyString(), anyString(), any(), any());
    verify(awsCloudformationClient, times(1)).updateStack(anyString(), any(), any());
    verify(awsCloudformationClient, times(2)).getAllStacks(anyString(), any(), any());
    verify(cloudformationBaseHelper, times(1)).printStackEvents(any(), anyString(), anyLong(), any(), any());
    verify(cloudformationBaseHelper, times(1)).printStackResources(any(), anyString(), any(), any());
    verify(awsCFHelperServiceDelegate, times(1)).normalizeS3TemplatePath(anyString());
    ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
    verify(logCallback, atLeastOnce()).saveExecutionLog(logCaptor.capture());
    assertThat(logCaptor.getAllValues().contains("# Update Successful for stack"));
  }

  @Test
  @Owner(developers = VLICA)
  @Category(UnitTests.class)
  public void testCreateNewStackIsFailureCauseOfException() throws IOException, InterruptedException, TimeoutException {
    doThrow(new AmazonServiceException("service exception"))
        .when(awsCloudformationClient)
        .createStack(anyString(), any(), any());

    parameters.templateBody("templateBody");

    CloudformationTaskNGResponse response =
        createStackTaskHandler.executeTaskInternal(parameters.build(), "delegateId", "task-Id", logCallback);

    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    verify(cloudformationBaseHelper, times(1)).getCloudformationTags(anyString());
    verify(awsCloudformationClient, times(1)).getAllStacks(anyString(), any(), any());
    verify(cloudformationBaseHelper, times(1)).getCapabilities(any(), anyString(), anyString(), any(), any());
    verify(awsCloudformationClient, times(1)).createStack(anyString(), any(), any());
    verify(cloudformationBaseHelper, times(0)).printStackEvents(any(), anyString(), anyLong(), any(), any());
    verify(cloudformationBaseHelper, times(0)).printStackResources(any(), anyString(), any(), any());

    ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<LogLevel> logLevelCaptor = ArgumentCaptor.forClass(LogLevel.class);

    verify(logCallback, atLeastOnce()).saveExecutionLog(msgCaptor.capture(), logLevelCaptor.capture());
    assertThat(logLevelCaptor.getValue()).isEqualTo(ERROR);
  }

  @Test
  @Owner(developers = VLICA)
  @Category(UnitTests.class)
  public void testUpdateNewStackIsFailureCauseOfException() throws IOException, InterruptedException, TimeoutException {
    doReturn(createStackResult).when(awsCloudformationClient).createStack(anyString(), any(), any());

    Stack createdStack = new Stack();
    createdStack.setStackStatus("CREATE_COMPLETE");
    createdStack.setStackName("stackName");
    when(awsCloudformationClient.getAllStacks(anyString(), any(), any()))
        .thenReturn(Collections.singletonList(createdStack), Collections.singletonList(createdStack));

    ExistingStackInfo existingStackInfo = ExistingStackInfo.builder().stackExisted(true).build();
    when(cloudformationBaseHelper.getExistingStackInfo(any(), anyString(), any())).thenReturn(existingStackInfo);

    doThrow(new AmazonServiceException(" AWS service exception"))
        .when(awsCloudformationClient)
        .updateStack(anyString(), any(), any());

    parameters.templateUrl("templateURL");
    CloudformationTaskNGResponse response =
        createStackTaskHandler.executeTaskInternal(parameters.build(), "delegateId", "task-Id", logCallback);

    ArgumentCaptor<LogLevel> logLevelCaptor = ArgumentCaptor.forClass(LogLevel.class);

    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(FAILURE);
    verify(cloudformationBaseHelper, times(1)).getCloudformationTags(anyString());
    verify(cloudformationBaseHelper, times(1)).getCapabilities(any(), anyString(), anyString(), any(), any());
    verify(awsCloudformationClient, times(1)).updateStack(anyString(), any(), any());
    verify(awsCloudformationClient, times(1)).getAllStacks(anyString(), any(), any());

    verify(logCallback, atLeastOnce()).saveExecutionLog(anyString(), logLevelCaptor.capture());
    assertThat(logLevelCaptor.getValue()).isEqualTo(ERROR);
  }

  @Test
  @Owner(developers = VLICA)
  @Category(UnitTests.class)
  public void testCreateNewStackIsFailureCauseCreatedStackHasWrongStatus()
      throws IOException, InterruptedException, TimeoutException {
    doReturn(createStackResult).when(awsCloudformationClient).createStack(anyString(), any(), any());

    Stack stackFailedStatus = new Stack();
    stackFailedStatus.setStackStatus("CREATE_FAILED");
    when(awsCloudformationClient.getAllStacks(anyString(), any(), any()))
        .thenReturn(new ArrayList<>(), Collections.singletonList(stackFailedStatus));
    parameters.templateBody("templateBody");
    CloudformationTaskNGResponse response =
        createStackTaskHandler.executeTaskInternal(parameters.build(), "delegateId", "task-Id", logCallback);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(FAILURE);

    Stack stackRollbackFailedStatus = new Stack();
    stackRollbackFailedStatus.setStackStatus("ROLLBACK_FAILED");
    when(awsCloudformationClient.getAllStacks(anyString(), any(), any()))
        .thenReturn(new ArrayList<>(), Collections.singletonList(stackRollbackFailedStatus));
    parameters.templateBody("templateBody");
    CloudformationTaskNGResponse response1 =
        createStackTaskHandler.executeTaskInternal(parameters.build(), "delegateId", "task-Id", logCallback);
    assertThat(response1).isNotNull();
    assertThat(response1.getCommandExecutionStatus()).isEqualTo(FAILURE);

    Stack stackRollbackCompleteStatus = new Stack();
    stackRollbackCompleteStatus.setStackStatus("ROLLBACK_COMPLETE");
    when(awsCloudformationClient.getAllStacks(anyString(), any(), any()))
        .thenReturn(new ArrayList<>(), Collections.singletonList(stackRollbackCompleteStatus));
    parameters.templateBody("templateBody");
    CloudformationTaskNGResponse response2 =
        createStackTaskHandler.executeTaskInternal(parameters.build(), "delegateId", "task-Id", logCallback);
    assertThat(response2).isNotNull();
    assertThat(response2.getCommandExecutionStatus()).isEqualTo(FAILURE);

    Stack stackUnknownStatus = new Stack();
    stackUnknownStatus.setStackStatus("UNKNOWN");
    when(awsCloudformationClient.getAllStacks(anyString(), any(), any()))
        .thenReturn(new ArrayList<>(), Collections.singletonList(stackUnknownStatus));
    parameters.templateBody("templateBody");
    CloudformationTaskNGResponse response3 =
        createStackTaskHandler.executeTaskInternal(parameters.build(), "delegateId", "task-Id", logCallback);
    assertThat(response3).isNotNull();
    assertThat(response3.getCommandExecutionStatus()).isEqualTo(FAILURE);

    Stack stackRollbackInProgressStatus = new Stack();
    stackRollbackInProgressStatus.setStackStatus("ROLLBACK_IN_PROGRESS");
    when(awsCloudformationClient.getAllStacks(anyString(), any(), any()))
        .thenReturn(new ArrayList<>(), Collections.singletonList(stackRollbackInProgressStatus));
    parameters.templateBody("templateBody");
    CloudformationTaskNGResponse response4 =
        createStackTaskHandler.executeTaskInternal(parameters.build(), "delegateId", "task-Id", logCallback);
    assertThat(response4).isNotNull();
    assertThat(response4.getCommandExecutionStatus()).isEqualTo(FAILURE);
  }

  @Test
  @Owner(developers = VLICA)
  @Category(UnitTests.class)
  public void testUpdateNewStackIsFailureCauseUpdatedStackHasWrongStatus()
      throws IOException, InterruptedException, TimeoutException {
    ExistingStackInfo existingStackInfo = ExistingStackInfo.builder().stackExisted(true).build();
    when(cloudformationBaseHelper.getExistingStackInfo(any(), anyString(), any())).thenReturn(existingStackInfo);
    UpdateStackResult updatedStackResult = new UpdateStackResult();
    updatedStackResult.setStackId("stackId-123");
    when(awsCloudformationClient.updateStack(anyString(), any(), any())).thenReturn(updatedStackResult);

    Stack stackUpdateRollbackFailedStatus = new Stack();
    stackUpdateRollbackFailedStatus.setStackStatus("UPDATE_ROLLBACK_FAILED");
    stackUpdateRollbackFailedStatus.setStackName("stackName");
    when(awsCloudformationClient.getAllStacks(anyString(), any(), any()))
        .thenReturn(Collections.singletonList(stackUpdateRollbackFailedStatus),
            Collections.singletonList(stackUpdateRollbackFailedStatus));
    parameters.templateBody("templatupdateStackeBody");
    CloudformationTaskNGResponse response =
        createStackTaskHandler.executeTaskInternal(parameters.build(), "delegateId", "task-Id", logCallback);
    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(FAILURE);

    Stack stackUpdateCompleteCleanupInProgress = new Stack();
    stackUpdateCompleteCleanupInProgress.setStackStatus("UPDATE_COMPLETE_CLEANUP_IN_PROGRESS");
    stackUpdateCompleteCleanupInProgress.setStackName("stackName");
    when(awsCloudformationClient.getAllStacks(anyString(), any(), any()))
        .thenReturn(Collections.singletonList(stackUpdateCompleteCleanupInProgress),
            Collections.singletonList(stackUpdateCompleteCleanupInProgress));
    parameters.templateBody("templateBody");
    CloudformationTaskNGResponse response1 =
        createStackTaskHandler.executeTaskInternal(parameters.build(), "delegateId", "task-Id", logCallback);
    assertThat(response1).isNotNull();
    assertThat(response1.getCommandExecutionStatus()).isEqualTo(FAILURE);

    Stack stackRollbackCompleteCleanupInProgress = new Stack();
    stackRollbackCompleteCleanupInProgress.setStackStatus("UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS");
    stackRollbackCompleteCleanupInProgress.setStackName("stackName");
    when(awsCloudformationClient.getAllStacks(anyString(), any(), any()))
        .thenReturn(Collections.singletonList(stackRollbackCompleteCleanupInProgress),
            Collections.singletonList(stackRollbackCompleteCleanupInProgress));
    parameters.templateBody("templateBody");
    CloudformationTaskNGResponse response2 =
        createStackTaskHandler.executeTaskInternal(parameters.build(), "delegateId", "task-Id", logCallback);
    assertThat(response2).isNotNull();
    assertThat(response2.getCommandExecutionStatus()).isEqualTo(FAILURE);

    Stack stackRollbackInProgress = new Stack();
    stackRollbackInProgress.setStackStatus("UPDATE_ROLLBACK_IN_PROGRESS");
    stackRollbackInProgress.setStackName("stackName");
    when(awsCloudformationClient.getAllStacks(anyString(), any(), any()))
        .thenReturn(
            Collections.singletonList(stackRollbackInProgress), Collections.singletonList(stackRollbackInProgress));
    parameters.templateBody("templateBody");
    CloudformationTaskNGResponse response3 =
        createStackTaskHandler.executeTaskInternal(parameters.build(), "delegateId", "task-Id", logCallback);
    assertThat(response3).isNotNull();
    assertThat(response3.getCommandExecutionStatus()).isEqualTo(FAILURE);

    Stack stackUnknownStatus = new Stack();
    stackUnknownStatus.setStackStatus("UNKNOWN");
    stackUnknownStatus.setStackName("stackName");
    when(awsCloudformationClient.getAllStacks(anyString(), any(), any()))
        .thenReturn(Collections.singletonList(stackUnknownStatus), Collections.singletonList(stackUnknownStatus));
    parameters.templateBody("templateBody");
    CloudformationTaskNGResponse response4 =
        createStackTaskHandler.executeTaskInternal(parameters.build(), "delegateId", "task-Id", logCallback);
    assertThat(response4).isNotNull();
    assertThat(response4.getCommandExecutionStatus()).isEqualTo(FAILURE);
  }

  @Test
  @Owner(developers = VLICA)
  @Category(UnitTests.class)
  public void testDeleteExistingStackAndCreateNewOne() throws IOException, InterruptedException, TimeoutException {
    Stack createdStack = new Stack();
    createdStack.setStackStatus("CREATE_COMPLETE");

    Stack existingStackWithRollbackStatus = new Stack();
    existingStackWithRollbackStatus.setStackStatus("ROLLBACK_COMPLETE");
    existingStackWithRollbackStatus.setStackName("stackName");
    when(awsCloudformationClient.getAllStacks(anyString(), any(), any()))
        .thenReturn(
            Collections.singletonList(existingStackWithRollbackStatus), Collections.singletonList(createdStack));

    doReturn(createStackResult).when(awsCloudformationClient).createStack(anyString(), any(), any());

    parameters.templateBody("templateBody");

    CloudformationTaskNGResponse response =
        createStackTaskHandler.executeTaskInternal(parameters.build(), "delegateId", "task-Id", logCallback);

    assertThat(response).isNotNull();
    assertThat(response.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    verify(cloudformationBaseHelper, times(1)).getCloudformationTags(anyString());
    verify(cloudformationBaseHelper, times(1)).getCapabilities(any(), anyString(), anyString(), any(), any());
    verify(awsCloudformationClient, times(1)).createStack(anyString(), any(), any());
    verify(awsCloudformationClient, times(2)).getAllStacks(anyString(), any(), any());
    verify(cloudformationBaseHelper, times(1)).printStackEvents(any(), anyString(), anyLong(), any(), any());
    verify(cloudformationBaseHelper, times(1)).printStackResources(any(), anyString(), any(), any());

    ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
    verify(logCallback, atLeastOnce()).saveExecutionLog(logCaptor.capture());
    assertThat(logCaptor.getAllValues().contains("# Stack stackName deleted successfully now creating a new stack"));
    assertThat(logCaptor.getAllValues().contains("# Create Successful for stack"));
  }
}
