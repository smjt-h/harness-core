/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.azure;

import static io.harness.exception.WingsException.USER;
import static io.harness.rule.OwnerRule.BUHA;
import static io.harness.rule.OwnerRule.MLUKIC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.AzureEnvironmentType;
import io.harness.azure.client.AzureManagementClient;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.azureconnector.AzureAuthDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureClientKeyCertDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureClientSecretKeyDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureCredentialDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureCredentialType;
import io.harness.delegate.beans.connector.azureconnector.AzureInheritFromDelegateDetailsDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureMSIAuthDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureMSIAuthSADTO;
import io.harness.delegate.beans.connector.azureconnector.AzureMSIAuthUADTO;
import io.harness.delegate.beans.connector.azureconnector.AzureManagedIdentityType;
import io.harness.delegate.beans.connector.azureconnector.AzureManualDetailsDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureSecretType;
import io.harness.delegate.beans.connector.azureconnector.AzureUserAssignedMSIAuthDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.exception.InvalidCredentialsException;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.security.encryption.SecretDecryptionService;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.CDP)
public class AzureNgHelperTest extends CategoryTest {
  @InjectMocks AzureNgHelper azureNgHelper;
  @Mock private SecretDecryptionService secretDecryptionService;
  @Mock private NGErrorHelper ngErrorHelper;
  @Mock private AzureManagementClient azureManagementClient;

  String clientId = "clientId";
  String tenantId = "tenantId";
  String secretIdentifier = "secretKey";
  String pass = "pass";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testValidateSuccessConnectionWithSecretKey() {
    AzureConnectorDTO azureConnectorDTO = getAzureConnectorDTOWithSecretType(AzureSecretType.SECRET_KEY);

    ConnectorValidationResult result = azureNgHelper.getConnectorValidationResult(null, azureConnectorDTO);

    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.SUCCESS);
    verify(secretDecryptionService).decrypt(any(), any());
    verify(azureManagementClient).validateAzureConnection(clientId, tenantId, pass, AzureEnvironmentType.AZURE);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testValidateSuccessConnectionWithCert() {
    AzureConnectorDTO azureConnectorDTO = getAzureConnectorDTOWithSecretType(AzureSecretType.KEY_CERT);

    ConnectorValidationResult result = azureNgHelper.getConnectorValidationResult(null, azureConnectorDTO);

    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.SUCCESS);
    verify(secretDecryptionService).decrypt(any(), any());
    verify(azureManagementClient)
        .validateAzureConnectionWithCert(clientId, tenantId, pass.getBytes(), AzureEnvironmentType.AZURE);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testValidationFailedWithSecretKey() {
    AzureConnectorDTO azureConnectorDTO = getAzureConnectorDTOWithSecretType(AzureSecretType.SECRET_KEY);
    doThrow(new InvalidCredentialsException("Invalid Azure credentials.", USER))
        .when(azureManagementClient)
        .validateAzureConnection(anyString(), anyString(), anyString(), any());
    ConnectorValidationResult result = azureNgHelper.getConnectorValidationResult(null, azureConnectorDTO);

    verify(secretDecryptionService).decrypt(any(), any());
    verify(azureManagementClient).validateAzureConnection(clientId, tenantId, pass, AzureEnvironmentType.AZURE);
    verify(ngErrorHelper).createErrorDetail("Invalid Azure credentials.");
    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.FAILURE);
  }

  private AzureConnectorDTO getAzureConnectorDTOWithSecretType(AzureSecretType type) {
    SecretRefData secretRef = SecretRefData.builder()
                                  .identifier(secretIdentifier)
                                  .scope(Scope.ACCOUNT)
                                  .decryptedValue(pass.toCharArray())
                                  .build();
    return AzureConnectorDTO.builder()
        .azureEnvironmentType(AzureEnvironmentType.AZURE)
        .credential(
            AzureCredentialDTO.builder()
                .azureCredentialType(AzureCredentialType.MANUAL_CREDENTIALS)
                .config(AzureManualDetailsDTO.builder()
                            .clientId(clientId)
                            .tenantId(tenantId)
                            .authDTO(AzureAuthDTO.builder()
                                         .azureSecretType(type)
                                         .credentials(type == AzureSecretType.KEY_CERT
                                                 ? AzureClientKeyCertDTO.builder().clientCertRef(secretRef).build()
                                                 : AzureClientSecretKeyDTO.builder().secretKey(secretRef).build())
                                         .build())
                            .build())

                .build())
        .build();
  }

  private AzureConnectorDTO getAzureConnectorDTOWithMSI(String clientId) {
    AzureMSIAuthDTO azureMSIAuthDTO;

    if (EmptyPredicate.isNotEmpty(clientId)) {
      AzureUserAssignedMSIAuthDTO azureUserAssignedMSIAuthDTO =
          AzureUserAssignedMSIAuthDTO.builder().clientId(clientId).build();
      azureMSIAuthDTO = AzureMSIAuthUADTO.builder()
                            .azureManagedIdentityType(AzureManagedIdentityType.USER_ASSIGNED_MANAGED_IDENTITY)
                            .credentials(azureUserAssignedMSIAuthDTO)
                            .build();
    } else {
      azureMSIAuthDTO = AzureMSIAuthSADTO.builder()
                            .azureManagedIdentityType(AzureManagedIdentityType.SYSTEM_ASSIGNED_MANAGED_IDENTITY)
                            .build();
    }

    return AzureConnectorDTO.builder()
        .azureEnvironmentType(AzureEnvironmentType.AZURE)
        .credential(AzureCredentialDTO.builder()
                        .azureCredentialType(AzureCredentialType.INHERIT_FROM_DELEGATE)
                        .config(AzureInheritFromDelegateDetailsDTO.builder().authDTO(azureMSIAuthDTO).build())

                        .build())
        .build();
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testGetConnectorValidationResultWithInheritFromDelegateUserAssignedMSIConnection() {
    AzureConnectorDTO azureConnectorDTO = getAzureConnectorDTOWithMSI("testClientId");

    ConnectorValidationResult result = azureNgHelper.getConnectorValidationResult(null, azureConnectorDTO);

    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.SUCCESS);

    verify(azureManagementClient).validateAzureConnection(true, "testClientId", AzureEnvironmentType.AZURE);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testGetConnectorValidationResultWithInheritFromDelegateSystemAssignedMSIConnection() {
    AzureConnectorDTO azureConnectorDTO = getAzureConnectorDTOWithMSI(null);

    ConnectorValidationResult result = azureNgHelper.getConnectorValidationResult(null, azureConnectorDTO);

    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.SUCCESS);

    verify(azureManagementClient).validateAzureConnection(false, null, AzureEnvironmentType.AZURE);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testFail_GetConnectorValidationResultWithInheritFromDelegateUserAssignedMSIConnection() {
    AzureConnectorDTO azureConnectorDTO = getAzureConnectorDTOWithMSI("badTestClientId");
    doThrow(new InvalidRequestException("Failed to connect to Azure cluster.", USER))
        .when(azureManagementClient)
        .validateAzureConnection(true, "badTestClientId", AzureEnvironmentType.AZURE);
    ConnectorValidationResult result = azureNgHelper.getConnectorValidationResult(null, azureConnectorDTO);

    verify(azureManagementClient).validateAzureConnection(true, "badTestClientId", AzureEnvironmentType.AZURE);
    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.FAILURE);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testFail_GetConnectorValidationResultWithInheritFromDelegateSystemAssignedMSIConnection() {
    AzureConnectorDTO azureConnectorDTO = getAzureConnectorDTOWithMSI(null);
    doThrow(new InvalidRequestException("Failed to connect to Azure cluster.", USER))
        .when(azureManagementClient)
        .validateAzureConnection(false, null, AzureEnvironmentType.AZURE);
    ConnectorValidationResult result = azureNgHelper.getConnectorValidationResult(null, azureConnectorDTO);

    verify(azureManagementClient).validateAzureConnection(false, null, AzureEnvironmentType.AZURE);
    assertThat(result.getStatus()).isEqualTo(ConnectivityStatus.FAILURE);
  }
}
