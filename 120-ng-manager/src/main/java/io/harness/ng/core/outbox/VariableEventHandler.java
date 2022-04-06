/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.outbox;

import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;

public class VariableEventHandler implements OutboxEventHandler {
  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    return false;
  }
}
