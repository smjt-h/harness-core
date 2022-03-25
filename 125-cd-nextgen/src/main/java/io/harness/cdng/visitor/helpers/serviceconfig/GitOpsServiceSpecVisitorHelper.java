package io.harness.cdng.visitor.helpers.serviceconfig;

import io.harness.cdng.service.beans.GitOpsServiceSpec;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

public class GitOpsServiceSpecVisitorHelper implements ConfigValidator {
    @Override
    public Object createDummyVisitableElement(Object originalElement) {
        return GitOpsServiceSpec.builder().build();
    }

    @Override
    public void validate(Object object, ValidationVisitor visitor) {
        // nothing to validate
    }
}
