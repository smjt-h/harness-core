package io.harness.utils;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;

import java.util.List;

@OwnedBy(PIPELINE)
public interface WithDelegateSelector {
  /**
   *
   * @return list of delegate selector parameter field value
   */
  ParameterField<List<TaskSelectorYaml>> delegateSelectors();
}
