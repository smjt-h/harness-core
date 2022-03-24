package io.harness.utils;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.CollectionUtils;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;

import java.util.List;

@OwnedBy(PIPELINE)
public interface WithDelegateSelector {
  /**
   *
   * @return list of delegate selector parameter field value
   */
  default ParameterField<List<TaskSelectorYaml>> delegateSelectors( ParameterField<List<TaskSelectorYaml>> delegateSelectors){
    return ParameterField.createValueField(
            CollectionUtils.emptyIfNull(delegateSelectors != null ? delegateSelectors.getValue() : null));
  }

}
