package io.harness.connector;

import io.harness.ng.core.dto.ErrorDetail;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Schema(description = "Validation details for the PDC Connector")
@Value
@EqualsAndHashCode(callSuper = true)
public class PhysicalDataCenterConnectorValidationResult extends ConnectorValidationResult {
  @Schema(description = "Validation passed host list") List<String> validationPassedHosts;
  @Schema(description = "Validation failed host list") List<String> validationFailedHosts;

  @Builder
  PhysicalDataCenterConnectorValidationResult(ConnectivityStatus status, List<ErrorDetail> errors, String errorSummary,
      long testedAt, String delegateId, List<String> validationPassedHosts, List<String> validationFailedHosts) {
    super(status, errors, errorSummary, testedAt, delegateId);
    this.validationPassedHosts = validationPassedHosts;
    this.validationFailedHosts = validationFailedHosts;
  }

  public static class PhysicalDataCenterConnectorValidationResultBuilder extends ConnectorValidationResultBuilder {}
}
