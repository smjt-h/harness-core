package io.harness.connector.mappers.pdcconnector;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.connector.entities.embedded.pdcconnector.Host;
import io.harness.connector.entities.embedded.pdcconnector.HostAttribute;
import io.harness.connector.entities.embedded.pdcconnector.PhysicalDataCenterConnector;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.pdcconnector.HostAttributeDTO;
import io.harness.delegate.beans.connector.pdcconnector.HostDTO;
import io.harness.delegate.beans.connector.pdcconnector.PhysicalDataCenterConnectorDTO;
import io.harness.encryption.SecretRefHelper;

import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

@Singleton
public class PhysicalDataCenterEntityToDTO
    implements ConnectorEntityToDTOMapper<PhysicalDataCenterConnectorDTO, PhysicalDataCenterConnector> {
  @Override
  public PhysicalDataCenterConnectorDTO createConnectorDTO(PhysicalDataCenterConnector connector) {
    return PhysicalDataCenterConnectorDTO.builder()
        .hosts(getHostDTOSFromHosts(connector.getHosts()))
        .sshKeyRef(SecretRefHelper.createSecretRef(connector.getSshKeyRef()))
        .build();
  }

  private List<HostDTO> getHostDTOSFromHosts(List<Host> hosts) {
    if (isEmpty(hosts)) {
      return Collections.emptyList();
    }

    return hosts.stream().filter(Objects::nonNull).map(this::getHostDTO).collect(Collectors.toList());
  }

  @NotNull
  private HostDTO getHostDTO(Host host) {
    HostDTO hostDTO = new HostDTO();
    hostDTO.setHostName(host.getHostName());
    hostDTO.setHostAttributes(getHostAttributesDTOsFromHostAttributes(host.getHostAttributes()));
    return hostDTO;
  }

  private List<HostAttributeDTO> getHostAttributesDTOsFromHostAttributes(List<HostAttribute> hostAttributes) {
    if (isEmpty(hostAttributes)) {
      return Collections.emptyList();
    }

    return hostAttributes.stream().filter(Objects::nonNull).map(this::getHostAttributeDTO).collect(Collectors.toList());
  }

  @NotNull
  private HostAttributeDTO getHostAttributeDTO(HostAttribute hostAttribute) {
    HostAttributeDTO hostAttributeDTO = new HostAttributeDTO();
    hostAttributeDTO.setName(hostAttribute.getName());
    hostAttributeDTO.setType(hostAttribute.getType());
    return hostAttributeDTO;
  }
}
