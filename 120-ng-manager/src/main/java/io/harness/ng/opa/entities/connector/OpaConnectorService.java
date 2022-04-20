package io.harness.ng.opa.entities.connector;

import com.google.inject.Inject;
import io.harness.ng.opa.OpaService;

public interface OpaConnectorService{

     void evaluatePolicies(String accountId, String expandedJson, String orgIdentifier,
                                 String projectIdentifier, String action, String planExecutionId);
}
