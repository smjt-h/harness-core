package io.harness.connector.mappers.pdcconnector;

import io.harness.connector.entities.embedded.pdcconnector.PhysicalDataCenterConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.pdcconnector.PhysicalDataCenterConnectorDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;

@Singleton
public class PhysicalDataCenterDTOToEntity
    implements ConnectorDTOToEntityMapper<PhysicalDataCenterConnectorDTO, PhysicalDataCenterConnector> {
  @Override
  public PhysicalDataCenterConnector toConnectorEntity(PhysicalDataCenterConnectorDTO connectorDTO) {
    return PhysicalDataCenterConnector.builder()
        .hostNames(connectorDTO.getHostNames())
        .sshKeyRef(SecretRefHelper.getSecretConfigString(connectorDTO.getSshKeyRef()))
        .build();
  }
}
