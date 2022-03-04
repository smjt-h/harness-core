/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.debezium;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;
import io.debezium.embedded.EmbeddedEngineChangeEvent;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.serde.DebeziumSerdes;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.connect.source.SourceRecord;

@Singleton
@Slf4j
public class DebeziumChangeConsumer implements DebeziumEngine.ChangeConsumer<ChangeEvent<String, String>> {
  ChangeHandler changeHandler;
  private static final String OP_FIELD = "__op";
  public DebeziumChangeConsumer(ChangeHandler changeHandler) {
    this.changeHandler = changeHandler;
  }

  private boolean handleEvent(ChangeEvent<String, String> changeEvent) {
    // configuring id deserializer
    Serde<String> idSerde = DebeziumSerdes.payloadJson(String.class);
    idSerde.configure(Maps.newHashMap(ImmutableMap.of("from.field", "id")), true);
    Deserializer<String> idDeserializer = idSerde.deserializer();
    String id = idDeserializer.deserialize(null, changeEvent.key().getBytes());
    Optional<OpType> opType =
        getOperationType(((EmbeddedEngineChangeEvent<String, String>) changeEvent).sourceRecord());
    changeHandler.handleEvent(opType.get(), id, changeEvent);
    return true;
  }

  @Override
  public void handleBatch(List<ChangeEvent<String, String>> changeEvents,
      DebeziumEngine.RecordCommitter<ChangeEvent<String, String>> recordCommitter) throws InterruptedException {
    for (ChangeEvent<String, String> changeEvent : changeEvents) {
      try {
        handleEvent(changeEvent);
      } catch (Exception exception) {
        // TODO: Handle Failure
        log.error(String.format("Exception caught when trying to process event: [%s].", changeEvent), exception);
      }
      recordCommitter.markProcessed(changeEvent);
    }
    recordCommitter.markBatchFinished();
  }

  private Optional<OpType> getOperationType(SourceRecord sourceRecord) {
    return Optional.ofNullable(sourceRecord.headers().lastWithName(OP_FIELD))
        .flatMap(x -> OpType.fromString((String) x.value()));
  }
}
