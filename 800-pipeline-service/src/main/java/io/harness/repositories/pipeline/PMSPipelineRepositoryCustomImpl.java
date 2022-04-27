/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.pipeline;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.beans.FeatureName.GIT_SIMPLIFICATION;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.ChangeType;
import io.harness.gitaware.dto.GitContextRequestParams;
import io.harness.gitaware.helper.GitAwareEntityHelper;
import io.harness.gitsync.beans.StoreType;
import io.harness.gitsync.common.helper.EntityDistinctElementHelper;
import io.harness.gitsync.helpers.GitContextHelper;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.gitsync.persistance.GitSyncableHarnessRepo;
import io.harness.gitsync.scm.beans.ScmGitMetaData;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxService;
import io.harness.pms.PmsFeatureFlagService;
import io.harness.pms.events.PipelineCreateEvent;
import io.harness.pms.events.PipelineDeleteEvent;
import io.harness.pms.events.PipelineUpdateEvent;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.PipelineMetadataV2;
import io.harness.pms.pipeline.mappers.PMSPipelineFilterHelper;
import io.harness.pms.pipeline.service.PipelineMetadataService;
import io.harness.springdata.TransactionHelper;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

@GitSyncableHarnessRepo
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class PMSPipelineRepositoryCustomImpl implements PMSPipelineRepositoryCustom {
  private final MongoTemplate mongoTemplate;
  private final GitAwarePersistence gitAwarePersistence;
  private final GitSyncSdkService gitSyncSdkService;
  private final TransactionHelper transactionHelper;
  private final PipelineMetadataService pipelineMetadataService;
  private final PmsFeatureFlagService pmsFeatureFlagService;
  private final GitAwareEntityHelper gitAwareEntityHelper;
  private final OutboxService outboxService;

  @Override
  public Page<PipelineEntity> findAll(Criteria criteria, Pageable pageable, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, boolean getDistinctFromBranches) {
    if (getDistinctFromBranches) {
      return EntityDistinctElementHelper.getDistinctElementPage(mongoTemplate, criteria, pageable, PipelineEntity.class,
          PipelineEntityKeys.accountId, PipelineEntityKeys.orgIdentifier, PipelineEntityKeys.projectIdentifier,
          PipelineEntityKeys.identifier);
    }
    List<PipelineEntity> pipelineEntities = gitAwarePersistence.find(
        criteria, pageable, projectIdentifier, orgIdentifier, accountIdentifier, PipelineEntity.class);
    return PageableExecutionUtils.getPage(pipelineEntities, pageable,
        ()
            -> gitAwarePersistence.count(
                criteria, projectIdentifier, orgIdentifier, accountIdentifier, PipelineEntity.class));
  }

  @Override
  public Long countAllPipelines(Criteria criteria) {
    Query query = new Query().addCriteria(criteria);
    return mongoTemplate.count(query, PipelineEntity.class);
  }

  @Override
  public PipelineEntity save(PipelineEntity pipelineToSave) {
    String accountIdentifier = pipelineToSave.getAccountIdentifier();
    String orgIdentifier = pipelineToSave.getOrgIdentifier();
    String projectIdentifier = pipelineToSave.getProjectIdentifier();
    Supplier<OutboxEvent> supplier = ()
        -> outboxService.save(
            new PipelineCreateEvent(accountIdentifier, orgIdentifier, projectIdentifier, pipelineToSave));
    Supplier<OutboxEvent> supplierWithGitSyncTrue = ()
        -> outboxService.save(
            new PipelineCreateEvent(accountIdentifier, orgIdentifier, projectIdentifier, pipelineToSave, true));
    return transactionHelper.performTransaction(() -> {
      PipelineEntity savedEntity = savePipelineEntity(pipelineToSave, supplier);
      if (savedEntity.getStoreType() == null) {
        makeSupplierCallIfGitSyncIsEnabled(
            accountIdentifier, orgIdentifier, projectIdentifier, supplierWithGitSyncTrue);
      }
      checkForMetadataAndSaveIfAbsent(savedEntity);
      return savedEntity;
    });
  }

  private PipelineEntity savePipelineEntity(PipelineEntity pipelineToSave, Supplier<OutboxEvent> supplier) {
    if (GitContextHelper.isOldFlow()) {
      return gitAwarePersistence.save(
          pipelineToSave, pipelineToSave.getYaml(), ChangeType.ADD, PipelineEntity.class, supplier);
    }
    GitContextHelper.initDefaultScmGitMetaData();
    GitEntityInfo gitEntityInfo = GitContextHelper.getGitEntityInfo();
    if (gitEntityInfo == null || gitEntityInfo.getStoreType().equals(StoreType.INLINE)) {
      pipelineToSave.setStoreType(StoreType.INLINE);
      PipelineEntity savedPipelineEntity = mongoTemplate.save(pipelineToSave);
      if (supplier != null) {
        supplier.get();
      }
      return savedPipelineEntity;
    }
    String yamlToPush = pipelineToSave.getYaml();
    // push yamlToPush to git and create PR if needed
    pipelineToSave.setYaml("");
    pipelineToSave.setStoreType(StoreType.REMOTE);
    pipelineToSave.setConnectorRef(gitEntityInfo.getConnectorRef());
    pipelineToSave.setRepo(gitEntityInfo.getYamlGitConfigId());
    pipelineToSave.setFilePath(gitEntityInfo.getFilePath());

    return mongoTemplate.save(pipelineToSave);
  }

  private void checkForMetadataAndSaveIfAbsent(PipelineEntity savedEntity) {
    // checking if PipelineMetadata exists or not, if exists don't re-save the entity, as only one entry across git
    // repos should be there.
    Optional<PipelineMetadataV2> metadataOptional =
        pipelineMetadataService.getMetadata(savedEntity.getAccountIdentifier(), savedEntity.getOrgIdentifier(),
            savedEntity.getProjectIdentifier(), savedEntity.getIdentifier());
    if (!metadataOptional.isPresent()) {
      PipelineMetadataV2 metadata = PipelineMetadataV2.builder()
                                        .accountIdentifier(savedEntity.getAccountIdentifier())
                                        .orgIdentifier(savedEntity.getOrgIdentifier())
                                        .projectIdentifier(savedEntity.getProjectIdentifier())
                                        .runSequence(0)
                                        .identifier(savedEntity.getIdentifier())
                                        .build();
      pipelineMetadataService.save(metadata);
    }
  }

  @Override
  public Optional<PipelineEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, boolean notDeleted) {
    Criteria criteria = Criteria.where(PipelineEntityKeys.deleted)
                            .is(!notDeleted)
                            .and(PipelineEntityKeys.identifier)
                            .is(pipelineIdentifier)
                            .and(PipelineEntityKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(PipelineEntityKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(PipelineEntityKeys.accountId)
                            .is(accountId);
    Optional<PipelineEntity> optionalPipelineEntity =
        gitAwarePersistence.findOne(criteria, projectIdentifier, orgIdentifier, accountId, PipelineEntity.class);

    return findV2(accountId, orgIdentifier, projectIdentifier, criteria, optionalPipelineEntity);
  }

  @Override
  public PipelineEntity updatePipelineYaml(
      PipelineEntity pipelineToUpdate, PipelineEntity oldPipelineEntity, ChangeType changeType) {
    String accountIdentifier = pipelineToUpdate.getAccountIdentifier();
    String orgIdentifier = pipelineToUpdate.getOrgIdentifier();
    String projectIdentifier = pipelineToUpdate.getProjectIdentifier();
    if (GitContextHelper.isOldFlow()) {
      Supplier<OutboxEvent> supplier = null;
      if (!gitSyncSdkService.isGitSyncEnabled(accountIdentifier, orgIdentifier, projectIdentifier)) {
        supplier = ()
            -> outboxService.save(new PipelineUpdateEvent(
                accountIdentifier, orgIdentifier, projectIdentifier, pipelineToUpdate, oldPipelineEntity));
      }
      PipelineEntity updatedEntity = gitAwarePersistence.save(
          pipelineToUpdate, pipelineToUpdate.getYaml(), changeType, PipelineEntity.class, supplier);
      if (updatedEntity != null) {
        Supplier<OutboxEvent> supplierWithGitSyncTrue = ()
            -> outboxService.save(new PipelineUpdateEvent(
                accountIdentifier, orgIdentifier, projectIdentifier, pipelineToUpdate, oldPipelineEntity, true));
        makeSupplierCallIfGitSyncIsEnabled(
            accountIdentifier, orgIdentifier, projectIdentifier, supplierWithGitSyncTrue);
      }
      return updatedEntity;
    }
    return updateForSimplifiedFlow(pipelineToUpdate);
  }

  private PipelineEntity updateForSimplifiedFlow(PipelineEntity pipelineToUpdate) {
    Criteria criteria = Criteria.where(PipelineEntityKeys.deleted)
                            .is(false)
                            .and(PipelineEntityKeys.identifier)
                            .is(pipelineToUpdate.getIdentifier())
                            .and(PipelineEntityKeys.projectIdentifier)
                            .is(pipelineToUpdate.getProjectIdentifier())
                            .and(PipelineEntityKeys.orgIdentifier)
                            .is(pipelineToUpdate.getOrgIdentifier())
                            .and(PipelineEntityKeys.accountId)
                            .is(pipelineToUpdate.getAccountId());
    Query query = new Query(criteria);
    GitEntityInfo gitEntityInfo = GitContextHelper.getGitEntityInfo();
    if (gitEntityInfo.getStoreType() == StoreType.INLINE) {
      Update updateOperations = PMSPipelineFilterHelper.getUpdateOperationsForSimplifiedGitExperience(pipelineToUpdate);
      return mongoTemplate.findAndModify(
          query, updateOperations, new FindAndModifyOptions().returnNew(true), PipelineEntity.class);
    }
    String yamlToPush = pipelineToUpdate.getYaml();
    // push yamlToPush to git and create PR if needed
    pipelineToUpdate.setYaml("");
    pipelineToUpdate.setStoreType(StoreType.REMOTE);
    pipelineToUpdate.setConnectorRef(gitEntityInfo.getConnectorRef());
    pipelineToUpdate.setRepo(gitEntityInfo.getYamlGitConfigId());
    pipelineToUpdate.setFilePath(gitEntityInfo.getFilePath());
    Update updateOperations = PMSPipelineFilterHelper.getUpdateOperationsForSimplifiedGitExperience(pipelineToUpdate);

    return mongoTemplate.findAndModify(
        query, updateOperations, new FindAndModifyOptions().returnNew(true), PipelineEntity.class);
  }

  @Override
  public PipelineEntity updatePipelineMetadata(
      String accountId, String orgIdentifier, String projectIdentifier, Criteria criteria, Update update) {
    if (GitContextHelper.isOldFlow()) {
      criteria = gitAwarePersistence.makeCriteriaGitAware(
          accountId, orgIdentifier, projectIdentifier, PipelineEntity.class, criteria);
    }
    Query query = new Query(criteria);
    RetryPolicy<Object> retryPolicy = getRetryPolicyForPipelineUpdate();
    return Failsafe.with(retryPolicy)
        .get(()
                 -> mongoTemplate.findAndModify(
                     query, update, new FindAndModifyOptions().returnNew(true), PipelineEntity.class));
  }

  @Override
  public PipelineEntity deletePipeline(PipelineEntity pipelineToUpdate) {
    String accountId = pipelineToUpdate.getAccountId();
    String orgIdentifier = pipelineToUpdate.getOrgIdentifier();
    String projectIdentifier = pipelineToUpdate.getProjectIdentifier();
    Optional<PipelineEntity> pipelineEntityOptional =
        findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
            accountId, orgIdentifier, projectIdentifier, pipelineToUpdate.getIdentifier(), true);
    if (pipelineEntityOptional.isPresent()) {
      Supplier<OutboxEvent> supplier = ()
          -> outboxService.save(new PipelineDeleteEvent(accountId, orgIdentifier, projectIdentifier, pipelineToUpdate));
      PipelineEntity pipelineEntity = gitAwarePersistence.save(
          pipelineToUpdate, pipelineToUpdate.getYaml(), ChangeType.DELETE, PipelineEntity.class, supplier);
      if (pipelineEntity.getDeleted()) {
        Supplier<OutboxEvent> supplierWithGitSyncTrue = ()
            -> outboxService.save(
                new PipelineDeleteEvent(accountId, orgIdentifier, projectIdentifier, pipelineToUpdate, true));
        makeSupplierCallIfGitSyncIsEnabled(accountId, orgIdentifier, projectIdentifier, supplierWithGitSyncTrue);
      }
      return pipelineEntity;
    }
    throw new InvalidRequestException("No such pipeline exists");
  }

  private void makeSupplierCallIfGitSyncIsEnabled(
      String accountId, String orgIdentifier, String projectIdentifier, Supplier<OutboxEvent> supplier) {
    if (gitSyncSdkService.isGitSyncEnabled(accountId, orgIdentifier, projectIdentifier) && supplier != null) {
      supplier.get();
    }
  }

  private RetryPolicy<Object> getRetryPolicyForPipelineUpdate() {
    int MAX_ATTEMPTS = 3;
    Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(10);
    return new RetryPolicy<>()
        .handle(OptimisticLockingFailureException.class)
        .handle(DuplicateKeyException.class)
        .withDelay(RETRY_SLEEP_DURATION)
        .withMaxAttempts(MAX_ATTEMPTS)
        .onFailedAttempt(event
            -> log.info(
                "[Retrying]: Failed updating Pipeline; attempt: {}", event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event
            -> log.error(
                "[Failed]: Failed updating Pipeline; attempt: {}", event.getAttemptCount(), event.getFailure()));
  }

  private Optional<PipelineEntity> findV2(String accountId, String orgIdentifier, String projectIdentifier,
      Criteria criteria, Optional<PipelineEntity> entityV1) {
    if (pmsFeatureFlagService.isEnabled(accountId, GIT_SIMPLIFICATION)) {
      if (entityV1.isPresent()) {
        PipelineEntity entity = entityV1.get();
        if (entity.getStoreType() == null) {
          GitContextHelper.updateScmGitMetaData(ScmGitMetaData.builder()
                                                    .repoName(entity.getRepo())
                                                    .branchName(entity.getBranch())
                                                    .blobId(entity.getObjectIdOfYaml())
                                                    .filePath(entity.getFilePath())
                                                    .build());
          return entityV1;
        }
      }
      Criteria gitAwareCriteria = Criteria.where(PipelineEntityKeys.storeType).in(Arrays.asList(StoreType.values()));
      Query query = new Query().addCriteria(new Criteria().andOperator(criteria, gitAwareCriteria));
      PipelineEntity savedEntity = mongoTemplate.findOne(query, PipelineEntity.class);
      if (savedEntity == null) {
        return Optional.empty();
      }

      if (savedEntity.getStoreType() == StoreType.REMOTE) {
        // fetch yaml from git
        GitEntityInfo gitEntityInfo = GitContextHelper.getGitEntityInfoV2();
        savedEntity = (PipelineEntity) gitAwareEntityHelper.fetchEntityFromRemote(savedEntity,
            Scope.builder()
                .accountIdentifier(accountId)
                .orgIdentifier(orgIdentifier)
                .projectIdentifier(projectIdentifier)
                .build(),
            GitContextRequestParams.builder()
                .branchName(gitEntityInfo.getBaseBranch())
                .commitId(gitEntityInfo.getCommitId())
                .connectorRef(savedEntity.getConnectorRef())
                .filePath(savedEntity.getFilePath())
                .repoName(savedEntity.getRepo())
                .build(),
            Collections.emptyMap());
      }

      return Optional.of(savedEntity);
    } else {
      return entityV1;
    }
  }
}
