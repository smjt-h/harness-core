package io.harness.ng.opa;

import java.io.IOException;

public interface OpaService {
    void evaluate();
     OpaEvaluationContext createEvaluationContext(String yaml, String key) throws IOException;

}
