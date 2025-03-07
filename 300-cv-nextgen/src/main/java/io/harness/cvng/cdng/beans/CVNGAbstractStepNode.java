/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.cdng.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.AbstractStepNode;
import io.harness.yaml.core.VariableExpression;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;

import java.util.List;
import lombok.Data;

@OwnedBy(HarnessTeam.CV)
@Data
public abstract class CVNGAbstractStepNode extends AbstractStepNode {
  // check if this field is needed or not
  @VariableExpression(skipVariableExpression = true) List<FailureStrategyConfig> failureStrategies;
}
