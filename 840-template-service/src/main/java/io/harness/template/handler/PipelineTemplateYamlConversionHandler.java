/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.template.handler;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.template.beans.NGTemplateConstants.IDENTIFIER;
import static io.harness.template.beans.NGTemplateConstants.NAME;
import static io.harness.template.beans.NGTemplateConstants.STAGES;
import static io.harness.template.beans.NGTemplateConstants.TEMPLATE_INPUTS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ngexception.NGTemplateException;
import io.harness.ng.core.template.TemplateEntityType;
import io.harness.pms.yaml.YamlField;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@OwnedBy(CDC)
public class PipelineTemplateYamlConversionHandler implements YamlConversionHandler {
  @Override
  public String getRootField(TemplateEntityType templateEntityType) {
    return templateEntityType.getRootYamlName();
  }

  @Override
  public TemplateYamlConversionData getAdditionalFieldsToAdd(
      TemplateEntityType templateEntityType, YamlField yamlField) {
    Map<String, Object> fieldsToAdd = new HashMap<>();
    fieldsToAdd.put(IDENTIFIER, TEMPLATE_INPUTS);
    fieldsToAdd.put(NAME, TEMPLATE_INPUTS);
    String rootYamlFieldName = getRootField(templateEntityType);
    YamlField rootYamlField = yamlField.getNode().getField(rootYamlFieldName);
    if (rootYamlField == null) {
      throw new NGTemplateException("yamlNode provided doesn not have root yaml field: " + rootYamlFieldName);
    }
    YamlField stagesYamlField = rootYamlField.getNode().getField(STAGES);
    if (stagesYamlField == null) {
      throw new NGTemplateException("yamlNode provided doesn not have type yaml field");
    }
    TemplateYamlConversionRecord conversionRecord = TemplateYamlParallelConversionRecord.builder()
                                                        .fieldsToAdd(fieldsToAdd)
                                                        .path(stagesYamlField.getYamlPath())
                                                        .build();
    return TemplateYamlConversionData.builder()
        .templateYamlConversionRecordList(Collections.singletonList(conversionRecord))
        .build();
  }
}
