/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.variables;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.helm.HelmRollbackStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.pipeline.variables.GenericStepVariableCreator;

import java.util.Collections;
import java.util.Set;

@OwnedBy(HarnessTeam.CDP)
public class HelmRollbackStepVariableCreator extends GenericStepVariableCreator<HelmRollbackStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Collections.singleton(StepSpecTypeConstants.HELM_ROLLBACK);
  }

  @Override
  public Class<HelmRollbackStepNode> getFieldClass() {
    return HelmRollbackStepNode.class;
  }
}
