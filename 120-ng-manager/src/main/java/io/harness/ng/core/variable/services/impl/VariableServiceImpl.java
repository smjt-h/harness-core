/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.variable.services.impl;

import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.springdata.TransactionUtils.DEFAULT_TRANSACTION_RETRY_POLICY;

import io.harness.beans.Scope;
import io.harness.beans.ScopeLevel;
import io.harness.exception.DuplicateFieldException;
import io.harness.ng.core.events.VariableCreateEvent;
import io.harness.ng.core.variable.dto.VariableDTO;
import io.harness.ng.core.variable.entity.Variable;
import io.harness.ng.core.variable.entity.Variable.VariableKeys;
import io.harness.ng.core.variable.mappers.VariableMapper;
import io.harness.ng.core.variable.services.VariableService;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.variable.spring.VariableRepository;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import net.jodah.failsafe.Failsafe;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.transaction.support.TransactionTemplate;

public class VariableServiceImpl implements VariableService {
  private final VariableRepository variableRepository;
  private final VariableMapper variableMapper;
  private final TransactionTemplate transactionTemplate;
  private final OutboxService outboxService;

  @Inject
  public VariableServiceImpl(VariableRepository variableRepository, VariableMapper variableMapper,
      @Named(OUTBOX_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate, OutboxService outboxService) {
    this.variableRepository = variableRepository;
    this.variableMapper = variableMapper;
    this.transactionTemplate = transactionTemplate;
    this.outboxService = outboxService;
  }

  @Override
  public Variable create(String accountIdentifier, VariableDTO variableDTO) {
    variableDTO.getVariableConfig().validate();
    try {
      Variable variable = variableMapper.toVariable(accountIdentifier, variableDTO);
      return Failsafe.with(DEFAULT_TRANSACTION_RETRY_POLICY).get(() -> transactionTemplate.execute(status -> {
        Variable savedVariable = variableRepository.save(variable);
        outboxService.save(new VariableCreateEvent(accountIdentifier, variableMapper.writeDTO(savedVariable)));
        return savedVariable;
      }));
    } catch (DuplicateKeyException de) {
      throw new DuplicateFieldException(
          String.format("A variable with identifier %s and orgIdentifier %s and projectIdentifier %s already present.",
              variableDTO.getIdentifier(), variableDTO.getOrgIdentifier(), variableDTO.getProjectIdentifier()),
          USER_SRE, de);
    }
  }

  @Override
  public Optional<Variable> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return variableRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier);
  }

  @Override
  public VariableDTO getVariableInNearestScope(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    ScopeLevel variableScopeLevel = ScopeLevel.of(Scope.of(accountIdentifier, orgIdentifier, projectIdentifier));
    Optional<Variable> variable = Optional.ofNullable(null);
    List<ScopeLevel> reversedScopeLevels = Arrays.asList(ScopeLevel.values().clone());
    Collections.reverse(reversedScopeLevels);
    for (ScopeLevel level : reversedScopeLevels) {
      if (level.ordinal() > variableScopeLevel.ordinal()) {
        continue;
      }
      variable = getVariableByScope(accountIdentifier, orgIdentifier, projectIdentifier, identifier, level);
    }
    if (variable.isPresent()) {
      return variableMapper.writeDTO(variable.get());
    } else {
      String parentScopeMessage = " or any parent scope";
      if (variableScopeLevel.equals(ScopeLevel.ACCOUNT)) {
        parentScopeMessage = "";
      }
      throw new NotFoundException(
          String.format("Variable [%s] not found in %s scope%s", identifier, variableScopeLevel, parentScopeMessage));
    }
  }

  private Optional<Variable> getVariableByScope(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier, ScopeLevel scope) {
    switch (scope) {
      case ACCOUNT:
        return variableRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountIdentifier, null, null, identifier);
      case ORGANIZATION:
        return variableRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, null, identifier);
      default:
      case PROJECT:
        return variableRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    }
  }

  @Override
  public List<Variable> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, List<String> variableIdentifier) {
    return null;
  }

  @Override
  public List<Variable> get(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Criteria criteria = Criteria.where(VariableKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(VariableKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(VariableKeys.projectIdentifier)
                            .is(projectIdentifier);
    return variableRepository.findAll(criteria);
  }

  @Override
  public List<VariableDTO> listVariableDTOs(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    List<Variable> variableList = get(accountIdentifier, orgIdentifier, projectIdentifier);
    return variableList.stream().map(variableMapper::writeDTO).collect(Collectors.toList());
  }
}
