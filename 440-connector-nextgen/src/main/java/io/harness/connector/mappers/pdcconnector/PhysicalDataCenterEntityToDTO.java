package io.harness.connector.mappers.pdcconnector;

import io.harness.connector.entities.embedded.pdcconnector.PhysicalDataCenterConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.pdcconnector.PhysicalDataCenterConnectorDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
public class PhysicalDataCenterEntityToDTO
    implements ConnectorEntityToDTOMapper<PhysicalDataCenterConnectorDTO, PhysicalDataCenterConnector> {
  @Override
  public PhysicalDataCenterConnectorDTO createConnectorDTO(PhysicalDataCenterConnector connector) {
    return PhysicalDataCenterConnectorDTO.builder()
        .hostNames(connector.getHostNames())
        .sshKeyRef(SecretRefHelper.createSecretRef(connector.getSshKeyRef()))
        .build();
  }
}
