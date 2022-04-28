/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;

import software.wings.sm.ContextElementInfo;
import software.wings.sm.StateExecutionData;

import java.util.Map;
import lombok.Builder;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class SkipStateExecutionData extends StateExecutionData {
  private String skipAssertionExpression;
  private String workflowId;

  @Builder
  public SkipStateExecutionData(String stateName, String stateType, Long startTs, Long endTs, ExecutionStatus status,
      String errorMsg, Integer waitInterval, ContextElementInfo elementInfo, Map<String, Object> stateParams,
      Map<String, Object> templateVariable, String skipAssertionExpression, String workflowId) {
    super(stateName, stateType, startTs, endTs, status, errorMsg, waitInterval, elementInfo, stateParams,
        templateVariable);
    this.skipAssertionExpression = skipAssertionExpression;
    this.workflowId = workflowId;
  }

  public String getSkipAssertionExpression() {
    return skipAssertionExpression;
  }

  public void setSkipAssertionExpression(String skipAssertionExpression) {
    this.skipAssertionExpression = skipAssertionExpression;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(executionDetails, "skipAssertionExpression",
        ExecutionDataValue.builder().displayName("Skip Condition").value(skipAssertionExpression).build());
    return executionDetails;
  }
}
