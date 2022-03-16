/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.pdcconnector;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.connector.ConnectorTestConstants.ATTRIBUTE_NAME_1;
import static io.harness.connector.ConnectorTestConstants.ATTRIBUTE_NAME_2;
import static io.harness.connector.ConnectorTestConstants.ATTRIBUTE_TYPE_1;
import static io.harness.connector.ConnectorTestConstants.ATTRIBUTE_TYPE_2;
import static io.harness.connector.ConnectorTestConstants.HOST_NAME_1;
import static io.harness.connector.ConnectorTestConstants.HOST_NAME_2;
import static io.harness.connector.ConnectorTestConstants.SSK_KEY_REF_IDENTIFIER;
import static io.harness.connector.ConnectorTestConstants.SSK_KEY_REF_IDENTIFIER_WITH_ACCOUNT_SCOPE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.pdcconnector.Host;
import io.harness.connector.entities.embedded.pdcconnector.HostAttribute;
import io.harness.connector.entities.embedded.pdcconnector.PhysicalDataCenterConnector;
import io.harness.delegate.beans.connector.pdcconnector.HostAttributeDTO;
import io.harness.delegate.beans.connector.pdcconnector.HostDTO;
import io.harness.delegate.beans.connector.pdcconnector.PhysicalDataCenterConnectorDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
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

@OwnedBy(CDP)
@RunWith(MockitoJUnitRunner.class)
public class PhysicalDataCenterDTOToEntityTest extends CategoryTest {
  @InjectMocks private PhysicalDataCenterDTOToEntity physicalDataCenterDTOToEntity;

  @Test
  @Owner(developers = OwnerRule.IVAN)
  @Category(UnitTests.class)
  public void testCreateConnectorDTO() {
    PhysicalDataCenterConnector physicalDataCenterConnector = physicalDataCenterDTOToEntity.toConnectorEntity(
        PhysicalDataCenterConnectorDTO.builder()
            .hosts(getHostDTOs())
            .sshKeyRef(SecretRefData.builder().identifier(SSK_KEY_REF_IDENTIFIER).scope(Scope.ACCOUNT).build())
            .build());

    assertThat(physicalDataCenterConnector).isNotNull();
    assertThat(physicalDataCenterConnector.getSshKeyRef()).isEqualTo(SSK_KEY_REF_IDENTIFIER_WITH_ACCOUNT_SCOPE);
    assertThat(physicalDataCenterConnector.getHosts().size()).isEqualTo(2);

    Host host1 = getHost(HOST_NAME_1);
    Host host2 = getHost(HOST_NAME_2);
    assertThat(physicalDataCenterConnector.getHosts()).contains(host1, host2);
  }

  private List<HostDTO> getHostDTOs() {
    return Arrays.asList(getHostDTO(HOST_NAME_1), getHostDTO(HOST_NAME_2));
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

  private Host getHost(String hostName) {
    return Host.builder().hostName(hostName).hostAttributes(getHostAttributes()).build();
  }

  @NotNull
  private List<HostAttribute> getHostAttributes() {
    return Arrays.asList(HostAttribute.builder().name(ATTRIBUTE_NAME_1).type(ATTRIBUTE_TYPE_1).build(),
        HostAttribute.builder().name(ATTRIBUTE_NAME_2).type(ATTRIBUTE_TYPE_2).build());
  }
}
