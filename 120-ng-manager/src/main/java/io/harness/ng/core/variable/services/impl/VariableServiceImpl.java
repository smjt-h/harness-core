/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.variable.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.ng.core.variable.entity.Variable.VariableKeys;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.springdata.TransactionUtils.DEFAULT_TRANSACTION_RETRY_POLICY;
import static io.harness.utils.PageUtils.getPageRequest;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.NGResourceFilterConstants;
import io.harness.beans.SortOrder;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.events.VariableCreateEvent;
import io.harness.ng.core.events.VariableDeleteEvent;
import io.harness.ng.core.events.VariableUpdateEvent;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.variable.dto.VariableConfigDTO;
import io.harness.ng.core.variable.dto.VariableDTO;
import io.harness.ng.core.variable.dto.VariableResponseDTO;
import io.harness.ng.core.variable.entity.Variable;
import io.harness.ng.core.variable.mappers.VariableMapper;
import io.harness.ng.core.variable.services.VariableService;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.variable.spring.VariableRepository;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import net.jodah.failsafe.Failsafe;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.transaction.support.TransactionTemplate;

public class VariableServiceImpl implements VariableService {
  private final VariableRepository variableRepository;
  private final VariableMapper variableMapper;
  private final TransactionTemplate transactionTemplate;
  private final OutboxService outboxService;
  private final ProjectService projectService;
  private final OrganizationService organizationService;

  @Inject
  public VariableServiceImpl(VariableRepository variableRepository, VariableMapper variableMapper,
      @Named(OUTBOX_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate, OutboxService outboxService,
      ProjectService projectService, OrganizationService organizationService) {
    this.variableRepository = variableRepository;
    this.variableMapper = variableMapper;
    this.transactionTemplate = transactionTemplate;
    this.outboxService = outboxService;
    this.projectService = projectService;
    this.organizationService = organizationService;
  }

  @Override
  public Variable create(String accountIdentifier, VariableDTO variableDTO) {
    if (null == variableDTO.getVariableConfig()) {
      throw new InvalidRequestException("Variable config cannot be null");
    }
    variableDTO.getVariableConfig().validate();
    assureThatTheProjectAndOrgExists(variableDTO, accountIdentifier);
    try {
      Variable variable = variableMapper.toVariable(accountIdentifier, variableDTO);
      return Failsafe.with(DEFAULT_TRANSACTION_RETRY_POLICY).get(() -> transactionTemplate.execute(status -> {
        Variable savedVariable = variableRepository.save(variable);
        outboxService.save(new VariableCreateEvent(accountIdentifier, variableMapper.writeDTO(savedVariable)));
        return savedVariable;
      }));
    } catch (DuplicateKeyException de) {
      throw new DuplicateFieldException(
          String.format(
              "A variable with identifier [%s] and orgIdentifier [%s] and projectIdentifier [%s] already present.",
              variableDTO.getIdentifier(), variableDTO.getOrgIdentifier(), variableDTO.getProjectIdentifier()),
          USER_SRE, de);
    }
  }

  @Override
  public PageResponse<VariableResponseDTO> list(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, int page, int size, String searchTerm, boolean includeVariablesFromEverySubScope) {
    Criteria criteria = getCriteriaForVariableList(
        accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, includeVariablesFromEverySubScope);
    Pageable pageable = getPageRequest(
        PageRequest.builder()
            .pageIndex(page)
            .pageSize(size)
            .sortOrders(Collections.singletonList(
                SortOrder.Builder.aSortOrder().withField(VariableKeys.createdAt, SortOrder.OrderType.DESC).build()))
            .build());
    Page<Variable> variables = variableRepository.findAll(criteria, pageable);

    return PageUtils.getNGPageResponse(
        variables, variables.getContent().stream().map(variableMapper::toResponseWrapper).collect(Collectors.toList()));
  }

  private Criteria getCriteriaForVariableList(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String searchTerm, boolean includeVariablesFromEverySubScope) {
    Criteria criteria = Criteria.where(VariableKeys.accountIdentifier).is(accountIdentifier);
    if (!includeVariablesFromEverySubScope) {
      criteria.and(VariableKeys.orgIdentifier)
          .is(orgIdentifier)
          .and(VariableKeys.projectIdentifier)
          .is(projectIdentifier);
    } else {
      if (isNotBlank(orgIdentifier)) {
        criteria.and(VariableKeys.orgIdentifier).is(orgIdentifier);
        if (isNotBlank(projectIdentifier)) {
          criteria.and(VariableKeys.projectIdentifier).is(projectIdentifier);
        }
      }
    }
    if (!StringUtils.isEmpty(searchTerm)) {
      criteria = criteria.orOperator(
          Criteria.where(VariableKeys.name).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS),
          Criteria.where(VariableKeys.identifier)
              .regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS));
    }
    return criteria;
  }

  @Override
  public List<VariableDTO> list(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    List<Variable> variables = variableRepository.findAllByAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier);
    return variables.stream().map(variableMapper::writeDTO).collect(Collectors.toList());
  }

  @Override
  public Optional<VariableResponseDTO> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    Optional<Variable> variable =
        variableRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return variable.map(variableMapper::toResponseWrapper);
  }

  @Override
  public Variable update(String accountIdentifier, VariableDTO variableDTO) {
    VariableConfigDTO variableConfigDTO = variableDTO.getVariableConfig();
    variableConfigDTO.validate();
    Optional<Variable> existingVariable =
        variableRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(accountIdentifier,
            variableDTO.getOrgIdentifier(), variableDTO.getProjectIdentifier(), variableDTO.getIdentifier());
    validateTheUpdateRequestIsValid(accountIdentifier, variableDTO, existingVariable);
    assureThatTheProjectAndOrgExists(variableDTO, accountIdentifier);
    try {
      Variable newVariable = variableMapper.toVariable(accountIdentifier, variableDTO);
      newVariable.setLastModifiedAt(System.currentTimeMillis());
      if (existingVariable.isPresent()) {
        newVariable.setCreatedAt(existingVariable.get().getCreatedAt());
        newVariable.setId(existingVariable.get().getId());
      } else {
        throw new NotFoundException(
            String.format("Variable [%s] Not Found with orgIdentifier- [%s], projectIdentifier- [%s]",
                variableDTO.getIdentifier(), variableDTO.getOrgIdentifier(), variableDTO.getProjectIdentifier()));
      }
      return Failsafe.with(DEFAULT_TRANSACTION_RETRY_POLICY).get(() -> transactionTemplate.execute(status -> {
        Variable updatedVariable = variableRepository.save(newVariable);
        outboxService.save(new VariableUpdateEvent(accountIdentifier, variableMapper.writeDTO(updatedVariable),
            variableMapper.writeDTO(existingVariable.get())));
        return updatedVariable;
      }));
    } catch (DuplicateKeyException de) {
      throw new DuplicateFieldException(
          String.format(
              "A variable with identifier [%s] and orgIdentifier [%s] and projectIdentifier [%s] already present.",
              variableDTO.getIdentifier(), variableDTO.getOrgIdentifier(), variableDTO.getProjectIdentifier()),
          USER_SRE, de);
    }
  }

  @Override
  public boolean delete(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String variableIdentifier) {
    Optional<Variable> existingVariable =
        variableRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, variableIdentifier);
    if (existingVariable.isPresent()) {
      variableRepository.delete(existingVariable.get());
      outboxService.save(new VariableDeleteEvent(accountIdentifier, variableMapper.writeDTO(existingVariable.get())));
    } else {
      throw new NotFoundException(
          String.format("Variable [%s] Not Found with orgIdentifier- [%s], projectIdentifier- [%s]", variableIdentifier,
              orgIdentifier, projectIdentifier));
    }
    return false;
  }

  public void validateTheUpdateRequestIsValid(
      String accountIdentifier, VariableDTO variableDTO, Optional<Variable> existingVariable) {
    if (!existingVariable.isPresent()) {
      throw new NotFoundException(
          String.format("Variable [%s] Not Found with orgIdentifier- [%s], projectIdentifier- [%s]",
              variableDTO.getIdentifier(), variableDTO.getOrgIdentifier(), variableDTO.getProjectIdentifier()));
    }
    validateImmutableFieldsAreNotChanged(variableDTO, existingVariable.get());
  }

  public void validateImmutableFieldsAreNotChanged(VariableDTO variableDTO, Variable existingVariable) {
    if (!Objects.equals(variableDTO.getType(), existingVariable.getType())) {
      throw new InvalidRequestException("Variable Type cannot be changed");
    }
    if (!Objects.equals(variableDTO.getVariableConfig().getValueType(), existingVariable.getValueType())) {
      throw new InvalidRequestException("Variable Value Type cannot be changed");
    }
  }

  void assureThatTheProjectAndOrgExists(VariableDTO variableDTO, String accountIdentifier) {
    String orgIdentifier = variableDTO.getOrgIdentifier();
    String projectIdentifier = variableDTO.getProjectIdentifier();

    if (isNotEmpty(projectIdentifier)) {
      // its a project level variable
      if (isEmpty(orgIdentifier)) {
        throw new InvalidRequestException(
            String.format("Project %s specified without the org Identifier", projectIdentifier));
      }
      checkThatTheProjectExists(orgIdentifier, projectIdentifier, accountIdentifier);
    } else if (isNotEmpty(orgIdentifier)) {
      // its a org level variable
      checkThatTheOrganizationExists(orgIdentifier, accountIdentifier);
    }
  }

  private void checkThatTheOrganizationExists(String orgIdentifier, String accountIdentifier) {
    if (isNotEmpty(orgIdentifier)) {
      final Optional<Organization> organization = organizationService.get(accountIdentifier, orgIdentifier);
      if (!organization.isPresent()) {
        throw new NotFoundException(String.format("org [%s] not found.", orgIdentifier));
      }
    }
  }

  private void checkThatTheProjectExists(String orgIdentifier, String projectIdentifier, String accountIdentifier) {
    if (isNotEmpty(orgIdentifier) && isNotEmpty(projectIdentifier)) {
      final Optional<Project> project = projectService.get(accountIdentifier, orgIdentifier, projectIdentifier);
      if (!project.isPresent()) {
        throw new NotFoundException(String.format("project [%s] not found.", projectIdentifier));
      }
    }
  }
}
