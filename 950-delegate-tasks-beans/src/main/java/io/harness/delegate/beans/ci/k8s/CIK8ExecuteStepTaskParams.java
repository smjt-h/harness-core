/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.ci.k8s;

import io.harness.delegate.beans.ci.CIExecuteStepTaskParams;
import io.harness.delegate.beans.executioncapability.DelegateSelectorCapabilityHelper;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.LiteEngineConnectionCapability;
import io.harness.expression.ExpressionEvaluator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CIK8ExecuteStepTaskParams implements CIExecuteStepTaskParams, ExecutionCapabilityDemander {
  @NotNull private String ip;
  @NotNull private int port;
  @NotNull private String delegateSvcEndpoint;
  private boolean isLocal;
  private List<String> delegateSelectors;
  @NotNull private byte[] serializedStep;

  @Builder.Default private static final CIExecuteStepTaskParams.Type type = CIExecuteStepTaskParams.Type.K8;

  @Override
  public CIExecuteStepTaskParams.Type getType() {
    return type;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> executionCapabilityList = new ArrayList<>();
    executionCapabilityList.add(LiteEngineConnectionCapability.builder().ip(ip).port(port).isLocal(isLocal).build());
    DelegateSelectorCapabilityHelper.populateDelegateSelectorCapability(
        executionCapabilityList, Sets.newHashSet(delegateSelectors), "Step");
    return executionCapabilityList;
  }
}
