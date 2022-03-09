/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.event.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.event.OrchestrationLogPublisher;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.contracts.execution.events.AddExecutableResponseRequest;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.SdkResponseEventUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class AddExecutableResponseRequestProcessor implements SdkResponseProcessor {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationLogPublisher orchestrationLogPublisher;

  @Override
  public void handleEvent(SdkResponseEventProto event) {
    AddExecutableResponseRequest request = event.getAddExecutableResponseRequest();
    nodeExecutionService.updateV2(SdkResponseEventUtils.getNodeExecutionId(event),
        ops -> ops.addToSet(NodeExecutionKeys.executableResponses, request.getExecutableResponse()));
    orchestrationLogPublisher.createAndHandleEventLog(event.getAmbiance().getPlanExecutionId(),
        AmbianceUtils.obtainCurrentRuntimeId(event.getAmbiance()), OrchestrationEventType.NODE_EXECUTION_UPDATE);
  }
}
