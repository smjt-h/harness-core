/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.cloudformation;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.NGONZALEZ;
import static io.harness.rule.OwnerRule.TMACARI;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.aws.AWSCloudformationClient;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.task.aws.AwsNgConfigMapper;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Stack;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class CloudFormationBaseHelperImplTest extends CategoryTest {
  @Mock private AWSCloudformationClient awsHelperService;
  @Mock private SecretDecryptionService encryptionService;
  @Mock private AwsNgConfigMapper awsNgConfigMapper;
  @Mock private LogCallback logCallback;
  @InjectMocks private CloudformationBaseHelperImpl cloudFormationBaseHelper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetIfStackExists() {
    String customStackName = "CUSTOM_STACK_NAME";
    String stackId = "STACK_ID";
    doReturn(singletonList(new Stack().withStackId(stackId).withStackName(customStackName)))
        .when(awsHelperService)
        .getAllStacks(anyString(), any(), any());
    Optional<Stack> stack = cloudFormationBaseHelper.getIfStackExists(
        customStackName, "foo", AwsInternalConfig.builder().build(), "us-east-1");
    assertThat(stack.isPresent()).isTrue();
    assertThat(stackId).isEqualTo(stack.get().getStackId());
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testAwsInternalConfigWithoutCredentials() {
    AwsConnectorDTO emptyDTO = AwsConnectorDTO.builder().build();
    AwsInternalConfig emptyConfig = AwsInternalConfig.builder().build();
    doReturn(emptyConfig).when(awsNgConfigMapper).createAwsInternalConfig(emptyDTO);
    cloudFormationBaseHelper.getAwsInternalConfig(emptyDTO, "us-east-1", any());
    verify(encryptionService, times(0)).decrypt(any(), any());
  }
  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testAwsInternalConfigWithCredentials() {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    AwsConnectorDTO emptyDTO =
        AwsConnectorDTO.builder()
            .credential(AwsCredentialDTO.builder().config(AwsManualConfigSpecDTO.builder().build()).build())
            .build();
    AwsInternalConfig emptyConfig = AwsInternalConfig.builder().build();
    doReturn(emptyConfig).when(awsNgConfigMapper).createAwsInternalConfig(emptyDTO);
    doReturn(emptyDTO).when(encryptionService).decrypt(any(), any());
    cloudFormationBaseHelper.getAwsInternalConfig(emptyDTO, "us-east-1", encryptedDataDetails);
    verify(encryptionService, times(1)).decrypt(any(), any());
  }
  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testDeleteStack() {
    AwsInternalConfig emptyConfig = AwsInternalConfig.builder().build();
    DeleteStackRequest deleteStackRequest = new DeleteStackRequest();
    deleteStackRequest.withStackName("stackName");
    deleteStackRequest.withRoleARN("roleARN");
    cloudFormationBaseHelper.deleteStack("us-east-1", emptyConfig, "stackName", "roleARN", 10);
    verify(awsHelperService).deleteStack("us-east-1", deleteStackRequest, emptyConfig);
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void waitForStackToBeDeleted() {
    AwsInternalConfig emptyConfig = AwsInternalConfig.builder().build();
    DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest();
    describeStacksRequest.withStackName("stackName");
    cloudFormationBaseHelper.waitForStackToBeDeleted("us-east-1", emptyConfig, "stackName", logCallback);
    verify(awsHelperService).stackDeletionCompleted(describeStacksRequest, emptyConfig, "us-east-1", logCallback);
  }
}
