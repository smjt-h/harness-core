/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.steps.serverless;

import static io.harness.cdng.visitor.YamlTypes.SERVERLESS_AWS_LAMBDA_DEPLOY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.plan.steps.CDPMSStepPlanCreatorV2;
import io.harness.cdng.serverless.ServerlessAwsLambdaRollbackStepNode;
import io.harness.cdng.serverless.ServerlessAwsLambdaRollbackStepParameters;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import com.google.common.collect.Sets;
import java.util.Set;

@OwnedBy(HarnessTeam.CDP)
public class ServerlessAwsLambdaRollbackStepPlanCreator
    extends CDPMSStepPlanCreatorV2<ServerlessAwsLambdaRollbackStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.SERVERLESS_AWS_LAMBDA_ROLLBACK);
  }

  @Override
  public Class<ServerlessAwsLambdaRollbackStepNode> getFieldClass() {
    return ServerlessAwsLambdaRollbackStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(
      PlanCreationContext ctx, ServerlessAwsLambdaRollbackStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }

  @Override
  protected StepParameters getStepParameters(PlanCreationContext ctx, ServerlessAwsLambdaRollbackStepNode stepElement) {
    final StepParameters stepParameters = super.getStepParameters(ctx, stepElement);

    String serverlessAwsLambdaRollbackFnq = getExecutionStepFqn(ctx.getCurrentField(), SERVERLESS_AWS_LAMBDA_DEPLOY);
    ((ServerlessAwsLambdaRollbackStepParameters) ((StepElementParameters) stepParameters).getSpec())
        .setServerlessAwsLambdaRollbackFnq(serverlessAwsLambdaRollbackFnq);

    return stepParameters;
  }
}
