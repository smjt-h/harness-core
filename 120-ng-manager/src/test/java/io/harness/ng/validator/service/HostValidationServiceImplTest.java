/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.validator.service;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.secrets.SSHConfigValidationTaskResponse;
import io.harness.delegate.utils.TaskSetupAbstractionHelper;
import io.harness.ng.core.api.NGSecretServiceV2;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ng.core.models.Secret;
import io.harness.ng.core.models.SecretSpec;
import io.harness.ng.validator.dto.HostValidationDTO;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.services.SshKeySpecDTOHelper;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static io.harness.rule.OwnerRule.VLAD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@OwnedBy(HarnessTeam.CDC)
public class HostValidationServiceImplTest extends CategoryTest {
  @Mock private NGSecretServiceV2 ngSecretServiceV2;
  @Mock private SshKeySpecDTOHelper sshKeySpecDTOHelper;
  @Mock private TaskSetupAbstractionHelper taskSetupAbstractionHelper;
  @Mock private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @InjectMocks
  HostValidationServiceImpl hostValidationService;

  private static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
  private static final String ORG_IDENTIFIER = "orgIdentifier";
  private static final String PROJECT_IDENTIFIER = "projectIdentifier";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldValidateSshHost() {
    String host = "host1";
    String secretIdentifier = "secret1";
    Secret secret = Mockito.mock(Secret.class);
    when(ngSecretServiceV2.get(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, secretIdentifier))
            .thenReturn(Optional.of(secret));
    when(secret.getType()).thenReturn(SecretType.SSHKey);
    SecretSpec secretKeySpec = mock(SecretSpec.class);
    when(secret.getSecretSpec()).thenReturn(secretKeySpec);
    SSHKeySpecDTO secretSpecDTO = mock(SSHKeySpecDTO.class);
    when(secretKeySpec.toDTO()).thenReturn(secretSpecDTO);
    EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder().build();
    List<EncryptedDataDetail> encryptionDetails = Arrays.asList(encryptedDataDetail);
    when(sshKeySpecDTOHelper.getSSHKeyEncryptionDetails(any(), any())).thenReturn(encryptionDetails);
    when(taskSetupAbstractionHelper.getOwner(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER)).thenReturn("owner");
    SSHConfigValidationTaskResponse response = SSHConfigValidationTaskResponse.builder().connectionSuccessful(true).build();
    when(delegateGrpcClientWrapper.executeSyncTask(any())).thenReturn(response);

    HostValidationDTO result = hostValidationService.validateSSHHost(host, ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, secretIdentifier);
    assertThat(result.getHost()).isEqualTo(host);
    assertThat(result.getStatus()).isEqualTo(HostValidationDTO.HostValidationStatus.SUCCESS);
    assertThat(result.getError()).isEqualTo(ErrorDetail.builder().build());
  }
}
