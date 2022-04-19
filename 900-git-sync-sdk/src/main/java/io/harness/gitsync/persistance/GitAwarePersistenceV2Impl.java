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
import io.harness.gitsync.v2.StoreType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@Singleton
@OwnedBy(HarnessTeam.PL)
public class GitAwarePersistenceV2Impl implements GitAwarePersistenceV2 {
  private Map<String, GitSdkEntityHandlerInterface> gitPersistenceHelperServiceMap;

  @Inject GitAwarePersistence gitAwarePersistence;
  @Inject MongoTemplate mongoTemplate;
  @Inject SCMGitSyncHelper scmGitSyncHelper;

  @Override
  public Optional<GitAware> findOne(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, Class entityClass, Criteria criteria) {
    Optional<GitAware> savedEntity =
        gitAwarePersistence.findOne(criteria, projectIdentifier, orgIdentifier, accountIdentifier, entityClass);
    if (savedEntity.isPresent()) {
      return savedEntity;
    }

    Criteria gitAwareCriteria = Criteria.where(getGitSdkEntityHandlerInterface(entityClass).getStoreTypeKey())
                                    .in(EnumUtils.getEnumList(StoreType.class));

    Query query = new Query().addCriteria(new Criteria().andOperator(criteria, gitAwareCriteria));
    final GitAware savedObject = (GitAware) mongoTemplate.findOne(query, entityClass);
    if (savedObject == null) {
      // Check with @Naman if I should directly throw ObjectNotFound Exception here which will result into 404
      // We have to check current behaviour in such cases and try to maintain it otherwise it will break
      // API contract for the current consumers of the API
      return Optional.empty();
    }

    if (savedObject.getStoreType() == StoreType.REMOTE) {
      // fetch yaml from git
      scmGitSyncHelper.getFile();
    }
  }

  private GitSdkEntityHandlerInterface getGitSdkEntityHandlerInterface(Class entityClass) {
    return gitPersistenceHelperServiceMap.get(entityClass.getCanonicalName());
  }
}
