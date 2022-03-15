/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.debezium.DebeziumChangeEvent;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.sdk.execution.events.PmsCommonsBaseEventHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class TimescaleHandler implements PmsCommonsBaseEventHandler<DebeziumChangeEvent> {
  public static ObjectMapper objectMapper;
  @Inject PipelineExecutionSummaryHandlerCd pipelineExecutionSummaryHandlerCd;
  @Inject PipelineExecutionSummaryHandlerCi pipelineExecutionSummaryHandlerCi;

  @SneakyThrows
  PipelineExecutionSummaryEntity deserialize(String value) {
    return objectMapper.readValue(value, PipelineExecutionSummaryEntity.class);
  }

  @SneakyThrows
  @Override
  public void handleEvent(DebeziumChangeEvent event, Map<String, String> metadataMap, long timestamp) {
    String optype = event.getOptype();
    String id = objectMapper.readValue(event.getKey(), String.class);
    PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity = deserialize(event.getValue());
    switch (optype) {
      case "SNAPSHOT":
      case "CREATE":
        handleCreateEvent(id, pipelineExecutionSummaryEntity);
        break;
      case "UPDATE":
        handleUpdateEvent(id, pipelineExecutionSummaryEntity);
        break;
      case "DELETE":
        handleDeleteEvent(id);
        break;
      default:
        break;
    }
  }

  public void handleUpdateEvent(String id, PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity) {
    if (pipelineExecutionSummaryEntity.getModuleInfo().containsKey("ci")) {
      pipelineExecutionSummaryHandlerCi.update(id, pipelineExecutionSummaryEntity);
    }
    if (pipelineExecutionSummaryEntity.getModuleInfo().containsKey("cd")) {
      pipelineExecutionSummaryHandlerCd.update(id, pipelineExecutionSummaryEntity);
    }
  }

  public void handleDeleteEvent(String id) {
    pipelineExecutionSummaryHandlerCd.delete(id);
    pipelineExecutionSummaryHandlerCi.delete(id);
  }

  public void handleCreateEvent(String id, PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity) {
    if (pipelineExecutionSummaryEntity.getModuleInfo().containsKey("ci")) {
      pipelineExecutionSummaryHandlerCi.insert(id, pipelineExecutionSummaryEntity);
    }
    if (pipelineExecutionSummaryEntity.getModuleInfo().containsKey("cd")) {
      pipelineExecutionSummaryHandlerCd.insert(id, pipelineExecutionSummaryEntity);
    }
  }
}
