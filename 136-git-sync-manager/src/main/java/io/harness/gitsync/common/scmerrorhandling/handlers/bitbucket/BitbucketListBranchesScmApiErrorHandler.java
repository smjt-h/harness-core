package io.harness.gitsync.common.scmerrorhandling.handlers.bitbucket;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.UNEXPECTED;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.*;
import io.harness.gitsync.common.scmerrorhandling.handlers.ScmApiErrorHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PL)
public class BitbucketListBranchesScmApiErrorHandler implements ScmApiErrorHandler {
  @Override
  public void handleError(int statusCode, String errorMessage) throws WingsException {
    switch (statusCode) {
      case 401:
      case 403:
        throw NestedExceptionUtils.hintWithExplanationException(SCMExceptionHints.BITBUCKET_INVALID_CREDENTIALS,
            SCMExceptionExplanations.LIST_BRANCH_WITH_INVALID_CRED, new ScmUnauthorizedException(errorMessage));
      case 404:
        throw NestedExceptionUtils.hintWithExplanationException(SCMExceptionHints.BITBUCKET_REPO_NOT_FOUND,
            SCMExceptionExplanations.LIST_BRANCH_WHEN_REPO_NOT_EXIST, new ScmResourceNotFoundException(errorMessage));
      default:
        log.error(String.format("Error while listing bitbucket branches: [%s: %s]", statusCode, errorMessage));
        throw new ScmException(UNEXPECTED);
    }
  }
}
