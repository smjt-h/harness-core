package io.harness.connector.mappers.pdcconnector;

import static io.harness.connector.ConnectorTestConstants.ATTRIBUTE_NAME_1;
import static io.harness.connector.ConnectorTestConstants.ATTRIBUTE_NAME_2;
import static io.harness.connector.ConnectorTestConstants.ATTRIBUTE_TYPE_1;
import static io.harness.connector.ConnectorTestConstants.ATTRIBUTE_TYPE_2;
import static io.harness.connector.ConnectorTestConstants.HOST_NAME_1;
import static io.harness.connector.ConnectorTestConstants.HOST_NAME_2;
import static io.harness.connector.ConnectorTestConstants.SSK_KEY_REF_IDENTIFIER;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.pdcconnector.Host;
import io.harness.connector.entities.embedded.pdcconnector.HostAttribute;
import io.harness.connector.entities.embedded.pdcconnector.PhysicalDataCenterConnector;
import io.harness.delegate.beans.connector.pdcconnector.HostAttributeDTO;
import io.harness.delegate.beans.connector.pdcconnector.HostDTO;
import io.harness.delegate.beans.connector.pdcconnector.PhysicalDataCenterConnectorDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PhysicalDataCenterEntityToDTOTest extends CategoryTest {
  @InjectMocks private PhysicalDataCenterEntityToDTO physicalDataCenterEntityToDTO;

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testCreateConnectorDTO() {
    PhysicalDataCenterConnectorDTO connectorDTO = physicalDataCenterEntityToDTO.createConnectorDTO(
        PhysicalDataCenterConnector.builder().hosts(getHosts()).sshKeyRef(SSK_KEY_REF_IDENTIFIER).build());

    assertThat(connectorDTO).isNotNull();
    assertThat(connectorDTO.getSshKeyRef().getIdentifier()).isEqualTo(SSK_KEY_REF_IDENTIFIER);
    assertThat(connectorDTO.getHosts().size()).isEqualTo(2);

    HostDTO hostDTO1 = getHostDTO(HOST_NAME_1);
    HostDTO hostDTO2 = getHostDTO(HOST_NAME_2);
    assertThat(connectorDTO.getHosts()).contains(hostDTO1, hostDTO2);
  }

  private List<Host> getHosts() {
    return Arrays.asList(getHost(HOST_NAME_1), getHost(HOST_NAME_2));
  }

  private Host getHost(String hostName) {
    return Host.builder().hostName(hostName).hostAttributes(getHostAttributes()).build();
  }

  @NotNull
  private List<HostAttribute> getHostAttributes() {
    return Arrays.asList(HostAttribute.builder().name(ATTRIBUTE_NAME_1).type(ATTRIBUTE_TYPE_1).build(),
        HostAttribute.builder().name(ATTRIBUTE_NAME_2).type(ATTRIBUTE_TYPE_2).build());
  }

  private HostDTO getHostDTO(String hostName) {
    HostDTO hostDTO = new HostDTO();
    hostDTO.setHostName(hostName);
    hostDTO.setHostAttributes(getListHostAttributeDTOs());

    return hostDTO;
  }

  private List<HostAttributeDTO> getListHostAttributeDTOs() {
    HostAttributeDTO hostAttributeDTO1 = new HostAttributeDTO();
    hostAttributeDTO1.setType(ATTRIBUTE_TYPE_1);
    hostAttributeDTO1.setName(ATTRIBUTE_NAME_1);

    HostAttributeDTO hostAttributeDTO2 = new HostAttributeDTO();
    hostAttributeDTO2.setType(ATTRIBUTE_TYPE_2);
    hostAttributeDTO2.setName(ATTRIBUTE_NAME_2);

    return Arrays.asList(hostAttributeDTO1, hostAttributeDTO2);
  }
}
