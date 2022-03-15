package io.harness.ng.validator.service.api;

import io.harness.ng.validator.dto.HostValidationDTO;

import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

public interface HostValidationService {
  /**
   * Validate SSH hosts credentials and connectivity.
   *
   * @param hosts the hosts (the host is host name and port number, or only host name)
   * @param accountIdentifier the account identifier
   * @param orgIdentifier the account identifier
   * @param projectIdentifier the project identifier
   * @param secretIdentifierWithScope the secret identifier with scope
   * @return the list of host validation results
   */
  List<HostValidationDTO> validateSSHHosts(@NotNull List<String> hosts, String accountIdentifier,
      @Nullable String orgIdentifier, @Nullable String projectIdentifier, @NotNull String secretIdentifierWithScope);

  /**
   * Validate SSH host credentials and connectivity.
   *
   * @param host the host is host name and port number, or only host name
   * @param accountIdentifier the account identifier
   * @param orgIdentifier the account identifier
   * @param projectIdentifier the project identifier
   * @param secretIdentifierWithScope the secret identifier with scope
   * @return host validation result
   */
  HostValidationDTO validateSSHHost(@NotNull String host, String accountIdentifier, @Nullable String orgIdentifier,
      @Nullable String projectIdentifier, @NotNull String secretIdentifierWithScope);
}
