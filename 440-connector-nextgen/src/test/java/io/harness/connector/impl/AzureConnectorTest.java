/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.impl;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.beans.connector.ConnectorType.AZURE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.AzureEnvironmentType;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorsTestBase;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.embedded.azureconnector.AzureConfig;
import io.harness.connector.entities.embedded.azureconnector.AzureManualCredential;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureCredentialDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureCredentialType;
import io.harness.delegate.beans.connector.azureconnector.AzureManualDetailsDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureSecretType;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.repositories.ConnectorRepository;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
@Slf4j
public class AzureConnectorTest extends ConnectorsTestBase {
  @Mock SecretRefInputValidationHelper secretRefInputValidationHelper;
  @Mock ConnectorRepository connectorRepository;
  @Inject @InjectMocks DefaultConnectorServiceImpl connectorService;

  String identifier = "identifier";
  String name = "name";
  String description = "description";

  String accountIdentifier = "accountIdentifier";

  String clientId = "11111111-aaaa-bbbb-2222-123456789123";
  String tenantId = "22222222-cccc-dddd-3333-234567891234";
  String secretKeyRef = "azureKey";
  SecretRefData secretKey;
  @Rule public ExpectedException expectedEx = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    secretKey = SecretRefData.builder().identifier("secretRef").scope(Scope.ACCOUNT).build();
    doNothing().when(secretRefInputValidationHelper).validateTheSecretInput(any(), any());
  }

  @Test
  @Owner(developers = OwnerRule.BUHA)
  @Category(UnitTests.class)
  public void testCreateAzureConnectorManualConfig() {
    ConnectorDTO connectorDTO = createConnectorDTO();
    ConnectorResponseDTO connectorDTOOutput = createConnector(connectorDTO);
    ensureAzureConnectorFieldsAreCorrect(connectorDTOOutput);
    AzureConnectorDTO azureConnectorDTO = (AzureConnectorDTO) connectorDTOOutput.getConnector().getConnectorConfig();
    assertThat(azureConnectorDTO).isNotNull();
    assertThat(azureConnectorDTO.getCredential()).isNotNull();
    assertThat(azureConnectorDTO.getCredential().getAzureCredentialType())
        .isEqualByComparingTo(AzureCredentialType.MANUAL_CREDENTIALS);
    assertThat(azureConnectorDTO.getCredential().getConfig()).isNotNull();
    assertThat(((AzureManualDetailsDTO) azureConnectorDTO.getCredential().getConfig()).getClientId())
        .isEqualTo(clientId);
    assertThat(((AzureManualDetailsDTO) azureConnectorDTO.getCredential().getConfig()).getTenantId())
        .isEqualTo(tenantId);
    assertThat(((AzureManualDetailsDTO) azureConnectorDTO.getCredential().getConfig()).getSecretRef())
        .isEqualTo(secretKey);
  }

  private ConnectorResponseDTO createConnector(ConnectorDTO connectorRequest) {
    return connectorService.create(connectorRequest, accountIdentifier);
  }

  private ConnectorDTO createConnectorDTO() {
    AzureCredentialDTO azureCredentialDTO = AzureCredentialDTO.builder()
                                                .azureCredentialType(AzureCredentialType.MANUAL_CREDENTIALS)
                                                .config(AzureManualDetailsDTO.builder()
                                                            .clientId(clientId)
                                                            .tenantId(tenantId)
                                                            .secretType(AzureSecretType.SECRET_KEY)
                                                            .secretRef(secretKey)
                                                            .build())
                                                .build();

    AzureConnectorDTO azureConnectorDTO =
        AzureConnectorDTO.builder().credential(azureCredentialDTO).delegateSelectors(null).build();

    return ConnectorDTO.builder()
        .connectorInfo(ConnectorInfoDTO.builder()
                           .name(name)
                           .identifier(identifier)
                           .description(description)
                           .connectorType(AZURE)
                           .connectorConfig(azureConnectorDTO)
                           .build())
        .build();
  }

  private Connector createConnector() {
    return AzureConfig.builder()
        .azureEnvironmentType(AzureEnvironmentType.AZURE)
        .credentialType(AzureCredentialType.MANUAL_CREDENTIALS)
        .credential(AzureManualCredential.builder()
                        .clientId(clientId)
                        .tenantId(tenantId)
                        .azureSecretType(AzureSecretType.SECRET_KEY)
                        .secretKeyRef(secretKeyRef)
                        .build())
        .build();
  }

  private void ensureAzureConnectorFieldsAreCorrect(ConnectorResponseDTO connectorResponse) {
    ConnectorInfoDTO connector = connectorResponse.getConnector();
    assertThat(connector).isNotNull();
    assertThat(connector.getName()).isEqualTo(name);
    assertThat(connector.getIdentifier()).isEqualTo(identifier);
    assertThat(connector.getConnectorType()).isEqualTo(AZURE);
  }
}
