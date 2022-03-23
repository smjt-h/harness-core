/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.envGroup.services;

import com.google.common.collect.ImmutableMap;
import com.google.inject.name.Named;
import com.google.protobuf.StringValue;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.repositories.envGroup.EnvironmentGroupRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
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

  @Inject
  public EnvironmentGroupServiceImpl(EnvironmentGroupRepository environmentRepository,@Named(EventsFrameworkConstants.ENTITY_CRUD) Producer eventProducer) {
    this.environmentRepository = environmentRepository;
    this.eventProducer = eventProducer;
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
    return savedEntity;
  }

  @Override
  public Page<EnvironmentGroupEntity> list(
      Criteria criteria, Pageable pageRequest, String projectIdentifier, String orgIdentifier, String accountId) {
    return environmentRepository.list(criteria, pageRequest, projectIdentifier, orgIdentifier, accountId);
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
                              EventsFrameworkMetadataConstants.ENTITY_TYPE, EventsFrameworkMetadataConstants.ENVIRONMENT_GROUP_ENTITY,
                              EventsFrameworkMetadataConstants.ACTION, action))
                      .setData(environmentChangeEvent.build().toByteString())
                      .build());
    } catch (EventsFrameworkDownException e) {
      log.error("Failed to send event to events framework Environment Group Identifier: {}", identifier, e);
    }
  }
}
