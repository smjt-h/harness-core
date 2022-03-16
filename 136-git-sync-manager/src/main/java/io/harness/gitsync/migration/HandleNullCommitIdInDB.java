/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.migration;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.gitsync.common.beans.BranchSyncStatus.UNSYNCED;
import static io.harness.gitsync.common.beans.GitBranch.GitBranchKeys;
import static io.harness.gitsync.core.beans.GitCommit.GitCommitKeys;

import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Update.update;

import io.harness.data.structure.CollectionUtils;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.common.beans.GitBranch;
import io.harness.gitsync.common.service.GitBranchSyncService;
import io.harness.gitsync.common.service.YamlGitConfigService;
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
  private final YamlGitConfigService yamlGitConfigService;
  private final GitBranchSyncService gitBranchSyncService;

  @Override
  public void migrate() {
    log.info("Started processing the migration to handle the null commitId");
    try {
      Criteria criteria = Criteria.where(GitCommitKeys.commitId).is(null);
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
      try {
        updateTheBranchToUnSynced(repoUrl, branch);
        deleteTheGitCommitRecord(gitCommit);
        triggerTheBranchSync(gitCommit);
      } catch (Exception ex) {
        log.info("Exception while processing the repo {} and branch {}", repoUrl, branch);
      }
    }
  }

  private void triggerTheBranchSync(GitCommit gitCommit) {
    String repoUrl = gitCommit.getRepoURL();
    String branch = gitCommit.getBranchName();

    List<YamlGitConfigDTO> yamlGitConfigs =
        yamlGitConfigService.getByAccountAndRepo(gitCommit.getAccountIdentifier(), gitCommit.getRepoURL());
    if (isEmpty(yamlGitConfigs)) {
      log.info("Could not find a yamlgitconfig for the repo {}, ignoring branch sync for commit {}", repoUrl,
          gitCommit.getUuid());
    }

    YamlGitConfigDTO yamlGitConfigDTO = yamlGitConfigs.get(0);
    gitBranchSyncService.createBranchSyncEvent(yamlGitConfigDTO.getAccountIdentifier(),
        yamlGitConfigDTO.getOrganizationIdentifier(), yamlGitConfigDTO.getProjectIdentifier(),
        yamlGitConfigDTO.getIdentifier(), repoUrl, branch, null);
  }

  private void deleteTheGitCommitRecord(GitCommit gitCommit) {
    log.info("Deleting the gitCommit with the uuid {}", gitCommit.getUuid());
    Criteria criteria = Criteria.where(GitCommitKeys.uuid).is(gitCommit.getUuid());
    DeleteResult removeResult = mongoTemplate.remove(query(criteria), GitCommit.class);
    log.info("Removed {} record for the commitId {}", removeResult.getDeletedCount(), gitCommit.getUuid());
  }

  private void updateTheBranchToUnSynced(String repoUrl, String branch) {
    log.info("Deleting the branch {} in repo {}", branch, repoUrl);
    Update update = update(GitBranchKeys.branchSyncStatus, UNSYNCED);
    Criteria criteria = Criteria.where(GitBranchKeys.repoURL).is(repoUrl).and(GitBranchKeys.branchName).is(branch);
    UpdateResult updateResult = mongoTemplate.updateMulti(query(criteria), update, GitBranch.class);
    log.info("Updated {} record for the repo {} and branch {}", updateResult.getModifiedCount(), repoUrl, branch);
  }
}
