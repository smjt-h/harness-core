package io.harness.ng.validator.service.api;

import io.harness.ng.validator.dto.HostValidationDTO;

import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

public interface HostValidationService {
  /**
   * Validate SSH hosts credentials and connectivity.
   *
   * @param hosts the host names
   * @param accountIdentifier the account identifier
   * @param orgIdentifier the account identifier
   * @param projectIdentifier the project identifier
   * @param secretIdentifier the secret identifier
   * @return the list of host validation results
   */
  List<HostValidationDTO> validateSSHHosts(@NotNull List<String> hosts, String accountIdentifier,
      @Nullable String orgIdentifier, @Nullable String projectIdentifier, @NotNull String secretIdentifier);

  /**
   * Validate SSH host credentials and connectivity.
   *
   * @param hostName the host name
   * @param accountIdentifier the account identifier
   * @param orgIdentifier the account identifier
   * @param projectIdentifier the project identifier
   * @param secretIdentifier the secret identifier
   * @return host validation result
   */
  HostValidationDTO validateSSHHost(@NotNull String hostName, String accountIdentifier, @Nullable String orgIdentifier,
      @Nullable String projectIdentifier, @NotNull String secretIdentifier);
}
