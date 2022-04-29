/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.creator.variables;

import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.nodes.RunTestStepNode;
import io.harness.beans.steps.outcome.CIStepOutcome;
import io.harness.pms.contracts.plan.YamlExtraProperties;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.sdk.core.pipeline.variables.GenericStepVariableCreator;
import io.harness.pms.sdk.core.variables.VariableCreatorHelper;
import io.harness.yaml.core.variables.OutputNGVariable;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RunTestStepVariableCreator extends GenericStepVariableCreator<RunTestStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(CIStepInfoType.RUN_TESTS.getDisplayName());
  }

  @Override
  public Class<RunTestStepNode> getFieldClass() {
    return RunTestStepNode.class;
  }

  @Override
  public YamlExtraProperties getStepExtraProperties(String fqnPrefix, String localNamePrefix, RunTestStepNode config) {
    YamlExtraProperties stepExtraProperties = super.getStepExtraProperties(fqnPrefix, localNamePrefix, config);

    Map<String, String> outputVariablesMap = new HashMap<>();
    if (config.getRunTestsStepInfo().getOutputVariables().getValue() != null) {
      List<OutputNGVariable> outputNGVariables = config.getRunTestsStepInfo().getOutputVariables().getValue();
      for (OutputNGVariable outputVariable : outputNGVariables) {
        outputVariablesMap.put(outputVariable.getName(), "variable");
      }
    }

    CIStepOutcome ciStepOutcome = CIStepOutcome.builder().outputVariables(outputVariablesMap).build();

    List<String> outputExpressions = VariableCreatorHelper.getExpressionsInObject(ciStepOutcome, "output");
    List<YamlProperties> outputProperties = new LinkedList<>();
    for (String outputExpression : outputExpressions) {
      outputProperties.add(YamlProperties.newBuilder()
                               .setFqn(fqnPrefix + "." + outputExpression)
                               .setLocalName(localNamePrefix + "." + outputExpression)
                               .setVisible(true)
                               .build());
    }

    return YamlExtraProperties.newBuilder()
        .addAllProperties(stepExtraProperties.getPropertiesList())
        .addAllOutputProperties(outputProperties)
        .build();
  }
}
