package io.harness.connector;

import io.harness.ng.core.dto.ErrorDetail;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "This has validation details for the host")
public class HostValidationResult {
  public enum HostValidationStatus {
    SUCCESS,
    FAILED;

    public static HostValidationStatus fromBoolean(boolean status) {
      return status ? SUCCESS : FAILED;
    }
  }
  @Schema(description = "Hostname") private String host;
  @Schema(description = "This has the validation status for a host.") private HostValidationStatus status;
  @Schema(description = "Host error details") private ErrorDetail error;
}
