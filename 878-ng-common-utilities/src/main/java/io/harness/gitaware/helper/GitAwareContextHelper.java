/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitaware.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContext;
import io.harness.exception.UnexpectedException;
import io.harness.gitsync.helpers.GitContextHelper;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.gitsync.scm.beans.ScmGitMetaData;
import io.harness.gitsync.scm.beans.ScmGitMetaDataContext;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.manage.GlobalContextManager;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
public class GitAwareContextHelper {
  public GitEntityInfo getGitEntityInfo() {
    final GitSyncBranchContext gitSyncBranchContext =
        GlobalContextManager.get(GitSyncBranchContext.NG_GIT_SYNC_CONTEXT);
    if (gitSyncBranchContext == null) {
      throw new UnexpectedException("Git Details not found in context");
    }
    return gitSyncBranchContext.getGitBranchInfo();
  }

  public void initDefaultScmGitMetaData() {
    if (!GlobalContextManager.isAvailable()) {
      GlobalContextManager.set(new GlobalContext());
    }
    GlobalContextManager.upsertGlobalContextRecord(
        ScmGitMetaDataContext.builder().scmGitMetaData(ScmGitMetaData.builder().build()).build());
  }

  public ScmGitMetaData getScmGitMetaData() {
    ScmGitMetaDataContext gitMetaDataContext = GlobalContextManager.get(ScmGitMetaDataContext.NG_GIT_SYNC_CONTEXT);
    if (gitMetaDataContext == null) {
      throw new UnexpectedException("No SCM Git Metadata found in context");
    }
    return gitMetaDataContext.getScmGitMetaData();
  }

  public void updateScmGitMetaData(ScmGitMetaData scmGitMetaData) {
    if (!GlobalContextManager.isAvailable()) {
      GlobalContextManager.set(new GlobalContext());
    }
    GlobalContextManager.upsertGlobalContextRecord(
        ScmGitMetaDataContext.builder().scmGitMetaData(scmGitMetaData).build());
  }

  public boolean isOldFlow() {
    GitEntityInfo gitEntityInfo = GitContextHelper.getGitEntityInfo();
    return gitEntityInfo == null || gitEntityInfo.getStoreType() == null;
  }

  public EntityGitDetails getEntityGitDetailsFromScmGitMetadata() {
    ScmGitMetaData scmGitMetaData = getScmGitMetaData();
    if (scmGitMetaData == null) {
      return EntityGitDetails.builder().build();
    }
    return EntityGitDetails.builder()
        .objectId(scmGitMetaData.getBlobId())
        .branch(scmGitMetaData.getBranchName())
        .repoIdentifier(scmGitMetaData.getRepoName())
        .repoName(scmGitMetaData.getRepoName())
        .filePath(scmGitMetaData.getFilePath())
        .commitId(scmGitMetaData.getCommitId())
        .build();
  }
}
