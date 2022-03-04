/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.debezium;

import io.debezium.engine.ChangeEvent;

public interface ChangeHandler {
  void handleUpdateEvent(String id, ChangeEvent<String, String> changeEvent);

  void handleDeleteEvent(String id);

  void handleCreateEvent(String id, ChangeEvent<String, String> changeEvent);

  default void handleEvent(OpType opType, String id, ChangeEvent<String, String> changeEvent) {
    switch (opType) {
      case SNAPSHOT:
      case CREATE:
        handleCreateEvent(id, changeEvent);
        break;
      case UPDATE:
        handleUpdateEvent(id, changeEvent);
        break;
      case DELETE:
        handleDeleteEvent(id);
        break;
      default:
        break;
    }
  }
}
