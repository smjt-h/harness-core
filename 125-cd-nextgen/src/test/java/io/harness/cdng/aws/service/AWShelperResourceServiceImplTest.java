/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.aws.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.NGONZALEZ;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.cdng.common.resources.AwsResourceServiceHelper;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsIAMRolesResponse;
import io.harness.exception.AwsIAMRolesException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ng.core.BaseNGAccess;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class AWShelperResourceServiceImplTest extends CategoryTest {
  @Mock private AwsResourceServiceHelper serviceHelper;
  @InjectMocks private AwsHelperResourceServiceImpl service;
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }
  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testGetRolesArn() {
    AwsConnectorDTO awsMockConnectorDTO = mock(AwsConnectorDTO.class);
    doReturn(awsMockConnectorDTO).when(serviceHelper).getAwsConnector(any());
    BaseNGAccess mockAccess = BaseNGAccess.builder().build();
    doReturn(mockAccess).when(serviceHelper).getBaseNGAccess(any(), any(), any());
    List<EncryptedDataDetail> encryptedDataDetails = mock(List.class);
    doReturn(encryptedDataDetails).when(serviceHelper).getAwsEncryptionDetails(any(), any());
    Map<String, String> rolesMap = new HashMap<>();
    rolesMap.put("role1", "arn:aws:iam::123456789012:role/role1");
    rolesMap.put("role2", "arn:aws:iam::123456789012:role/role2");
    AwsIAMRolesResponse mockAwsIAMRolesResponse =
        AwsIAMRolesResponse.builder().roles(rolesMap).commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
    doReturn(mockAwsIAMRolesResponse).when(serviceHelper).getResponseData(any(), any(), anyString());
    IdentifierRef mockIdentifierRef = IdentifierRef.builder().build();
    service.getRolesARNs(mockIdentifierRef, "foo", "bar");
    assertThat(mockAwsIAMRolesResponse.getRoles().size()).isEqualTo(2);
  }

  @Test(expected = AwsIAMRolesException.class)
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testGetRolesArnWithErrorNotifiedResponse() {
    AwsConnectorDTO awsMockConnectorDTO = mock(AwsConnectorDTO.class);
    doReturn(awsMockConnectorDTO).when(serviceHelper).getAwsConnector(any());
    BaseNGAccess mockAccess = BaseNGAccess.builder().build();
    doReturn(mockAccess).when(serviceHelper).getBaseNGAccess(any(), any(), any());
    Map<String, String> rolesMap = new HashMap<>();
    rolesMap.put("role1", "arn:aws:iam::123456789012:role/role1");
    rolesMap.put("role2", "arn:aws:iam::123456789012:role/role2");
    ErrorNotifyResponseData mockAwsIAMRolesResponse = ErrorNotifyResponseData.builder().build();
    doReturn(mockAwsIAMRolesResponse).when(serviceHelper).getResponseData(any(), any(), anyString());
    IdentifierRef mockIdentifierRef = IdentifierRef.builder().build();
    service.getRolesARNs(mockIdentifierRef, "foo", "bar");
  }
}
