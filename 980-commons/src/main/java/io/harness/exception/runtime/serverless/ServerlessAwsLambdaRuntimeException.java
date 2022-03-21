/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception.runtime.serverless;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@OwnedBy(HarnessTeam.CDP)
@EqualsAndHashCode(callSuper = false)
public class ServerlessAwsLambdaRuntimeException extends RuntimeException {
  private final String message;
  private final Throwable cause;

  public ServerlessAwsLambdaRuntimeException(String message) {
    super(message);
    this.message = message;
    this.cause = null;
  }

  public ServerlessAwsLambdaRuntimeException(String message, Throwable cause) {
    super(message, cause);
    this.message = message;
    this.cause = cause;
  }
}
