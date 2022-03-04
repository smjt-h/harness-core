/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.handlers;

import io.harness.debezium.ChangeHandler;
import io.harness.entity.PipelineExecutionSummaryDashboardEntity;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;
import com.google.protobuf.util.JsonFormat;
import io.debezium.engine.ChangeEvent;
import lombok.SneakyThrows;

@Singleton
public class PipelineExecutionSummaryHandler implements ChangeHandler {
  @SneakyThrows
  PipelineExecutionSummaryDashboardEntity deserialize(ChangeEvent<String, String> changeEvent) {
    return new ObjectMapper().readValue(changeEvent.value(), PipelineExecutionSummaryDashboardEntity.class);
  }

  @SneakyThrows
  ExecutionTriggerInfo getExecutionTriggerInfo(ChangeEvent<String, String> changeEvent) {
    JsonNode executionTriggerInfoNode =
        new ObjectMapper().readTree(changeEvent.value().getBytes()).get("executionTriggerInfo");
    String triggerInfoJson = new ObjectMapper().writeValueAsString(executionTriggerInfoNode);
    ExecutionTriggerInfo.Builder builder = ExecutionTriggerInfo.newBuilder();
    JsonFormat.parser().ignoringUnknownFields().merge(triggerInfoJson, builder);
    ExecutionTriggerInfo triggerInfo = builder.build();
    return triggerInfo;
  }

  @Override
  public void handleUpdateEvent(String id, ChangeEvent<String, String> changeEvent) {
    PipelineExecutionSummaryDashboardEntity pipelineExecutionSummaryDashboardEntity = deserialize(changeEvent);
    ExecutionTriggerInfo triggerInfo = getExecutionTriggerInfo(changeEvent);
  }

  @Override
  public void handleDeleteEvent(String id) {}

  @Override
  public void handleCreateEvent(String id, ChangeEvent<String, String> changeEvent) {
    PipelineExecutionSummaryDashboardEntity pipelineExecutionSummaryDashboardEntity = deserialize(changeEvent);
    ExecutionTriggerInfo triggerInfo = getExecutionTriggerInfo(changeEvent);
  }
}
