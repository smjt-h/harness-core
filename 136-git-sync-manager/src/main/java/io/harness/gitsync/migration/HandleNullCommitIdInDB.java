package io.harness.gitsync.migration;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.gitsync.common.beans.BranchSyncStatus.UNSYNCED;
import static io.harness.gitsync.common.beans.GitBranch.GitBranchKeys.branchName;
import static io.harness.gitsync.common.beans.GitBranch.GitBranchKeys.branchSyncStatus;
import static io.harness.gitsync.common.beans.GitBranch.GitBranchKeys.repoURL;

import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Update.update;

import io.harness.data.structure.CollectionUtils;
import io.harness.gitsync.common.beans.GitBranch;
import io.harness.gitsync.core.beans.GitCommit;
import io.harness.migration.NGMigration;

import com.google.inject.Inject;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class HandleNullCommitIdInDB implements NGMigration {
  private final MongoTemplate mongoTemplate;

  @Override
  public void migrate() {
    log.info("Started processing the migration to handle the null commitId");
    try {
      Criteria criteria = Criteria.where(GitCommit.GitCommitKeys.commitId).is(null);
      List<GitCommit> gitCommitList = mongoTemplate.find(query(criteria), GitCommit.class);
      log.info("The size of the git commits to be processed is {}", CollectionUtils.emptyIfNull(gitCommitList));
      saveTheBranchAsUnsyncedAndDeleteTheCommit(gitCommitList);
    } catch (Exception exception) {
      log.error("Error occurred while handling the null commitId");
    }
    log.info("Completed processing the migration to handle the null commitId");
  }

  private void saveTheBranchAsUnsyncedAndDeleteTheCommit(List<GitCommit> gitCommitList) {
    if (isEmpty(gitCommitList)) {
      log.info("No commit Ids to be deleted");
    }
    for (GitCommit gitCommit : gitCommitList) {
      String repoUrl = gitCommit.getRepoURL();
      String branch = gitCommit.getBranchName();

      updateTheBranchToUnSynced(repoUrl, branch);
      deleteTheGitCommitRecord(gitCommit);
    }
  }

  private void deleteTheGitCommitRecord(GitCommit gitCommit) {
    log.info("Deleting the gitCommit with the uuid {}", gitCommit.getUuid());
    try {
      Criteria criteria = Criteria.where(GitCommit.GitCommitKeys.uuid).is(gitCommit.getUuid());
      DeleteResult removeResult = mongoTemplate.remove(query(criteria), GitCommit.class);
      log.info("Removed {} record for the commitId {}", removeResult.getDeletedCount(), gitCommit.getUuid());
    } catch (Exception ex) {
      log.info("Exception while deleting the commitId with the uuid {}", gitCommit.getUuid(), ex);
    }
  }

  private void updateTheBranchToUnSynced(String repoUrl, String branch) {
    log.info("Deleting the branch {} in repo {}", branch, repoUrl);
    try {
      Update update = update(branchSyncStatus, UNSYNCED);
      Criteria criteria = Criteria.where(repoURL).is(repoUrl).and(branchName).is(branch);
      UpdateResult updateResult = mongoTemplate.updateMulti(query(criteria), update, GitBranch.class);
      log.info("Updated {} record for the repo {} and branch {}", updateResult.getModifiedCount(), repoUrl, branch);
    } catch (Exception ex) {
      log.info("Exception while updating the record for the repo {} and branch {}", repoUrl, branch, ex);
    }
  }
}
