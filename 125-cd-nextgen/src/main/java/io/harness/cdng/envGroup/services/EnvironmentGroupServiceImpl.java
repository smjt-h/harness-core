/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.envGroup.services;

import static java.lang.String.format;

import com.google.common.collect.ImmutableMap;
import com.google.inject.name.Named;
import com.google.protobuf.StringValue;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity;
import io.harness.eventsframework.protohelper.IdentifierRefProtoDTOHelper;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.eventsframework.schemas.entitysetupusage.EntityDetailWithSetupUsageDetailProtoDTO;
import io.harness.eventsframework.schemas.entitysetupusage.EntitySetupUsageCreateV2DTO;
import io.harness.exception.InvalidRequestException;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.ng.core.entitysetupusage.dto.SetupUsageDetailType;
import io.harness.repositories.envGroup.EnvironmentGroupRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class EnvironmentGroupServiceImpl implements EnvironmentGroupService {
  private final EnvironmentGroupRepository environmentRepository;
  private Producer eventProducer;
  private Producer setupUsagesEventProducer;
  private IdentifierRefProtoDTOHelper identifierRefProtoDTOHelper;

  @Inject
  public EnvironmentGroupServiceImpl(EnvironmentGroupRepository environmentRepository, @Named(EventsFrameworkConstants.ENTITY_CRUD) Producer eventProducer, @Named(EventsFrameworkConstants.SETUP_USAGE) Producer setupUsagesEventProducer, IdentifierRefProtoDTOHelper identifierRefProtoDTOHelper) {
    this.environmentRepository = environmentRepository;
    this.eventProducer = eventProducer;
    this.setupUsagesEventProducer = setupUsagesEventProducer;
    this.identifierRefProtoDTOHelper = identifierRefProtoDTOHelper;
  }

  @Override
  public Optional<EnvironmentGroupEntity> get(
      String accountId, String orgIdentifier, String projectIdentifier, String envGroupId, boolean deleted) {
    return environmentRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
        accountId, orgIdentifier, projectIdentifier, envGroupId, !deleted);
  }

  @Override
  public EnvironmentGroupEntity create(EnvironmentGroupEntity entity) {
    EnvironmentGroupEntity savedEntity = environmentRepository.create(entity);
    publishEvent(savedEntity.getAccountIdentifier(),savedEntity.getOrgIdentifier(),savedEntity.getProjectIdentifier(),savedEntity.getIdentifier(),EventsFrameworkMetadataConstants.CREATE_ACTION);
    setupUsagesForEnvironmentList(entity);
    return savedEntity;
  }

  @Override
  public Page<EnvironmentGroupEntity> list(
      Criteria criteria, Pageable pageRequest, String projectIdentifier, String orgIdentifier, String accountId) {
    return environmentRepository.list(criteria, pageRequest, projectIdentifier, orgIdentifier, accountId);
  }

  @Override
  public EnvironmentGroupEntity delete(
      String accountId, String orgIdentifier, String projectIdentifier, String envGroupId, Long version) {
    Optional<EnvironmentGroupEntity> envGroupEntity =
        get(accountId, orgIdentifier, projectIdentifier, envGroupId, false);
    if (!envGroupEntity.isPresent()) {
      throw new InvalidRequestException(
          format("Environment Group [%s] under Project[%s], Organization [%s] doesn't exist.", envGroupId,
              projectIdentifier, orgIdentifier));
    }
    EnvironmentGroupEntity existingEntity = envGroupEntity.get();
    if (version != null && !version.equals(existingEntity.getVersion())) {
      throw new InvalidRequestException(
          format("Environment Group [%s] under Project[%s], Organization [%s] is not on the correct version.",
              envGroupId, projectIdentifier, orgIdentifier));
    }
    EnvironmentGroupEntity entityWithDelete = existingEntity.withDeleted(true);
    try {
      EnvironmentGroupEntity deletedEntity = environmentRepository.deleteEnvGroup(entityWithDelete);

      if (deletedEntity.getDeleted()) {
        return deletedEntity;
      } else {
        throw new InvalidRequestException(
            format("Environment Group Set [%s] under Project[%s], Organization [%s] couldn't be deleted.", envGroupId,
                projectIdentifier, orgIdentifier));
      }
    } catch (Exception e) {
      log.error(String.format("Error while deleting Environment Group [%s]", envGroupId), e);
      throw new InvalidRequestException(
          String.format("Error while deleting input set [%s]: %s", envGroupId, e.getMessage()));
    }
  }

  @Override
  public EnvironmentGroupEntity update(EnvironmentGroupEntity requestedEntity) {
    String accountId = requestedEntity.getAccountId();
    String orgId = requestedEntity.getOrgIdentifier();
    String projectId = requestedEntity.getProjectIdentifier();
    String envGroupId = requestedEntity.getIdentifier();

    Optional<EnvironmentGroupEntity> optionalEnvGroupEntity = get(accountId, orgId, projectId, envGroupId, false);
    if (!optionalEnvGroupEntity.isPresent()) {
      throw new InvalidRequestException(
          String.format("Environment Group %s in project %s in organization %s is either deleted or was not created",
              envGroupId, projectId, orgId));
    }

    EnvironmentGroupEntity originalEntity = optionalEnvGroupEntity.get();
    if (originalEntity.getVersion() != null && !originalEntity.getVersion().equals(originalEntity.getVersion())) {
      throw new InvalidRequestException(format(
          "Environment Group [%s] under Project[%s], Organization [%s] is not on the correct version.",
          originalEntity.getIdentifier(), originalEntity.getProjectIdentifier(), originalEntity.getOrgIdentifier()));
    }

    EnvironmentGroupEntity updatedEntity = originalEntity.withName(requestedEntity.getName())
                                               .withDescription(requestedEntity.getDescription())
                                               .withLastModifiedAt(System.currentTimeMillis())
                                               .withColor(requestedEntity.getColor())
                                               .withEnvIdentifiers(requestedEntity.getEnvIdentifiers())
                                               .withTags(requestedEntity.getTags())
                                               .withYaml(requestedEntity.getYaml());
    EnvironmentGroupEntity savedEntity =  environmentRepository.update(updatedEntity, originalEntity);
    publishEvent(savedEntity.getAccountIdentifier(),savedEntity.getOrgIdentifier(),savedEntity.getProjectIdentifier(),savedEntity.getIdentifier(),EventsFrameworkMetadataConstants.UPDATE_ACTION);
    setupUsagesForEnvironmentList(savedEntity);
    return savedEntity;
  }

  private void publishEvent(
          String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier, String action) {
    try {
      EntityChangeDTO.Builder environmentChangeEvent = EntityChangeDTO.newBuilder()
              .setAccountIdentifier(StringValue.of(accountIdentifier))
              .setIdentifier(StringValue.of(identifier));
      if (isNotBlank(orgIdentifier)) {
        environmentChangeEvent.setOrgIdentifier(StringValue.of(orgIdentifier));
      }
      if (isNotBlank(projectIdentifier)) {
        environmentChangeEvent.setProjectIdentifier(StringValue.of(projectIdentifier));
      }
      eventProducer.send(
              Message.newBuilder()
                      .putAllMetadata(ImmutableMap.of("accountId", accountIdentifier,
                              EventsFrameworkMetadataConstants.ENTITY_TYPE, EntityTypeProtoEnum.ENVIRONMENT_GROUP.name(),
                              EventsFrameworkMetadataConstants.ACTION, action))
                      .setData(environmentChangeEvent.build().toByteString())
                      .build());
    } catch (EventsFrameworkDownException e) {
      log.error("Failed to send event to events framework Environment Group Identifier: {}", identifier, e);
    }
  }

  private void setupUsagesForEnvironmentList(EnvironmentGroupEntity envGroupEntity) {
    List<EntityDetailProtoDTO> referredEntities = getEnvReferredEntities(envGroupEntity);
    EntityDetailProtoDTO envGroupDetails =
            EntityDetailProtoDTO.newBuilder()
                    .setIdentifierRef(identifierRefProtoDTOHelper.createIdentifierRefProtoDTO(envGroupEntity.getAccountId(),
                            envGroupEntity.getOrgIdentifier(), envGroupEntity.getProjectIdentifier(),
                            envGroupEntity.getIdentifier()))
                    .setType(EntityTypeProtoEnum.ENVIRONMENT_GROUP)
                    .setName(envGroupEntity.getName())
                    .build();

      EntitySetupUsageCreateV2DTO entityReferenceDTO =
              EntitySetupUsageCreateV2DTO.newBuilder()
                      .setAccountIdentifier(envGroupEntity.getAccountId())
                      .setReferredByEntity(envGroupDetails)
                      .addAllReferredEntities(referredEntities)
                      .setDeleteOldReferredByRecords(true)
                      .build();

      setupUsagesEventProducer.send(
              Message.newBuilder()
                      .putAllMetadata(ImmutableMap.of("accountId", envGroupEntity.getAccountId(),
                              EventsFrameworkMetadataConstants.REFERRED_ENTITY_TYPE, EntityTypeProtoEnum.ENVIRONMENT.name(),
                              EventsFrameworkMetadataConstants.ACTION, EventsFrameworkMetadataConstants.FLUSH_CREATE_ACTION))
                      .setData(entityReferenceDTO.toByteString())
                      .build());

  }


  private List<EntityDetailProtoDTO> getEnvReferredEntities(EnvironmentGroupEntity entity) {
    List<String> envIdentifiers = entity.getEnvIdentifiers();
    return envIdentifiers.stream().map(env -> EntityDetailProtoDTO.newBuilder().setIdentifierRef(IdentifierRefProtoDTO.newBuilder().setAccountIdentifier(StringValue.of(entity.getAccountId())).setOrgIdentifier(StringValue.of(entity.getOrgIdentifier())).setProjectIdentifier(StringValue.of(entity.getProjectIdentifier())).setIdentifier(StringValue.of(env)).build()).setType(EntityTypeProtoEnum.ENVIRONMENT).build()).collect(Collectors.toList());
  }
}
