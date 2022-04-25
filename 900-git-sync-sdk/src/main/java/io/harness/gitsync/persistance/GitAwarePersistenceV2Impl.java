/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.persistance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.gitsync.helpers.GitContextHelper;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.scm.SCMGitSyncHelper;
import io.harness.gitsync.scm.beans.ScmGetFileResponse;
import io.harness.gitsync.scm.beans.ScmGitMetaData;
import io.harness.gitsync.v2.GitAware;
import io.harness.gitsync.v2.StoreType;
import io.harness.utils.RetryUtils;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

@Singleton
@OwnedBy(HarnessTeam.PL)
@Slf4j
public class GitAwarePersistenceV2Impl implements GitAwarePersistenceV2 {
  @Inject private Map<String, GitSdkEntityHandlerInterface> gitPersistenceHelperServiceMap;
  @Inject private GitAwarePersistence gitAwarePersistence;
  @Inject private MongoTemplate mongoTemplate;
  @Inject private SCMGitSyncHelper scmGitSyncHelper;
  @Inject private TransactionTemplate transactionTemplate;

  private final RetryPolicy<Object> transactionRetryPolicy = RetryUtils.getRetryPolicy("[Retrying] attempt: {}",
      "[Failed] attempt: {}", ImmutableList.of(TransactionException.class), Duration.ofSeconds(1), 3, log);

  @Override
  public <B extends GitAware> Optional<B> findOne(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, Class<B> entityClass, Criteria criteria) {
    GitContextHelper.initDefaultScmGitMetaData();

    Optional<B> savedEntityOptional =
        gitAwarePersistence.findOne(criteria, projectIdentifier, orgIdentifier, accountIdentifier, entityClass);
    if (savedEntityOptional.isPresent()) {
      GitAware savedEntity = savedEntityOptional.get();
      // if storeType != null, then it is a v2 entity and we shouldn't rely on old logic to fetch v2 entity
      if (savedEntity.getStoreType() == null) {
        GitContextHelper.updateScmGitMetaData(ScmGitMetaData.builder()
                                                  .repoName(savedEntity.getRepo())
                                                  .branchName(savedEntity.getBranch())
                                                  .blobId(savedEntity.getObjectIdOfYaml())
                                                  .filePath(savedEntity.getFilePath())
                                                  .build());
        return savedEntityOptional;
      }
    }

    Criteria gitAwareCriteria = Criteria.where(getGitSdkEntityHandlerInterface(entityClass).getStoreTypeKey())
                                    .in(Arrays.asList(StoreType.values()));
    Query query = new Query().addCriteria(new Criteria().andOperator(criteria, gitAwareCriteria));
    final B savedEntity = mongoTemplate.findOne(query, entityClass);
    if (savedEntity == null) {
      return Optional.empty();
    }

    if (savedEntity.getStoreType() == StoreType.REMOTE) {
      // fetch yaml from git
      GitEntityInfo gitEntityInfo = GitContextHelper.getGitEntityInfoV2();
      // TODO put proper context map in request
      ScmGetFileResponse scmGetFileResponse = scmGitSyncHelper.getFile(Scope.builder()
                                                                           .accountIdentifier(accountIdentifier)
                                                                           .orgIdentifier(orgIdentifier)
                                                                           .projectIdentifier(projectIdentifier)
                                                                           .build(),
          savedEntity.getRepo(), gitEntityInfo.getBranch(), savedEntity.getFilePath(), gitEntityInfo.getCommitId(),
          savedEntity.getConnectorRef(), Collections.emptyMap());
      savedEntity.setData(scmGetFileResponse.getFileContent());
      GitContextHelper.updateScmGitMetaData(scmGetFileResponse.getGitMetaData());
    }

    return Optional.of(savedEntity);
  }

  @Override
  public <B extends GitAware> B save(
      B objectToSave, String yaml, ChangeType changeType, Class<B> entityClass, Supplier functor, String branchName) {
    GitContextHelper.initDefaultScmGitMetaData();

    if (objectToSave.getStoreType() == null) {
      return gitAwarePersistence.save(objectToSave, yaml, changeType, entityClass, functor);
    }

    if (objectToSave.getStoreType() == StoreType.INLINE) {
      return saveEntity(objectToSave, functor);
    }

    // TODO put proper context map in request
    scmGitSyncHelper.commitFile(Scope.builder().build(), objectToSave.getRepo(), branchName, objectToSave.getFilePath(),
        objectToSave.getConnectorRef(), Collections.emptyMap());
    return saveEntity(objectToSave, functor);
  }

  private GitSdkEntityHandlerInterface getGitSdkEntityHandlerInterface(Class entityClass) {
    return gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName());
  }

  private <B extends GitAware> B saveEntity(B objectToSave, Supplier functor) {
    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      final B mongoSavedObject = mongoTemplate.save(objectToSave);
      if (functor != null) {
        functor.get();
      }
      return mongoSavedObject;
    }));
  }
}
