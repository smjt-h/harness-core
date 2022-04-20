/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.expressions.functors;

import io.harness.beans.Scope;
import io.harness.beans.ScopeLevel;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.variable.dto.VariableConfigDTO;
import io.harness.ng.core.variable.dto.VariableDTO;
import io.harness.ng.core.variable.mappers.VariableMapper;
import io.harness.ng.core.variable.services.VariableService;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.execution.expression.SdkFunctor;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NGVariableFunctor implements SdkFunctor {
  public static final String NG_VARIABLE = "variable";
  @Inject VariableService variableService;
  @Inject VariableMapper variableMapper;
  /*

  <+default.var1>
  <+orgVariable.var1>
  <+projectVariable.var1>

  <+variable.get("account.var1")>
  1. This fall in line with secret expression e.g. <+secret.getValue(secretIdentifier)>
  2. Have scope of optimization without changing the expression. start with calling db for every variable encounters.
      To optimise we can fetch all account variables and keep in cache return from cache.

  <+variable.account.var1>
  {
    "var1" -> "value 1",
    "var2" -> "value 2",
  }
  1. This is similar to pipeline e.g. <+pipeline.variable.var1> though pipeline follows scope.variable.varName
  2. Only way to support this is to get all variables of account scope first and then return the expected.
  3. Optimization can be via caching only. Not possible to fetch value of just the required variable.

  <+account.variable.var1>
  1. Requires modification in existing functor.
  2. If account is part of different service and variable of different, then for this functor to function it require an
     aggregator layer in between to collect data from 2 different service.
  3. Here also we have to fetch all the variables in a given scope.
  4. Optimization can be via caching only. Not possible to fetch value of just the required variable.
  */
  // public abstract ScopeLevel getScope();

  @Override
  public Object get(Ambiance ambiance, String... args) {
    log.info("Fetching ng variables with args: {}", Arrays.asList(args));
    // String[] fqnNodes = args[0].split("\\.");
    // String scope = (fqnNodes.length > 1) ? fqnNodes[0] : "project";
    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    try {
      Scope ambianceScope =
          Scope.of(ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());
      ScopeLevel variableScopeLevel = getScope(args[0]);
      validateAccess(ScopeLevel.of(ambianceScope), variableScopeLevel);
      return getVariableValue(ngAccess, variableScopeLevel);
    } catch (UnknownEnumTypeException e) {
      log.info("Argument [{}] is not a scope. Assuming it as variable identifier and proceeding", args[0]);
    }
    // String identifier = fqnNodes[fqnNodes.length - 1];
    String identifier = args[0];
    return getVariableValue(ngAccess, identifier);
  }

  protected Map<String, String> getVariableValue(NGAccess ngAccess, ScopeLevel variableScopeLevel) {
    List<VariableDTO> variableDTOList;
    switch (variableScopeLevel) {
      case ACCOUNT:
        variableDTOList = variableService.listVariableDTOs(ngAccess.getAccountIdentifier(), null, null);
        break;
      case ORGANIZATION:
        variableDTOList =
            variableService.listVariableDTOs(ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), null);
        break;
      case PROJECT:
      default:
        variableDTOList = variableService.listVariableDTOs(
            ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());
        break;
    }
    return variableDTOList.stream().collect(
        Collectors.toMap(VariableDTO::getIdentifier, variableDTO -> variableDTO.getVariableConfig().getValue()));
  }

  protected String getVariableValue(NGAccess ngAccess, String identifier) {
    VariableDTO variableDTO = variableService.getVariableInNearestScope(
        ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier(), identifier);
    return variableDTO.getVariableConfig().getValue();
  }

  protected void validateAccess(ScopeLevel ambianceScopeLevel, ScopeLevel variableScopeLevel) {
    log.info("Validating scope bases access. Ambiance Scope: {} | Variable scope: {}", ambianceScopeLevel,
        variableScopeLevel);
    if (variableScopeLevel.ordinal() > ambianceScopeLevel.ordinal()) {
      throw new IllegalArgumentException(
          String.format("Variable of %s scope cannot be used at %s scope", variableScopeLevel, ambianceScopeLevel));
    }
  }

  private ScopeLevel getScope(String scope) {
    switch (scope) {
      case "account":
        return ScopeLevel.ACCOUNT;
      case "org":
        return ScopeLevel.ORGANIZATION;
      case "project":
        return ScopeLevel.PROJECT;
      default:
        throw new UnknownEnumTypeException("ScopeLevel", scope);
    }
  }
}
