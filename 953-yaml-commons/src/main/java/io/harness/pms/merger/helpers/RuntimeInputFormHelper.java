/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.merger.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.common.NGExpressionUtils;
import io.harness.pms.merger.YamlConfig;
import io.harness.pms.merger.fqn.FQN;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class RuntimeInputFormHelper {
  public String createTemplateFromYaml(String templateYaml) {
    return createRuntimeInputForm(templateYaml, true);
  }

  public String createRuntimeInputForm(String yaml, boolean keepInput) {
    YamlConfig yamlConfig = new YamlConfig(yaml);
    Map<FQN, Object> fullMap = yamlConfig.getFqnToValueMap();
    Map<FQN, Object> templateMap = new LinkedHashMap<>();
    fullMap.keySet().forEach(key -> {
      String value = fullMap.get(key).toString().replace("\"", "");
      if ((keepInput && NGExpressionUtils.matchesInputSetPattern(value))
          || (!keepInput && !NGExpressionUtils.matchesInputSetPattern(value) && !key.isIdentifierOrVariableName()
              && !key.isType())) {
        templateMap.put(key, fullMap.get(key));
      }
    });
    return (new YamlConfig(templateMap, yamlConfig.getYamlMap())).getYaml();
  }
}
