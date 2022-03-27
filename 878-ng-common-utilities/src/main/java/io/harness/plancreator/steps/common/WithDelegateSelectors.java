package io.harness.plancreator.steps.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;
import io.harness.reflection.ReflectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.List;

public interface WithDelegateSelectors {

    @JsonIgnore
    default SpecParameters getSpecParametersWithDelegateSelector(SpecParameters specParameters, ParameterField<List<TaskSelectorYaml>> delegateSelectors) throws NoSuchFieldException, IllegalAccessException {
        Field fieldv = specParameters.getClass().getSuperclass().getDeclaredField("delegateSelectors");
        fieldv.set(FieldUtils.getField(SpecParameters.class,"delegateSelectors"),delegateSelectors);
        //ReflectionUtils.setObjectField(field, specParameters, field.get(specParameters));

        return specParameters;
    }
}
