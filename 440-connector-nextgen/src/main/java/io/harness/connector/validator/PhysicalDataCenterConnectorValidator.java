package io.harness.connector.validator;

import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.HostValidationResult;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.pdcconnector.PhysicalDataCenterConnectorDTO;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.utils.PhysicalDataCenterUtils;
import io.harness.encryption.SecretRefHelper;
import io.harness.ng.validator.HostValidatoionService;

import com.google.inject.Inject;
import java.util.List;

public class PhysicalDataCenterConnectorValidator extends AbstractConnectorValidator {
  @Inject private HostValidatoionService hostValidatoionService;

  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    PhysicalDataCenterConnectorDTO connectorConfigDTO = (PhysicalDataCenterConnectorDTO) connectorConfig;
    return null;
  }

  @Override
  public String getTaskType() {
    return null;
  }

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO connectorDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    PhysicalDataCenterConnectorDTO connectorConfigDTO = (PhysicalDataCenterConnectorDTO) connectorDTO;
    List<String> hostNames = PhysicalDataCenterUtils.getHostsAsList(connectorConfigDTO.getHostNames());
    List<HostValidationResult> hostValidationResults =
        hostValidatoionService.validateSSHHosts(hostNames, accountIdentifier, orgIdentifier, projectIdentifier,
            SecretRefHelper.getSecretConfigString(connectorConfigDTO.getSshKeyRef()));
    return ConnectorValidationResult.builder().status(ConnectivityStatus.SUCCESS).build();
  }

  @Override
  public ConnectorValidationResult validate(ConnectorResponseDTO connectorResponseDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    return ConnectorValidationResult.builder().status(ConnectivityStatus.SUCCESS).build();
  }
}
