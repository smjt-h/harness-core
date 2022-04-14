/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans.steps;

import static io.harness.pms.yaml.YAMLFieldNameConstants.PIPELINE;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STAGE;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STEP;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STEP_GROUP;

import io.harness.advisers.rollback.OnFailRollbackParameters;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.StepElementParameters.StepElementParametersBuilder;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.StepUtils;
import io.harness.utils.TimeoutUtils;

import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@OwnedBy(HarnessTeam.CI)
@Slf4j
public class CiStepParametersUtils {
  public StepElementParametersBuilder getStepParameters(CIAbstractStepNode stepNode) {
    StepElementParametersBuilder stepBuilder = StepElementParameters.builder();
    stepBuilder.name(stepNode.getName());
    stepBuilder.identifier(stepNode.getIdentifier());
    stepBuilder.description(stepNode.getDescription());
    stepBuilder.skipCondition(stepNode.getSkipCondition());
    stepBuilder.timeout(ParameterField.createValueField(TimeoutUtils.getTimeoutString(stepNode.getTimeout())));
    stepBuilder.when(stepNode.getWhen());
    stepBuilder.type(stepNode.getType());
    stepBuilder.uuid(stepNode.getUuid());

    return stepBuilder;
  }
  public StepElementParametersBuilder getStepParameters(
      CIAbstractStepNode stepNode, OnFailRollbackParameters failRollbackParameters) {
    StepElementParametersBuilder stepBuilder = getStepParameters(stepNode);
    stepBuilder.rollbackParameters(failRollbackParameters);
    return stepBuilder;
  }

  public ParameterField<List<TaskSelectorYaml>> getDelegateSelectors(PlanCreationContext ctx) {
    ParameterField<List<TaskSelectorYaml>> delegateSelectors = null;
    try {
      // there should not be any selectors at step level for CI
      if (!ParameterField.isNull(StepUtils.delegateSelectorsFromFqn(ctx, STEP))) {
        log.warn("Getting delegate selectors at step level in CI");
      }

      // Delegate Selector Precedence: 1)stepGroup -> 2)Stage ->  3)Pipeline
      delegateSelectors = StepUtils.delegateSelectorsFromFqn(ctx, STEP_GROUP);
      if (!ParameterField.isNull(delegateSelectors)) {
        delegateSelectors.getValue().forEach(selector -> selector.setOrigin(STEP_GROUP));
        return delegateSelectors;
      }

      delegateSelectors = StepUtils.delegateSelectorsFromFqn(ctx, STAGE);
      if (!ParameterField.isNull(delegateSelectors)) {
        delegateSelectors.getValue().forEach(selector -> selector.setOrigin(STAGE));
        return delegateSelectors;
      }

      delegateSelectors = StepUtils.delegateSelectorsFromFqn(ctx, PIPELINE);
      if (!ParameterField.isNull(delegateSelectors)) {
        delegateSelectors.getValue().forEach(selector -> selector.setOrigin(PIPELINE));
        return delegateSelectors;
      }
    } catch (Exception e) {
      log.error("Exception while getting delegate selectors from yaml.", e);
    }
    return delegateSelectors;
  }
}
