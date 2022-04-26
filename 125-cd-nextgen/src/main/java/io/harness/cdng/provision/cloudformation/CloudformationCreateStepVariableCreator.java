package io.harness.cdng.provision.cloudformation;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.pipeline.variables.GenericStepVariableCreator;

import java.util.Collections;
import java.util.Set;

@OwnedBy(CDP)
public class CloudformationCreateStepVariableCreator
    extends GenericStepVariableCreator<CloudformationCreateStackStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Collections.singleton(StepSpecTypeConstants.CLOUDFORMATION_CREATE_STACK);
  }

  @Override
  public Class<CloudformationCreateStackStepNode> getFieldClass() {
    return CloudformationCreateStackStepNode.class;
  }
}
