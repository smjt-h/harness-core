package io.harness.ng.opa.entities.connector;

import io.harness.connector.ConnectorDTO;

public interface OpaConnectorService {
  void evaluatePoliciesWithJson(String accountId, String expandedJson, String orgIdentifier, String projectIdentifier,
      String action, String identifier);
  void evaluatePoliciesWithEntity(String accountId, ConnectorDTO connectorDTO, String orgIdentifier, String projectIdentifier,
                                  String action, String identifier);
}
