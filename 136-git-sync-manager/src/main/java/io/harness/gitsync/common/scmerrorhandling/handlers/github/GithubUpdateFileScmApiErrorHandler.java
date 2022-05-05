/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.scmerrorhandling.handlers.github;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.UNEXPECTED;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.SCMExceptionExplanations;
import io.harness.exception.SCMExceptionHints;
import io.harness.exception.ScmConflictException;
import io.harness.exception.ScmException;
import io.harness.exception.ScmResourceNotFoundException;
import io.harness.exception.ScmUnauthorizedException;
import io.harness.exception.ScmUnprocessableEntityException;
import io.harness.exception.WingsException;
import io.harness.gitsync.common.scmerrorhandling.handlers.ScmApiErrorHandler;

@OwnedBy(PL)
public class GithubUpdateFileScmApiErrorHandler implements ScmApiErrorHandler {
  @Override
  public void handleError(int statusCode, String errorMessage) throws WingsException {
    switch (statusCode) {
      case 401:
      case 403:
        throw NestedExceptionUtils.hintWithExplanationException(SCMExceptionHints.GITHUB_INVALID_CREDENTIALS,
            SCMExceptionExplanations.UPDATE_FILE_WITH_INVALID_CREDS, new ScmUnauthorizedException(errorMessage));
      case 404:
        throw NestedExceptionUtils.hintWithExplanationException(SCMExceptionHints.UPDATE_FILE_NOT_FOUND_ERROR,
            SCMExceptionExplanations.UPDATE_FILE_NOT_FOUND_ERROR, new ScmResourceNotFoundException(errorMessage));
      case 409:
        throw NestedExceptionUtils.hintWithExplanationException(SCMExceptionHints.UPDATE_FILE_CONFLICT_ERROR,
            SCMExceptionExplanations.UPDATE_FILE_CONFLICT_ERROR, new ScmConflictException(errorMessage));
      case 422:
        throw NestedExceptionUtils.hintWithExplanationException(
            SCMExceptionHints.UPDATE_FILE_UNPROCESSABLE_ENTITY_ERROR,
            SCMExceptionExplanations.UPDATE_FILE_UNPROCESSABLE_ENTITY_ERROR,
            new ScmUnprocessableEntityException(errorMessage));
      default:
        throw new ScmException(UNEXPECTED);
    }
  }
}
