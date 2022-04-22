/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.beans.Scope;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.template.SRMTemplateDTO;
import io.harness.cvng.core.beans.template.TemplateType;
import io.harness.cvng.core.entities.SRMTemplate;
import io.harness.cvng.core.entities.SRMTemplate.SRMTemplateKeys;
import io.harness.cvng.core.services.api.SRMTemplateService;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;

public class SRMTemplateServiceImpl implements SRMTemplateService {
  @Inject HPersistence hPersistence;

  @Override
  public PageResponse<SRMTemplateDTO> list(
      ProjectParams projectParams, List<TemplateType> templateTypes, String searchString, PageRequest pageRequest) {
    Query<SRMTemplate> query = hPersistence.createQuery(SRMTemplate.class);
    Criteria accountScopeCriteria =
        query.and(query.criteria(SRMTemplateKeys.accountId).equal(projectParams.getAccountIdentifier()),
            query.criteria(SRMTemplateKeys.scope).equal(Scope.ACCOUNT));
    Criteria orgScopeQuery =
        query.and(query.criteria(SRMTemplateKeys.accountId).equal(projectParams.getAccountIdentifier()),
            query.criteria(SRMTemplateKeys.orgIdentifier).equal(projectParams.getOrgIdentifier()),
            query.criteria(SRMTemplateKeys.scope).equal(Scope.ORG));
    Criteria projectScopeQuery =
        query.and(query.criteria(SRMTemplateKeys.accountId).equal(projectParams.getAccountIdentifier()),
            query.criteria(SRMTemplateKeys.orgIdentifier).equal(projectParams.getOrgIdentifier()),
            query.criteria(SRMTemplateKeys.projectIdentifier).equal(projectParams.getProjectIdentifier()),
            query.criteria(SRMTemplateKeys.scope).equal(Scope.PROJECT));
    query.or(accountScopeCriteria, orgScopeQuery, projectScopeQuery);
    if (CollectionUtils.isNotEmpty(templateTypes)) {
      query.field(SRMTemplateKeys.templateType).in(templateTypes);
    }
    if (StringUtils.isNotEmpty(searchString)) {
      query.field(SRMTemplateKeys.name).startsWith(searchString);
    }
    query.order(Sort.descending(SRMTemplateKeys.lastUpdatedAt));
    List<SRMTemplate> srmTemplates = query.asList(new FindOptions()
                                                      .skip(pageRequest.getPageIndex() * pageRequest.getPageSize())
                                                      .limit(pageRequest.getPageSize()));
    Long totalItemCount = query.count();
    return PageResponse.<SRMTemplateDTO>builder()
        .pageIndex(pageRequest.getPageIndex())
        .pageSize(pageRequest.getPageSize())
        .pageItemCount(srmTemplates.size())
        .empty(CollectionUtils.isEmpty(srmTemplates))
        .totalItems(totalItemCount)
        .totalPages((long) Math.ceil(((double) totalItemCount) / pageRequest.getPageSize()))
        .content(srmTemplates.stream().map(entity -> transformToDTO(entity)).collect(Collectors.toList()))
        .build();
  }

  @Override
  public SRMTemplateDTO get(String accountId, String fullyQualifiedIdentifier) {
    SRMTemplate srmTemplate = getEntity(accountId, fullyQualifiedIdentifier);
    if (srmTemplate == null) {
      throw new IllegalArgumentException("Unable to find a template with identifier : " + fullyQualifiedIdentifier);
    }
    return transformToDTO(srmTemplate);
  }

  @Override
  public SRMTemplateDTO create(ProjectParams projectParams, SRMTemplateDTO srmTemplateDTO) {
    SRMTemplate srmTemplate = transformToEntity(projectParams, srmTemplateDTO);
    hPersistence.save(srmTemplate);
    return transformToDTO(srmTemplate);
  }

  @Override
  public SRMTemplateDTO update(
      ProjectParams projectParams, String fullyQualifiedIdentifier, SRMTemplateDTO srmTemplateDTO) {
    SRMTemplate existingTemplate = getEntity(projectParams.getAccountIdentifier(), fullyQualifiedIdentifier);
    if (existingTemplate == null) {
      throw new IllegalArgumentException("Unable to find a template with identifier : " + fullyQualifiedIdentifier);
    }
    UpdateOperations<SRMTemplate> srmTemplateUpdateOperations =
        hPersistence.createUpdateOperations(SRMTemplate.class)
            .set(SRMTemplateKeys.name, srmTemplateDTO.getName())
            .set(SRMTemplateKeys.yamlTemplate, srmTemplateDTO.getYamlTemplate())
            .set(SRMTemplateKeys.version, srmTemplateDTO.getVersion());
    hPersistence.update(existingTemplate, srmTemplateUpdateOperations);
    return get(projectParams.getAccountIdentifier(), fullyQualifiedIdentifier);
  }

  @Override
  public boolean delete(String accountId, String fullyQualifiedIdentifier) {
    return hPersistence.delete(hPersistence.createQuery(SRMTemplate.class)
                                   .filter(SRMTemplateKeys.accountId, accountId)
                                   .filter(SRMTemplateKeys.fullyQualifiedIdentifier, fullyQualifiedIdentifier));
  }

  private SRMTemplate getEntity(String accountId, String fullyQualifiedIdentifier) {
    return hPersistence.createQuery(SRMTemplate.class)
        .filter(SRMTemplateKeys.accountId, accountId)
        .filter(SRMTemplateKeys.fullyQualifiedIdentifier, fullyQualifiedIdentifier)
        .get();
  }

  private String getFullyQualifiedIdentifier(ProjectParams projectParams, SRMTemplateDTO srmTemplateDTO) {
    Scope scope = srmTemplateDTO.getScope();
    switch (scope) {
      case ACCOUNT:
        return StringUtils.joinWith("/", projectParams.getAccountIdentifier(), srmTemplateDTO.getIdentifier());
      case ORG:
        return StringUtils.joinWith("/", projectParams.getAccountIdentifier(), projectParams.getOrgIdentifier(),
            srmTemplateDTO.getIdentifier());
      case PROJECT:
        return StringUtils.joinWith("/", projectParams.getAccountIdentifier(), projectParams.getOrgIdentifier(),
            projectParams.getProjectIdentifier(), srmTemplateDTO.getIdentifier());
      default:
        throw new IllegalStateException("Unknown scope : " + scope);
    }
  }

  private SRMTemplate transformToEntity(ProjectParams projectParams, SRMTemplateDTO srmTemplateDTO) {
    SRMTemplate.SRMTemplateBuilder srmTemplateBuilder =
        SRMTemplate.builder()
            .accountId(projectParams.getAccountIdentifier())
            .scope(srmTemplateDTO.getScope())
            .identifier(srmTemplateDTO.getIdentifier())
            .name(srmTemplateDTO.getName())
            .fullyQualifiedIdentifier(getFullyQualifiedIdentifier(projectParams, srmTemplateDTO))
            .templateType(srmTemplateDTO.getTemplateType())
            .version(srmTemplateDTO.getVersion())
            .yamlTemplate(srmTemplateDTO.getYamlTemplate());

    if (srmTemplateDTO.getScope().equals(Scope.ORG) || srmTemplateDTO.getScope().equals(Scope.PROJECT)) {
      srmTemplateBuilder.orgIdentifier(projectParams.getOrgIdentifier());
    }
    if (srmTemplateDTO.getScope().equals(Scope.PROJECT)) {
      srmTemplateBuilder.projectIdentifier(projectParams.getProjectIdentifier());
    }
    return srmTemplateBuilder.build();
  }

  private SRMTemplateDTO transformToDTO(SRMTemplate srmTemplate) {
    return SRMTemplateDTO.builder()
        .accountId(srmTemplate.getAccountId())
        .orgIdentifier(srmTemplate.getOrgIdentifier())
        .projectIdentifier(srmTemplate.getProjectIdentifier())
        .scope(srmTemplate.getScope())
        .identifier(srmTemplate.getIdentifier())
        .fullyQualifiedIdentifier(srmTemplate.getFullyQualifiedIdentifier())
        .templateType(srmTemplate.getTemplateType())
        .yamlTemplate(srmTemplate.getYamlTemplate())
        .name(srmTemplate.getName())
        .version(srmTemplate.getVersion())
        .build();
  }
}
