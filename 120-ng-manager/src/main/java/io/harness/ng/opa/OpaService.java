package io.harness.ng.opa;

import java.io.IOException;

public interface OpaService {
  void evaluate(OpaEvaluationContext context, String accountId, String orgIdentifier, String projectIdentifier,
      String identifier, String action, String key);
  OpaEvaluationContext createEvaluationContext(String yaml, String key) throws IOException;
}
