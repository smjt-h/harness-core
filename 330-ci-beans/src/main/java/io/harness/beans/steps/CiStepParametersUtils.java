/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans.steps;

import io.harness.advisers.rollback.OnFailRollbackParameters;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.TaskSelector;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.StepElementParameters.StepElementParametersBuilder;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.steps.StepUtils;
import io.harness.utils.TimeoutUtils;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

import static io.harness.pms.yaml.YAMLFieldNameConstants.*;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STAGE;

@UtilityClass
@OwnedBy(HarnessTeam.CI)
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

  public ParameterField<List<TaskSelectorYaml>> getDelegateSelectors(PlanCreationContext ctx){
    ParameterField<List<TaskSelectorYaml>> delegateSelectors = null;
    try{
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
      return delegateSelectors;

    }catch (Exception e){

    }


    return delegateSelectors;
  }
}
