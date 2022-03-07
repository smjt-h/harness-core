package io.harness.connector.validator;

import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.HostValidationResult;
import io.harness.delegate.beans.connector.pdcconnector.HostDTO;
import io.harness.delegate.beans.connector.pdcconnector.PhysicalDataCenterConnectorDTO;
import io.harness.encryption.SecretRefHelper;
import io.harness.ng.validator.HostValidatoionService;

import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class PhysicalDataCenterConnectorValidator implements ConnectionValidator<PhysicalDataCenterConnectorDTO> {
  @Inject private HostValidatoionService hostValidatoionService;

  @Override
  public ConnectorValidationResult validate(PhysicalDataCenterConnectorDTO connectorDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    List<String> hostNames = connectorDTO.getHosts().stream().map(HostDTO::getHostName).collect(Collectors.toList());
    List<HostValidationResult> hostValidationResults =
        hostValidatoionService.validateSSHHosts(hostNames, accountIdentifier, orgIdentifier, projectIdentifier,
            SecretRefHelper.getSecretConfigString(connectorDTO.getSshKeyRef()));

    return buildConnectorValidationResult(hostValidationResults);
  }

  private ConnectorValidationResult buildConnectorValidationResult(List<HostValidationResult> hostValidationResults) {
    return ConnectorValidationResult.builder().status(ConnectivityStatus.SUCCESS).build();
  }

  @Override
  public ConnectorValidationResult validate(ConnectorResponseDTO connectorResponseDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    return null;
  }
}
