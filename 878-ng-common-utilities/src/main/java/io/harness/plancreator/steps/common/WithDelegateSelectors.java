package io.harness.plancreator.steps.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;

import java.util.List;

public interface WithDelegateSelectors {

    @JsonIgnore
    default ParameterField<List<String>> delegateSelectorsAsList(ParameterField<List<String>> delegateSelectors) {
        return delegateSelectors;
    }

    @JsonIgnore
    default ParameterField<List<TaskSelectorYaml>> delegateSelectorsOnTaskSelectorYaml(ParameterField<List<TaskSelectorYaml>> delegateSelectors) {
        return delegateSelectors;
    }
}
