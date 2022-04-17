/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.persistance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.gitsync.scm.SCMGitSyncHelper;
import io.harness.gitsync.v2.GitAware;
import io.harness.pms.pipeline.PipelineEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(HarnessTeam.PL)
public class GitAwarePersistenceV2Impl extends GitAwarePersistenceNewImpl implements GitAwarePersistenceV2 {
  public GitAwarePersistenceV2Impl(MongoTemplate mongoTemplate, GitSyncSdkService gitSyncSdkService,
      Map<String, GitSdkEntityHandlerInterface> gitPersistenceHelperServiceMap, SCMGitSyncHelper scmGitSyncHelper,
      GitSyncMsvcHelper gitSyncMsvcHelper, ObjectMapper objectMapper, TransactionTemplate transactionTemplate) {
    super(mongoTemplate, gitSyncSdkService, gitPersistenceHelperServiceMap, scmGitSyncHelper, gitSyncMsvcHelper,
        objectMapper, transactionTemplate);
  }

  @Override
  public <B extends GitAware> Optional<B> findOne(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, Class<B> entityClass) {
    return super.findOne(null, "abc", "abc", entityClass);
    //        return Optional.empty();
  }
}
