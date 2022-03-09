package io.harness.ng.validator;

import io.harness.connector.HostValidationResult;

import java.util.List;

public interface HostValidationService {
  List<HostValidationResult> validateSSHHosts(List<String> hosts, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String secretIdentifier);

  HostValidationResult validateSSHHost(
      String host, String accountIdentifier, String orgIdentifier, String projectIdentifier, String secretIdentifier);
}
