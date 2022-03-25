package io.harness.cdng.visitor.helpers.cdstepinfo;

import io.harness.cdng.gitOps.GitOpsConfigUpdateStepInfo;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class GitOpsConfigUpdateStepInfoVisitorHelper implements ConfigValidator {
  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return GitOpsConfigUpdateStepInfo.builder().build();
  }

  @Override
  public void validate(Object object, ValidationVisitor visitor) {}
}
