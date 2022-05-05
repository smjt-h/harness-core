/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.merger;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.merger.fqn.FQN;
import io.harness.pms.merger.helpers.FQNMapGenerator;
import io.harness.pms.merger.helpers.YamlMapGenerator;
import io.harness.pms.yaml.YamlUtils;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Data
@Slf4j
public class YamlConfig {
  private String yaml;
  private JsonNode yamlMap;
  private Map<FQN, Object> fqnToValueMap;

  public YamlConfig(String yaml) {
    this.yaml = yaml;
    try {
      yamlMap = YamlUtils.readTree(yaml).getNode().getCurrJsonNode();
    } catch (IOException e) {
      log.error("Could not convert yaml to JsonNode. Yaml:\n" + yaml, e);
      throw new InvalidRequestException("Could not convert yaml to JsonNode: " + e.getMessage());
    }
    fqnToValueMap = FQNMapGenerator.generateFQNMap(yamlMap);
  }

  public YamlConfig(Map<FQN, Object> fqnToValueMap, JsonNode originalYaml) {
    this.fqnToValueMap = fqnToValueMap;
    yamlMap = YamlMapGenerator.generateYamlMap(fqnToValueMap, originalYaml, false);
    if (yamlMap.size() != 0) {
      yaml = YamlUtils.write(yamlMap).replace("---\n", "");
    }
  }

  public YamlConfig(Map<FQN, Object> fqnToValueMap, JsonNode originalYaml, boolean isSanitiseFlow) {
    this.fqnToValueMap = fqnToValueMap;
    yamlMap = YamlMapGenerator.generateYamlMap(fqnToValueMap, originalYaml, isSanitiseFlow);
    if (yamlMap.size() != 0) {
      yaml = YamlUtils.write(yamlMap).replace("---\n", "");
    }
  }
}
