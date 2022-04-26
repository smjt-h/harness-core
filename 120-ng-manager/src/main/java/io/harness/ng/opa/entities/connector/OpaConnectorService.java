package io.harness.ng.opa.entities.connector;

public interface OpaConnectorService {
  void evaluatePolicies(String accountId, String expandedJson, String orgIdentifier, String projectIdentifier,
      String action, String identifier);
}
