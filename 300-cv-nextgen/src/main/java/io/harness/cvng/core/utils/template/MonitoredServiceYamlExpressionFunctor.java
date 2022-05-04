/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.utils.template;

import io.harness.pms.yaml.YamlField;
import io.harness.yaml.utils.YamlPathUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MonitoredServiceYamlExpressionFunctor {
  // Root yaml map
  YamlField rootYamlField;

  public Object get(String expression) {
    Map<String, Map<String, Object>> fqnToValueMap = new HashMap<>();

    // Get the current element
    Map<String, Object> currentElementMap = YamlPathUtils.getYamlMap(rootYamlField, fqnToValueMap, new LinkedList<>());

    // Check child first
    if (currentElementMap.containsKey(expression)) {
      return currentElementMap.get(expression);
    }
    return null;
  }
}
