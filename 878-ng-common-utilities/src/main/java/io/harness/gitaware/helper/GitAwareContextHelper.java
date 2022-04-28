package io.harness.gitaware.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContext;
import io.harness.exception.UnexpectedException;
import io.harness.gitsync.helpers.GitContextHelper;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.scm.beans.ScmGitMetaData;
import io.harness.gitsync.scm.beans.ScmGitMetaDataContext;
import io.harness.manage.GlobalContextManager;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
public class GitAwareContextHelper {
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

  public boolean isOldFlow() {
    GitEntityInfo gitEntityInfo = GitContextHelper.getGitEntityInfo();
    return gitEntityInfo == null || gitEntityInfo.getStoreType() == null;
  }
}
