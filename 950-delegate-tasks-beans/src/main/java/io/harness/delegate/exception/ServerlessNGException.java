/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.exception;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.DataException;

import lombok.EqualsAndHashCode;
import lombok.Value;

@OwnedBy(HarnessTeam.CDP)
@Value
@EqualsAndHashCode(callSuper = false)
public class ServerlessNGException extends DataException {
  String previousVersionTimeStamp;
  public ServerlessNGException(Throwable cause, String previousVersionTimeStamp) {
    super(cause);
    this.previousVersionTimeStamp = previousVersionTimeStamp;
  }
}
