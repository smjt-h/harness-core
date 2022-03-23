/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.service.custom;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.ng.core.events.ServiceCreateEvent;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.mappers.ServiceFilterHelper;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxService;

import com.google.inject.Inject;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(CDC)
public class ServiceRepositoryCustomImpl implements ServiceV2RepositoryCustom {
  private final GitAwarePersistence gitAwarePersistence;
  private final GitSyncSdkService gitSyncSdkService;
  private final MongoTemplate mongoTemplate;
  OutboxService outboxService;
  @Override
  public ServiceEntity save(ServiceEntity serviceEntity) {
    Supplier<OutboxEvent> supplier = null;
    if (shouldLogAudits(
            serviceEntity.getAccountId(), serviceEntity.getOrgIdentifier(), serviceEntity.getProjectIdentifier())) {
      supplier = ()
          -> outboxService.save(new ServiceCreateEvent(serviceEntity.getAccountIdentifier(),
              serviceEntity.getOrgIdentifier(), serviceEntity.getProjectIdentifier(), serviceEntity));
    }
    return gitAwarePersistence.save(
        serviceEntity, serviceEntity.getYaml(), ChangeType.ADD, ServiceEntity.class, supplier);
  }

  @Override
  public Optional<ServiceEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier, boolean notDeleted) {
    return gitAwarePersistence.findOne(Criteria.where(ServiceEntity.ServiceEntityKeys.deleted)
                                           .is(!notDeleted)
                                           .and(ServiceEntity.ServiceEntityKeys.identifier)
                                           .is(serviceIdentifier)
                                           .and(ServiceEntity.ServiceEntityKeys.projectIdentifier)
                                           .is(projectIdentifier)
                                           .and(ServiceEntity.ServiceEntityKeys.orgIdentifier)
                                           .is(orgIdentifier)
                                           .and(ServiceEntity.ServiceEntityKeys.accountId)
                                           .is(accountId),
        projectIdentifier, orgIdentifier, accountId, ServiceEntity.class);
  }

  @Override
  public ServiceEntity update(ServiceEntity serviceEntity, Criteria criteria) {
    criteria = gitAwarePersistence.makeCriteriaGitAware(serviceEntity.getAccountId(), serviceEntity.getOrgIdentifier(),
        serviceEntity.getProjectIdentifier(), ServiceEntity.class, criteria);
    Update update = ServiceFilterHelper.getUpdateOperations(serviceEntity);
    return mongoTemplate.findAndModify(
        query(criteria), update, FindAndModifyOptions.options().returnNew(true), ServiceEntity.class);
  }

  boolean shouldLogAudits(String accountId, String orgIdentifier, String projectIdentifier) {
    // if git sync is disabled or if git sync is enabled (only for default branch)
    return !gitSyncSdkService.isGitSyncEnabled(accountId, orgIdentifier, projectIdentifier);
  }
}
