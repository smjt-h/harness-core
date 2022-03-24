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
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.service.intfc.security.EncryptionService;

import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class CloudFormationBaseHelperImplTest extends CategoryTest {
  @Mock private AWSCloudformationClient awsCloudformationClient;
  @Mock protected EncryptionService encryptionService;
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
    String stackNameSuffix = "Stack Name 00";
    Date timeStamp = new Date();
    Stack testStack = new Stack().withStackStatus("CREATE_COMPLETE").withStackName(stackName + stackNameSuffix);
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
}
