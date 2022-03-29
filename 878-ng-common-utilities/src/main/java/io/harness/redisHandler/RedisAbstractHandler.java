/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.redisHandler;

import static io.serializer.HObjectMapper.NG_DEFAULT_OBJECT_MAPPER;

import io.harness.debezium.DebeziumChangeEvent;
import io.harness.pms.sdk.execution.events.PmsCommonsBaseEventHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.SneakyThrows;

public abstract class RedisAbstractHandler implements PmsCommonsBaseEventHandler<DebeziumChangeEvent> {
  public final ObjectMapper objectMapper = NG_DEFAULT_OBJECT_MAPPER;

  @SneakyThrows
  String getId(String key) {
    JsonNode node = objectMapper.readTree(key);
    return node.get("id").asText();
  }
  @SneakyThrows
  @Override
  public void handleEvent(DebeziumChangeEvent event, Map<String, String> metadataMap, long timestamp) {
    String optype = event.getOptype();
    String id = getId(event.getKey());
    String value = event.getValue();
    switch (optype) {
      case "SNAPSHOT":
      case "CREATE":
        handleCreateEvent(id, value);
        break;
      case "UPDATE":
        handleUpdateEvent(id, value);
        break;
      case "DELETE":
        handleDeleteEvent(id);
        break;
      default:
        break;
    }
  }

  public abstract void handleCreateEvent(String id, String value);
  public abstract void handleDeleteEvent(String id);
  public abstract void handleUpdateEvent(String id, String value);
}
