/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.handlers;

import io.harness.debezium.ChangeHandler;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.debezium.engine.ChangeEvent;
import lombok.SneakyThrows;

@Singleton
public class PipelineExecutionSummaryHandler implements ChangeHandler {
  @Inject QueryForCd queryForCd;
  @Inject QueryForCi queryForCi;
  ObjectMapper objectMapper;

  public PipelineExecutionSummaryHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @SneakyThrows
  PipelineExecutionSummaryEntity deserialize(ChangeEvent<String, String> changeEvent) {
    String s2 = changeEvent.value();
    PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity =
        objectMapper.readValue(s2, PipelineExecutionSummaryEntity.class);
    return pipelineExecutionSummaryEntity;
  }

  @Override
  public void handleUpdateEvent(String id, ChangeEvent<String, String> changeEvent) {
    PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity = deserialize(changeEvent);
    if (pipelineExecutionSummaryEntity.getModuleInfo().containsKey("ci"))
      queryForCi.update(id, pipelineExecutionSummaryEntity);
    if (pipelineExecutionSummaryEntity.getModuleInfo().containsKey("cd"))
      queryForCd.update(id, pipelineExecutionSummaryEntity);
  }

  @Override
  public void handleDeleteEvent(String id) {
    queryForCd.delete(id);
    queryForCi.delete(id);
  }

  @Override
  public void handleCreateEvent(String id, ChangeEvent<String, String> changeEvent) {
    PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity = deserialize(changeEvent);
    if (pipelineExecutionSummaryEntity.getModuleInfo().containsKey("ci"))
      queryForCi.insert(id, pipelineExecutionSummaryEntity);
    if (pipelineExecutionSummaryEntity.getModuleInfo().containsKey("cd"))
      queryForCd.insert(id, pipelineExecutionSummaryEntity);
  }
}
