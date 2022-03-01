/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.azuremapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.azureconnector.AzureConfig;
import io.harness.connector.entities.embedded.azureconnector.AzureManualCredential;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorCredentialDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureCredentialType;
import io.harness.delegate.beans.connector.azureconnector.AzureManualDetailsDTO;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.CDP)
@Singleton
public class AzureDTOToEntity implements ConnectorDTOToEntityMapper<AzureConnectorDTO, AzureConfig> {
  @Override
  public AzureConfig toConnectorEntity(AzureConnectorDTO connectorDTO) {
    final AzureConnectorCredentialDTO credential = connectorDTO.getCredential();
    final AzureCredentialType credentialType = credential.getAzureCredentialType();
    final AzureConfig.AzureConfigBuilder azureConfigBuilder;
    switch (credentialType) {
      case INHERIT_FROM_DELEGATE:
        azureConfigBuilder = buildInheritFromDelegate();
        break;
      case MANUAL_CREDENTIALS:
        azureConfigBuilder = buildManualCredential(credential);
        break;
      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }
    return azureConfigBuilder.azureEnvironmentType(connectorDTO.getAzureEnvironmentType()).build();
  }

  private AzureConfig.AzureConfigBuilder buildInheritFromDelegate() {
    return AzureConfig.builder().credentialType(AzureCredentialType.INHERIT_FROM_DELEGATE).credential(null);
  }

  private AzureConfig.AzureConfigBuilder buildManualCredential(AzureConnectorCredentialDTO connector) {
    final AzureManualDetailsDTO config = (AzureManualDetailsDTO) connector.getConfig();
    final String secretKeyRef = SecretRefHelper.getSecretConfigString(config.getSecretRef());
    AzureManualCredential azureManualCredential = AzureManualCredential.builder()
                                                      .tenantId(config.getTenantId())
                                                      .clientId(config.getClientId())
                                                      .secretKeyRef(secretKeyRef)
                                                      .azureSecretType(config.getSecretType())
                                                      .build();
    return AzureConfig.builder()
        .credentialType(AzureCredentialType.MANUAL_CREDENTIALS)
        .credential(azureManualCredential);
  }
}
