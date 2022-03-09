package io.harness.ng.validator;

import io.harness.connector.HostValidationResult;

import java.util.List;
import java.util.Optional;

public interface HostValidationService {
  List<HostValidationResult> validateSSHHosts(List<String> hosts, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String secretIdentifier, Optional<Integer> limit);

  HostValidationResult validateSSHHost(
      String host, String accountIdentifier, String orgIdentifier, String projectIdentifier, String secretIdentifier);
}
