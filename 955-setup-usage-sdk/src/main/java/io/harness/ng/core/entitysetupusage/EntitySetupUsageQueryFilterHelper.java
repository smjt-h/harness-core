/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.entitysetupusage;

import static io.harness.annotations.dev.HarnessTeam.DX;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.context.GlobalContextData;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.manage.GlobalContextManager;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage.EntitySetupUsageKeys;

import com.google.inject.Singleton;
import java.util.List;
import java.util.Objects;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@OwnedBy(DX)
public class EntitySetupUsageQueryFilterHelper {
  public Criteria createCriteriaFromEntityFilter(
      String accountIdentifier, String referredEntityFQN, EntityType referredEntityType, String searchTerm) {
    Criteria criteria = new Criteria();
    criteria.and(EntitySetupUsageKeys.accountIdentifier).is(accountIdentifier);
    criteria.and(EntitySetupUsageKeys.referredEntityFQN).is(referredEntityFQN);
    if (referredEntityType != null) {
      criteria.and(EntitySetupUsageKeys.referredEntityType).is(referredEntityType.getYamlName());
    }
    if (isNotBlank(searchTerm)) {
      criteria.orOperator(Criteria.where(EntitySetupUsageKeys.referredEntityName).regex(searchTerm),
          Criteria.where(EntitySetupUsageKeys.referredByEntityName).regex(searchTerm));
    }
    populateGitCriteriaForReferredEntity(criteria);
    return criteria;
  }

  public Criteria createCriteriaFromEntityFilter(String accountIdentifier, String referredEntityFQN,
      EntityType referredEntityType, EntityType referredByEntityType, String searchTerm) {
    Criteria criteria = new Criteria();
    criteria.and(EntitySetupUsageKeys.accountIdentifier).is(accountIdentifier);
    criteria.and(EntitySetupUsageKeys.referredEntityFQN).is(referredEntityFQN);
    if (referredEntityType != null) {
      criteria.and(EntitySetupUsageKeys.referredEntityType).is(referredEntityType.getYamlName());
    }
    if (referredByEntityType != null) {
      criteria.and(EntitySetupUsageKeys.referredByEntityType).is(referredByEntityType.getYamlName());
    }
    if (isNotBlank(searchTerm)) {
      criteria.orOperator(Criteria.where(EntitySetupUsageKeys.referredEntityName).regex(searchTerm),
          Criteria.where(EntitySetupUsageKeys.referredByEntityName).regex(searchTerm));
    }
    populateGitCriteriaForReferredEntity(criteria);
    return criteria;
  }

  private Criteria createCriteriaForDefaultReferredEntity() {
    return new Criteria().orOperator(Criteria.where(EntitySetupUsageKeys.referredEntityIsDefault).is(true),
        Criteria.where(EntitySetupUsageKeys.referredEntityIsDefault).exists(false));
  }

  private Criteria createCriteriaForDefaultReferredByEntity() {
    return new Criteria().orOperator(Criteria.where(EntitySetupUsageKeys.referredByEntityIsDefault).is(true),
        Criteria.where(EntitySetupUsageKeys.referredByEntityIsDefault).exists(false));
  }

  private boolean gitContextIsPresent(EntityGitDetails entityGitDetails) {
    if (entityGitDetails == null
        || entityGitDetails.getBranch() == null && entityGitDetails.getRepoIdentifier() == null) {
      return false;
    }
    return true;
  }

  public Criteria createCriteriaForListAllReferredUsages(
      String accountIdentifier, String referredByEntityFQN, EntityType referredEntityType, String searchTerm) {
    Criteria criteria = new Criteria();
    criteria.and(EntitySetupUsageKeys.accountIdentifier).is(accountIdentifier);
    criteria.and(EntitySetupUsageKeys.referredByEntityFQN).is(referredByEntityFQN);
    if (referredEntityType != null) {
      criteria.and(EntitySetupUsageKeys.referredEntityType).is(referredEntityType.getYamlName());
    }
    if (isNotBlank(searchTerm)) {
      criteria.orOperator(Criteria.where(EntitySetupUsageKeys.referredEntityName).regex(searchTerm),
          Criteria.where(EntitySetupUsageKeys.referredByEntityName).regex(searchTerm));
    }
    populateGitCriteriaForReferredByEntity(criteria);
    return criteria;
  }

  public Criteria createCriteriaForListAllReferredUsagesBatch(String accountIdentifier,
      List<String> referredByEntityFQNList, EntityType referredByEntityType, EntityType referredEntityType) {
    Criteria criteria = Criteria.where(EntitySetupUsageKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(EntitySetupUsageKeys.referredByEntityFQN)
                            .in(referredByEntityFQNList)
                            .and(EntitySetupUsageKeys.referredByEntityType)
                            .is(referredByEntityType.getYamlName())
                            .and(EntitySetupUsageKeys.referredEntityType)
                            .is(referredEntityType.getYamlName());
    Criteria criteriaToGetDefaultEntity = createCriteriaForDefaultReferredByEntity();
    criteria.andOperator(criteriaToGetDefaultEntity);
    return criteria;
  }

  public Criteria createCriteriaToCheckWhetherThisEntityIsReferred(
      String accountIdentifier, String referredEntityFQN, EntityType referredEntityType) {
    Criteria criteria = new Criteria();
    criteria.and(EntitySetupUsageKeys.accountIdentifier).is(accountIdentifier);
    criteria.and(EntitySetupUsageKeys.referredEntityFQN).is(referredEntityFQN);
    if (referredEntityType != null) {
      criteria.and(EntitySetupUsageKeys.referredEntityType).is(referredEntityType.getYamlName());
    }
    populateGitCriteriaForReferredEntity(criteria);
    return criteria;
  }

  private void populateGitCriteriaForReferredEntity(Criteria criteria) {
    EntityGitDetails entityGitDetails = getGitDetailsFromThreadContext();
    if (gitContextIsPresent(entityGitDetails)) {
      criteria.and(EntitySetupUsageKeys.referredEntityRepoIdentifier).is(entityGitDetails.getRepoIdentifier());
      criteria.and(EntitySetupUsageKeys.referredEntityBranch).is(entityGitDetails.getBranch());
    } else {
      Criteria criteriaToGetDefaultEntity = createCriteriaForDefaultReferredEntity();
      criteria.andOperator(criteriaToGetDefaultEntity);
    }
  }

  private EntityGitDetails getGitDetailsFromThreadContext() {
    GlobalContextData globalContextData = GlobalContextManager.get(GitSyncBranchContext.NG_GIT_SYNC_CONTEXT);
    if (globalContextData == null) {
      return null;
    }
    return ((GitSyncBranchContext) Objects.requireNonNull(globalContextData)).toEntityGitDetails();
  }

  private void populateGitCriteriaForReferredByEntity(Criteria criteria) {
    EntityGitDetails entityGitDetails = getGitDetailsFromThreadContext();
    if (gitContextIsPresent(entityGitDetails)) {
      criteria.and(EntitySetupUsageKeys.referredByEntityRepoIdentifier).is(entityGitDetails.getRepoIdentifier());
      criteria.and(EntitySetupUsageKeys.referredByEntityBranch).is(entityGitDetails.getBranch());
    } else {
      Criteria criteriaToGetDefaultEntity = createCriteriaForDefaultReferredByEntity();
      criteria.andOperator(criteriaToGetDefaultEntity);
    }
  }

  public Criteria createCriteriaForDeletingAllReferredByEntries(String accountIdentifier, String referredByEntityFQN,
      EntityType referredByEntityType, EntityType referredEntityType) {
    Criteria criteria = new Criteria();
    criteria.and(EntitySetupUsageKeys.accountIdentifier).is(accountIdentifier);
    criteria.and(EntitySetupUsageKeys.referredByEntityFQN).is(referredByEntityFQN);
    if (referredEntityType != null) {
      criteria.and(EntitySetupUsageKeys.referredEntityType).is(referredEntityType.getYamlName());
    }
    if (referredByEntityType != null) {
      criteria.and(EntitySetupUsageKeys.referredByEntityType).is(referredByEntityType.getYamlName());
    }
    populateGitCriteriaForReferredByEntity(criteria);
    return criteria;
  }
}
