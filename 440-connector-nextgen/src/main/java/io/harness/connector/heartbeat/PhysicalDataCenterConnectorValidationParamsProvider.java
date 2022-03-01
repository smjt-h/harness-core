package io.harness.connector.heartbeat;

import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.pdcconnector.PhysicalDataCenterConnectorDTO;
import io.harness.delegate.beans.connector.pdcconnector.PhysicalDataCenterConnectorValidationParams;

public class PhysicalDataCenterConnectorValidationParamsProvider implements ConnectorValidationParamsProvider {
  @Override
  public ConnectorValidationParams getConnectorValidationParams(ConnectorInfoDTO connectorInfoDTO, String connectorName,
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return PhysicalDataCenterConnectorValidationParams.builder()
        .physicalDataCenterConnectorDTO((PhysicalDataCenterConnectorDTO) connectorInfoDTO.getConnectorConfig())
        .connectorName(connectorName)
        .build();
  }
}
