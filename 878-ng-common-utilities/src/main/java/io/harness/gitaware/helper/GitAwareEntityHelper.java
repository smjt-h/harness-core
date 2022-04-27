/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitaware.helper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.gitaware.dto.GitContextRequestParams;
import io.harness.gitsync.helpers.GitContextHelper;
import io.harness.gitsync.scm.SCMGitSyncHelper;
import io.harness.gitsync.scm.beans.ScmGetFileResponse;
import io.harness.persistence.gitaware.GitAware;

import com.google.inject.Inject;
import groovy.lang.Singleton;
import java.util.Map;

@OwnedBy(HarnessTeam.PL)
@Singleton
public class GitAwareEntityHelper {
  @Inject SCMGitSyncHelper scmGitSyncHelper;

  public GitAware fetchEntityFromRemote(
      GitAware entity, Scope scope, GitContextRequestParams gitContextRequestParams, Map<String, String> contextMap) {
    ScmGetFileResponse scmGetFileResponse =
        scmGitSyncHelper.getFile(Scope.builder()
                                     .accountIdentifier(scope.getAccountIdentifier())
                                     .orgIdentifier(scope.getOrgIdentifier())
                                     .projectIdentifier(scope.getProjectIdentifier())
                                     .build(),
            gitContextRequestParams.getRepoName(), gitContextRequestParams.getBranchName(),
            gitContextRequestParams.getFilePath(), gitContextRequestParams.getCommitId(),
            gitContextRequestParams.getConnectorRef(), contextMap);
    entity.setData(scmGetFileResponse.getFileContent());
    // Check if this looks good to all
    GitContextHelper.updateScmGitMetaData(scmGetFileResponse.getGitMetaData());
    return entity;
  }
}
