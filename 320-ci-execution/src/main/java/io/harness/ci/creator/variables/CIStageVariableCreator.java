/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.creator.variables;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.stages.IntegrationStageNode;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.sdk.core.variables.AbstractStageVariableCreator;
import io.harness.pms.sdk.core.variables.VariableCreatorHelper;
import io.harness.pms.sdk.core.variables.beans.VariableCreationContext;
import io.harness.pms.sdk.core.variables.beans.VariableCreationResponse;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.CI)
public class CIStageVariableCreator extends AbstractStageVariableCreator<IntegrationStageNode> {
  @Override
  public LinkedHashMap<String, VariableCreationResponse> createVariablesForChildrenNodes(
      VariableCreationContext ctx, YamlField config) {
    LinkedHashMap<String, VariableCreationResponse> responseMap = new LinkedHashMap<>();

    YamlField executionField =
        config.getNode().getField(YAMLFieldNameConstants.SPEC).getNode().getField(YAMLFieldNameConstants.EXECUTION);
    if (VariableCreatorHelper.isNotYamlFieldEmpty(executionField)) {
      Map<String, YamlField> executionDependencyMap = new HashMap<>();
      executionDependencyMap.put(executionField.getNode().getUuid(), executionField);
      responseMap.put(executionField.getNode().getUuid(),
          VariableCreationResponse.builder()
              .dependencies(DependenciesUtils.toDependenciesProto(executionDependencyMap))
              .build());
    }

    YamlField variablesField = config.getNode().getField(YAMLFieldNameConstants.VARIABLES);
    if (variablesField != null) {
      VariableCreationResponse variablesResponse =
          VariableCreatorHelper.createVariableResponseForVariables(variablesField, YAMLFieldNameConstants.STAGE);
      responseMap.put(variablesField.getNode().getUuid(), variablesResponse);
    }

    return responseMap;
  }

  @Override
  public VariableCreationResponse createVariablesForParentNode(VariableCreationContext ctx, YamlField config) {
    YamlNode node = config.getNode();
    String stageUUID = node.getUuid();
    Map<String, YamlProperties> yamlPropertiesMap = new LinkedHashMap<>();
    yamlPropertiesMap.put(stageUUID,
        YamlProperties.newBuilder()
            .setLocalName(YAMLFieldNameConstants.STAGE)
            .setFqn(YamlUtils.getFullyQualifiedName(node))
            .build());
    addVariablesForStage(yamlPropertiesMap, node);
    return VariableCreationResponse.builder().yamlProperties(yamlPropertiesMap).build();
  }

  private void addVariablesForStage(Map<String, YamlProperties> yamlPropertiesMap, YamlNode yamlNode) {
    YamlField nameField = yamlNode.getField(YAMLFieldNameConstants.NAME);
    if (nameField != null) {
      String nameFQN = YamlUtils.getFullyQualifiedName(nameField.getNode());
      yamlPropertiesMap.put(nameField.getNode().getCurrJsonNode().textValue(),
          YamlProperties.newBuilder().setLocalName(getStageLocalName(nameFQN)).setFqn(nameFQN).build());
    }
    YamlField descriptionField = yamlNode.getField(YAMLFieldNameConstants.DESCRIPTION);
    if (descriptionField != null) {
      String descriptionFQN = YamlUtils.getFullyQualifiedName(descriptionField.getNode());
      yamlPropertiesMap.put(descriptionField.getNode().getCurrJsonNode().textValue(),
          YamlProperties.newBuilder().setLocalName(getStageLocalName(descriptionFQN)).setFqn(descriptionFQN).build());
    }
  }

  private String getStageLocalName(String fqn) {
    String[] split = fqn.split("\\.");
    return fqn.replaceFirst(split[0], YAMLFieldNameConstants.STAGE);
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(YAMLFieldNameConstants.STAGE, Collections.singleton("CI"));
  }

  @Override
  public LinkedHashMap<String, VariableCreationResponse> createVariablesForChildrenNodesV2(
      VariableCreationContext ctx, IntegrationStageNode config) {
    return createVariablesForChildrenNodes(ctx, ctx.getCurrentField());
  }

  @Override
  public Class<IntegrationStageNode> getFieldClass() {
    return IntegrationStageNode.class;
  }
}
