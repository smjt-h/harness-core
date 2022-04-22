/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.helpers;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.interceptor.GitSyncConstants.DEFAULT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContext;
import io.harness.exception.UnexpectedException;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.gitsync.scm.beans.ScmGitMetaData;
import io.harness.gitsync.scm.beans.ScmGitMetaDataContext;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.manage.GlobalContextManager;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(DX)
public class GitContextHelper {
  public GitEntityInfo getGitEntityInfo() {
    final GitSyncBranchContext gitSyncBranchContext =
        GlobalContextManager.get(GitSyncBranchContext.NG_GIT_SYNC_CONTEXT);
    if (gitSyncBranchContext == null) {
      return null;
    }
    GitEntityInfo gitBranchInfo = gitSyncBranchContext.getGitBranchInfo();
    if (gitBranchInfo == null || gitBranchInfo.getYamlGitConfigId() == null || gitBranchInfo.getBranch() == null
        || gitBranchInfo.getYamlGitConfigId().equals(DEFAULT) || gitBranchInfo.getBranch().equals(DEFAULT)) {
      return null;
    }
    return gitBranchInfo;
  }

  public GitEntityInfo getGitEntityInfoV2() {
    final GitSyncBranchContext gitSyncBranchContext =
        GlobalContextManager.get(GitSyncBranchContext.NG_GIT_SYNC_CONTEXT);
    if (gitSyncBranchContext == null) {
      throw new UnexpectedException("Git Details not found in context");
    }
    return gitSyncBranchContext.getGitBranchInfo();
  }

  public boolean isUpdateToNewBranch() {
    GitEntityInfo gitEntityInfo = getGitEntityInfo();
    if (gitEntityInfo == null) {
      return false;
    }
    return gitEntityInfo.isNewBranch();
  }

  public static boolean isFullSyncFlow() {
    GitEntityInfo gitEntityInfo = getGitEntityInfo();
    if (gitEntityInfo == null) {
      return false;
    }
    return Boolean.TRUE.equals(gitEntityInfo.getIsFullSyncFlow());
  }

  public String getBranchForRefEntityValidations() {
    GitEntityInfo gitEntityInfo = getGitEntityInfo();
    if (gitEntityInfo.isNewBranch()) {
      return gitEntityInfo.getBaseBranch();
    }
    return gitEntityInfo.getBranch();
  }

  public void updateScmGitMetaData(ScmGitMetaData scmGitMetaData) {
    if (!GlobalContextManager.isAvailable()) {
      GlobalContextManager.set(new GlobalContext());
    }
    GlobalContextManager.upsertGlobalContextRecord(
        ScmGitMetaDataContext.builder().scmGitMetaData(scmGitMetaData).build());
  }

  public ScmGitMetaData getScmGitMetaData() {
    ScmGitMetaDataContext gitMetaDataContext = GlobalContextManager.get(ScmGitMetaDataContext.NG_GIT_SYNC_CONTEXT);
    if (gitMetaDataContext == null) {
      throw new UnexpectedException("No SCM Git Metadata found in context");
    }
    return gitMetaDataContext.getScmGitMetaData();
  }

  public EntityGitDetails getEntityGitDetailsFromScmGitMetadata() {
    ScmGitMetaData scmGitMetaData = getScmGitMetaData();
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
