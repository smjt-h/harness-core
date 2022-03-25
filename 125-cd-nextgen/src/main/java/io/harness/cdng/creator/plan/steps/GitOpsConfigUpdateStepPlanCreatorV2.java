package io.harness.cdng.creator.plan.steps;

import io.harness.cdng.gitOps.GitOpsConfigUpdateStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class GitOpsConfigUpdateStepPlanCreatorV2 extends CDPMSStepPlanCreatorV2<GitOpsConfigUpdateStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.GITOPS_CONFIG_UPDATE);
  }

  @Override
  public Class<GitOpsConfigUpdateStepNode> getFieldClass() {
    return GitOpsConfigUpdateStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, GitOpsConfigUpdateStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
