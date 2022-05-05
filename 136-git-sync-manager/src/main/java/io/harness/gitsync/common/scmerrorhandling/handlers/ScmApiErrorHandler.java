package io.harness.gitsync.common.scmerrorhandling.handlers;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.WingsException;

@OwnedBy(PL)
public interface ScmApiErrorHandler {
  String GITHUB = "Github";
  String BITBUCKET = "Bitbucket";
  void handleError(int statusCode, String errorMessage) throws WingsException;
}
