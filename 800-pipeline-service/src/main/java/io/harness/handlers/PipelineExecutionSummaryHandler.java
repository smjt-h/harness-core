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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.inject.Singleton;
import io.debezium.engine.ChangeEvent;
import lombok.SneakyThrows;

@Singleton
public class PipelineExecutionSummaryHandler implements ChangeHandler {
  @SneakyThrows
  PipelineExecutionSummaryDashboardEntity deserialize(ChangeEvent<String, String> changeEvent) {
    ObjectMapper om = new ObjectMapper();

    // creating a module
    SimpleModule module = new SimpleModule();
    // adding our custom serializer and deserializer
    module.addDeserializer(ExecutionTriggerInfo.class, new TriggerInfoDeserializer());
    // registering the module with ObjectMapper
    om.registerModule(module);

    String s2 = changeEvent.value();

    PipelineExecutionSummaryDashboardEntity p = om.readValue(s2, PipelineExecutionSummaryDashboardEntity.class);
    return p;
  }

  @Override
  public void handleUpdateEvent(String id, ChangeEvent<String, String> changeEvent) {
    PipelineExecutionSummaryDashboardEntity pipelineExecutionSummaryDashboardEntity = deserialize(changeEvent);
  }

  @Override
  public void handleDeleteEvent(String id) {}

  @Override
  public void handleCreateEvent(String id, ChangeEvent<String, String> changeEvent) {
    PipelineExecutionSummaryDashboardEntity pipelineExecutionSummaryDashboardEntity = deserialize(changeEvent);
  }
}
