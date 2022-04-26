package io.harness.ng.opa;

import io.harness.pms.contracts.governance.GovernanceMetadata;

import java.io.IOException;

public interface OpaService {
  GovernanceMetadata evaluate(OpaEvaluationContext context, String accountId, String orgIdentifier,
      String projectIdentifier, String identifier, String action, String key);
  OpaEvaluationContext createEvaluationContext(String yaml, String key) throws IOException;
}
