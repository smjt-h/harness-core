/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.helper.scmerrorhandling;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.exception.UnexpectedException;
import io.harness.gitsync.common.dtos.RepoProviders;
import io.harness.gitsync.common.dtos.ScmAPI;
import io.harness.gitsync.common.helper.RepoProviderHelper;
import io.harness.impl.ScmResponseStatusUtils;

import com.google.inject.Inject;
import java.util.Map;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(HarnessTeam.PL)
@UtilityClass
@Slf4j
public class ScmAPIErrorHandlerManager {
  @Inject private Map<Pair<ScmAPI, RepoProviders>, ScmAPIErrorHandler> scmAPIErrorHandlerMap;

  public void processAndThrowError(ScmAPI scmAPI, ConnectorType connectorType, int statusCode, String errorMessage) {
    if (statusCode < 400) {
      return;
    }

    ScmAPIErrorHandler scmAPIErrorHandler = null;
    try {
      scmAPIErrorHandler = getScmAPIErrorHandler(scmAPI, connectorType);
    } catch (Exception ex) {
      log.error("Error while getting SCM API error handler, calling default error handler", ex);
      ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(statusCode, errorMessage);
    }

    if (scmAPIErrorHandler != null) {
      scmAPIErrorHandler.handleError(statusCode, errorMessage);
    }
  }

  private ScmAPIErrorHandler getScmAPIErrorHandler(ScmAPI scmAPI, ConnectorType connectorType) {
    RepoProviders repoProvider = RepoProviderHelper.getRepoProviderFromConnectorType(connectorType);
    ScmAPIErrorHandler scmAPIErrorHandler = scmAPIErrorHandlerMap.get(Pair.of(scmAPI, repoProvider));
    if (scmAPIErrorHandler == null) {
      throw new UnexpectedException(
          String.format("No scm API handler registered for API: %s, providerType: %s, connectorType: %s", scmAPI,
              repoProvider, connectorType));
    }
    return scmAPIErrorHandler;
  }
}
