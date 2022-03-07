package io.harness.connector.mappers.pdcconnector;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.connector.entities.embedded.pdcconnector.Host;
import io.harness.connector.entities.embedded.pdcconnector.HostAttribute;
import io.harness.connector.entities.embedded.pdcconnector.PhysicalDataCenterConnector;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.pdcconnector.HostAttributeDTO;
import io.harness.delegate.beans.connector.pdcconnector.HostDTO;
import io.harness.delegate.beans.connector.pdcconnector.PhysicalDataCenterConnectorDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Singleton
public class PhysicalDataCenterDTOToEntity
    implements ConnectorDTOToEntityMapper<PhysicalDataCenterConnectorDTO, PhysicalDataCenterConnector> {
  @Override
  public PhysicalDataCenterConnector toConnectorEntity(PhysicalDataCenterConnectorDTO connectorDTO) {
    return PhysicalDataCenterConnector.builder()
        .hosts(getHostsFromHostDTOs(connectorDTO.getHosts()))
        .sshKeyRef(SecretRefHelper.getSecretConfigString(connectorDTO.getSshKeyRef()))
        .build();
  }

  private List<Host> getHostsFromHostDTOs(List<HostDTO> hostDTOs) {
    if (isEmpty(hostDTOs)) {
      return Collections.emptyList();
    }

    return hostDTOs.stream()
        .filter(Objects::nonNull)
        .map(hostDTO
            -> Host.builder()
                   .hostName(hostDTO.getHostName())
                   .hostAttributes(getHostAttributesFromHostAttributesDTOs(hostDTO.getHostAttributes()))
                   .build())
        .collect(Collectors.toList());
  }

  private List<HostAttribute> getHostAttributesFromHostAttributesDTOs(List<HostAttributeDTO> hostAttributeDTOs) {
    if (isEmpty(hostAttributeDTOs)) {
      return Collections.emptyList();
    }

    return hostAttributeDTOs.stream()
        .filter(Objects::nonNull)
        .map(hostAttributeDTO
            -> HostAttribute.builder().name(hostAttributeDTO.getName()).type(hostAttributeDTO.getType()).build())
        .collect(Collectors.toList());
  }
}
