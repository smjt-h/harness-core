/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.scm.errorhandling;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ScmInternalServerErrorException;
import io.harness.exception.ScmResourceNotFoundException;
import io.harness.exception.ScmUnexpectedException;
import io.harness.gitsync.scm.beans.ScmErrorDetails;

import groovy.lang.Singleton;

@OwnedBy(HarnessTeam.PL)
@Singleton
public class GetFileScmErrorHandler extends ScmErrorHandler {
  @Override
  void handleError(int statusCode, ScmErrorDetails errorDetails) {
    switch (statusCode) {
      case 400:
        throw new ScmResourceNotFoundException(errorDetails.getErrorMessage());
      case 500:
        throw new ScmInternalServerErrorException(errorDetails.getErrorMessage());
      default:
        throw new ScmUnexpectedException(errorDetails.getErrorMessage());
    }
  }
}
