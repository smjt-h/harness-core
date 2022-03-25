/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.service.gitsync;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.common.EntityReference;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.FileChange;
import io.harness.gitsync.FullSyncChangeSet;
import io.harness.gitsync.ScopeDetails;
import io.harness.gitsync.entityInfo.AbstractGitSdkEntityHandler;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.gitsync.sdk.EntityGitDetailsMapper;
import io.harness.grpc.utils.StringValueUtils;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.helper.ServiceEntityDetailUtils;
import io.harness.ng.core.service.mappers.NGServiceEntityMapper;
import io.harness.ng.core.service.services.ServiceEntityService;
import io.harness.ng.core.service.yaml.NGServiceConfig;
import io.harness.ng.core.service.yaml.NGServiceV2InfoConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@OwnedBy(HarnessTeam.CDC)
@Singleton
public class ServiceEntityGitSyncHandler extends AbstractGitSdkEntityHandler<ServiceEntity, NGServiceConfig>
    implements GitSdkEntityHandlerInterface<ServiceEntity, NGServiceConfig> {
  private final ServiceEntityService serviceEntityService;

  @Inject
  public ServiceEntityGitSyncHandler(ServiceEntityService serviceEntityService) {
    this.serviceEntityService = serviceEntityService;
  }

  @Override
  public Optional<EntityGitDetails> getEntityDetailsIfExists(String accountIdentifier, String yaml) {
    NGServiceConfig yamlDTO = getYamlDTO(yaml);
    NGServiceV2InfoConfig ngServiceV2InfoConfig = yamlDTO.getNgServiceV2InfoConfig();
    Optional<ServiceEntity> serviceEntity =
        serviceEntityService.get(accountIdentifier, ngServiceV2InfoConfig.getOrgIdentifier(),
            ngServiceV2InfoConfig.getProjectIdentifier(), ngServiceV2InfoConfig.getIdentifier(), false);
    return serviceEntity.map(EntityGitDetailsMapper::mapEntityGitDetails);
  }

  @Override
  public NGServiceConfig getYamlDTO(String yaml) {
    return NGServiceEntityMapper.toDTO(yaml);
  }

  @Override
  public String getYamlFromEntityRef(EntityDetailProtoDTO entityReference) {
    IdentifierRefProtoDTO identifierRef = entityReference.getIdentifierRef();
    Optional<ServiceEntity> serviceEntity =
        serviceEntityService.get(StringValueUtils.getStringFromStringValue(identifierRef.getAccountIdentifier()),
            StringValueUtils.getStringFromStringValue(identifierRef.getOrgIdentifier()),
            StringValueUtils.getStringFromStringValue(identifierRef.getProjectIdentifier()),
            StringValueUtils.getStringFromStringValue(identifierRef.getIdentifier()), false);
    return serviceEntity.get().getYaml();
  }

  @Override
  protected NGServiceConfig updateEntityFilePath(String accountIdentifier, String yaml, String newFilePath) {
    ServiceEntity serviceEntity = NGServiceEntityMapper.toServiceEntity(accountIdentifier, yaml);
    // return NGTemplateDtoMapper.toDTO(serviceEntityService.updateGitFilePath(serviceEntity, newFilePath));
    return null;
  }

  @Override
  public Supplier<NGServiceConfig> getYamlFromEntity(ServiceEntity entity) {
    return () -> NGServiceEntityMapper.toDTO(entity);
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.SERVICE;
  }

  @Override
  public Supplier<ServiceEntity> getEntityFromYaml(NGServiceConfig ngServiceConfig, String accountIdentifier) {
    return () -> NGServiceEntityMapper.toServiceEntity(accountIdentifier, ngServiceConfig);
  }

  @Override
  public EntityDetail getEntityDetail(ServiceEntity entity) {
    return ServiceEntityDetailUtils.getEntityDetail(entity);
  }

  @Override
  public NGServiceConfig save(String accountIdentifier, String yaml) {
    return null;
  }

  @Override
  public NGServiceConfig update(String accountIdentifier, String yaml, ChangeType changeType) {
    ServiceEntity serviceEntity = NGServiceEntityMapper.toServiceEntity(accountIdentifier, yaml);
    return NGServiceEntityMapper.toNGServiceConfig(serviceEntity);
  }

  @Override
  public boolean markEntityInvalid(String accountIdentifier, EntityReference entityReference, String erroneousYaml) {
    return false;
  }

  @Override
  public boolean delete(EntityReference entityReference) {
    return false;
  }

  @Override
  public String getObjectIdOfYamlKey() {
    return null;
  }

  @Override
  public String getIsFromDefaultBranchKey() {
    return null;
  }

  @Override
  public String getYamlGitConfigRefKey() {
    return null;
  }

  @Override
  public String getUuidKey() {
    return null;
  }

  @Override
  public String getBranchKey() {
    return null;
  }

  @Override
  public NGServiceConfig fullSyncEntity(FullSyncChangeSet fullSyncChangeSet) {
    return null;
  }

  @Override
  public List<FileChange> listAllEntities(ScopeDetails scopeDetails) {
    return null;
  }
}
