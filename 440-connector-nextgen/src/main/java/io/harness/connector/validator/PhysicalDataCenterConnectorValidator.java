package io.harness.connector.validator;

import static java.lang.String.format;

import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.HostValidationResult;
import io.harness.delegate.beans.connector.pdcconnector.HostDTO;
import io.harness.delegate.beans.connector.pdcconnector.PhysicalDataCenterConnectorDTO;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.ng.validator.HostValidationService;

import com.google.inject.Inject;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class PhysicalDataCenterConnectorValidator implements ConnectionValidator<PhysicalDataCenterConnectorDTO> {
  @Inject private HostValidationService hostValidationService;

  @Override
  public ConnectorValidationResult validate(PhysicalDataCenterConnectorDTO connectorDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    long startTestingAt = System.currentTimeMillis();
    List<HostValidationResult> hostValidationResults =
        hostValidationService.validateSSHHosts(getHostNames(connectorDTO), accountIdentifier, orgIdentifier,
            projectIdentifier, connectorDTO.getSshKeyRef().getIdentifier());

    return buildConnectorValidationResult(hostValidationResults, startTestingAt);
  }

  @Override
  public ConnectorValidationResult validate(ConnectorResponseDTO connectorResponseDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    return null;
  }

  @NotNull
  private List<String> getHostNames(PhysicalDataCenterConnectorDTO connectorDTO) {
    return connectorDTO.getHosts().stream().map(HostDTO::getHostName).collect(Collectors.toList());
  }

  private ConnectorValidationResult buildConnectorValidationResult(
      List<HostValidationResult> hostValidationResults, long startTestingAt) {
    ConnectivityStatus connectivityStatus = hostValidationResults.stream().anyMatch(isHostValidationFailed())
        ? ConnectivityStatus.FAILURE
        : ConnectivityStatus.SUCCESS;

    return connectivityStatus == ConnectivityStatus.SUCCESS
        ? buildConnectorValidationResultSuccess(startTestingAt)
        : buildConnectorValidationResultFailure(hostValidationResults, startTestingAt);
  }

  private ConnectorValidationResult buildConnectorValidationResultSuccess(long startTestingAt) {
    return ConnectorValidationResult.builder().testedAt(startTestingAt).status(ConnectivityStatus.SUCCESS).build();
  }

  private ConnectorValidationResult buildConnectorValidationResultFailure(
      List<HostValidationResult> hostValidationResults, long startTestingAt) {
    List<HostValidationResult> failedValidationHostResults =
        hostValidationResults.stream().filter(isHostValidationFailed()).collect(Collectors.toList());

    List<ErrorDetail> errorDetails =
        failedValidationHostResults.stream().map(HostValidationResult::getError).collect(Collectors.toList());

    String errorSummary =
        format("Validation failed for hosts: %s", StringUtils.join(toHostNames(failedValidationHostResults), ','));

    return ConnectorValidationResult.builder()
        .errors(errorDetails)
        .errorSummary(errorSummary)
        .testedAt(startTestingAt)
        .status(ConnectivityStatus.FAILURE)
        .build();
  }

  @NotNull
  private Predicate<HostValidationResult> isHostValidationFailed() {
    return hostValidationResult -> hostValidationResult.getStatus() == HostValidationResult.HostValidationStatus.FAILED;
  }

  @NotNull
  private List<String> toHostNames(List<HostValidationResult> hostValidationResults) {
    return hostValidationResults.stream().map(HostValidationResult::getHost).collect(Collectors.toList());
  }
}
