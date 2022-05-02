/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.steps.servicenow;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.HINGER;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.sdk.core.variables.beans.VariableCreationContext;
import io.harness.pms.sdk.core.variables.beans.VariableCreationResponse;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;
import io.harness.steps.servicenow.create.ServiceNowCreateStepNode;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class ServiceNowCreateStepVariableCreatorTest extends CategoryTest {
  ServiceNowCreateStepVariableCreator serviceNowCreateStepVariableCreator = new ServiceNowCreateStepVariableCreator();

  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void getClassType() {
    assertThat(serviceNowCreateStepVariableCreator.getFieldClass()).isEqualTo(ServiceNowCreateStepNode.class);
  }

  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void createVariablesForParentNode() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("serviceNowVariableCreatorUuidJson.yaml");
    String pipelineJson = Resources.toString(testFile, Charsets.UTF_8);
    YamlField fullYamlField = YamlUtils.readTree(pipelineJson);

    // Pipeline Node
    YamlField stepField = fullYamlField.getNode()
                              .getField("pipeline")
                              .getNode()
                              .getField("stages")
                              .getNode()
                              .asArray()
                              .get(0)
                              .getField("stage")
                              .getNode()
                              .getField("spec")
                              .getNode()
                              .getField("execution")
                              .getNode()
                              .getField("steps")
                              .getNode()
                              .asArray()
                              .get(0)
                              .getField("step");

    // yaml input expressions
    VariableCreationResponse variablesForParentNodeV2 =
        serviceNowCreateStepVariableCreator.createVariablesForParentNodeV2(
            VariableCreationContext.builder().currentField(stepField).build(),
            YamlUtils.read(stepField.getNode().toString(), ServiceNowCreateStepNode.class));

    List<String> fqnPropertiesList = variablesForParentNodeV2.getYamlProperties()
                                         .values()
                                         .stream()
                                         .map(YamlProperties::getFqn)
                                         .collect(Collectors.toList());
    assertThat(fqnPropertiesList)
        .containsOnly("pipeline.stages.stage1.spec.execution.steps.snow_create.name",
            "pipeline.stages.stage1.spec.execution.steps.snow_create.description",
            "pipeline.stages.stage1.spec.execution.steps.snow_create.timeout",
            "pipeline.stages.stage1.spec.execution.steps.snow_create.spec.ticketType",
            "pipeline.stages.stage1.spec.execution.steps.snow_create.spec.connectorRef",
            "pipeline.stages.stage1.spec.execution.steps.snow_create.spec.delegateSelectors",
            "pipeline.stages.stage1.spec.execution.steps.snow_create.spec.useServiceNowTemplate",
            "pipeline.stages.stage1.spec.execution.steps.snow_create.spec.templateName");

    // yaml extra properties
    List<String> fqnExtraPropertiesList = variablesForParentNodeV2.getYamlExtraProperties()
                                              .get("-sAzEwZxQ6akzAzTfrdQyw") // pipeline uuid
                                              .getPropertiesList()
                                              .stream()
                                              .map(YamlProperties::getFqn)
                                              .collect(Collectors.toList());
    assertThat(fqnExtraPropertiesList)
        .containsOnly("pipeline.stages.stage1.spec.execution.steps.snow_create.identifier",
            "pipeline.stages.stage1.spec.execution.steps.snow_create.type",
            "pipeline.stages.stage1.spec.execution.steps.snow_create.startTs",
            "pipeline.stages.stage1.spec.execution.steps.snow_create.endTs",
            "pipeline.stages.stage1.spec.execution.steps.snow_create.when");
  }
}
