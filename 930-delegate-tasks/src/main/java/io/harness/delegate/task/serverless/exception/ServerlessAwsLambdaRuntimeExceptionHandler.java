/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless.exception;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;
import io.harness.exception.runtime.serverless.ServerlessAwsLambdaRuntimeException;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import java.util.Set;

@OwnedBy(HarnessTeam.CDP)
@Singleton
public class ServerlessAwsLambdaRuntimeExceptionHandler implements ExceptionHandler {
  public static Set<Class<? extends Exception>> exceptions() {
    return ImmutableSet.<Class<? extends Exception>>builder().add(ServerlessAwsLambdaRuntimeException.class).build();
  }

  @Override
  public WingsException handleException(Exception exception) {
    return null;
  }
}
