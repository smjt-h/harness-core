package io.harness.utils;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.CollectionUtils;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.yaml.ParameterField;

import java.util.List;

@OwnedBy(PIPELINE)
public interface WithDelegateSelector {

  @JsonIgnore
  default ParameterField<List<String>> delegateSelectors() {
    return null;
  }

  @JsonIgnore
  default ParameterField<List<TaskSelectorYaml>> delegateSelectorsOnTaskSelectorYaml() {
    return null;
  }

}
