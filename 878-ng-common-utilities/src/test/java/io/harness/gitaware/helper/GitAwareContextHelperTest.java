package io.harness.gitaware.helper;

import static io.harness.rule.OwnerRule.MOHIT_GARG;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.UnexpectedException;
import io.harness.gitsync.helpers.GitContextHelper;
import io.harness.gitsync.scm.beans.ScmGitMetaData;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GitAwareContextHelperTest extends CategoryTest {
  private static final String CommitId = "commitId";
  private static final String FilePath = "filePath";
  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testScmGitMetaDataUpdate() {
    ScmGitMetaData scmGitMetaData = ScmGitMetaData.builder().commitId(CommitId).filePath(FilePath).build();
    GitContextHelper.updateScmGitMetaData(scmGitMetaData);
    ScmGitMetaData scmGitMetaDataFetched = GitAwareContextHelper.getScmGitMetaData();
    assertThat(scmGitMetaDataFetched).isNotNull();
    assertThat(scmGitMetaDataFetched.getFilePath()).isEqualTo(FilePath);
    assertThat(scmGitMetaDataFetched.getCommitId()).isEqualTo(CommitId);
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testScmGitMetaDataNotFound() {
    assertThatThrownBy(GitAwareContextHelper::getScmGitMetaData).isInstanceOf(UnexpectedException.class);
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testInitScmGitMetaData() {
    GitAwareContextHelper.initDefaultScmGitMetaData();
    ScmGitMetaData scmGitMetaDataFetched = GitAwareContextHelper.getScmGitMetaData();
    assertThat(scmGitMetaDataFetched).isNotNull();
    assertThat(scmGitMetaDataFetched.getFilePath()).isEqualTo(null);
    assertThat(scmGitMetaDataFetched.getCommitId()).isEqualTo(null);
    assertThat(scmGitMetaDataFetched.getBlobId()).isEqualTo(null);
    assertThat(scmGitMetaDataFetched.getBranchName()).isEqualTo(null);
    assertThat(scmGitMetaDataFetched.getRepoName()).isEqualTo(null);
  }
}