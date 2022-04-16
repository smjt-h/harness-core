/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.expression.functors;

import io.harness.beans.Scope;
import io.harness.beans.ScopeLevel;
import io.harness.exception.EngineFunctorException;
import io.harness.network.SafeHttpCall;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.variable.dto.VariableResponseDTO;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.execution.expression.SdkFunctor;
import io.harness.variable.remote.VariableClient;

import com.google.inject.Inject;
import java.io.IOException;

public class NGVariableFunctor implements SdkFunctor {
  public static final String NG_VARIABLE = "variable";
  @Inject VariableClient variableClient;
  @Override
  public String get(Ambiance ambiance, String... args) {
    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    Scope ambianceScope =
        Scope.of(ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());
    String scope = (args.length > 1) ? args[0] : "project";
    ScopeLevel variableScopeLevel = getVariableScope(scope);
    validateAccess(ScopeLevel.of(ambianceScope), variableScopeLevel);
    String identifier = args[args.length - 1];
    ResponseDTO<VariableResponseDTO> variableResponseDTO;
    try {
      switch (variableScopeLevel) {
        case ACCOUNT:
          variableResponseDTO =
              SafeHttpCall.execute(variableClient.getVariable(identifier, ngAccess.getAccountIdentifier(), null, null));
          break;
        case ORGANIZATION:
          variableResponseDTO = SafeHttpCall.execute(variableClient.getVariable(
              identifier, ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), null));
          break;
        case PROJECT:
        default:
          variableResponseDTO = SafeHttpCall.execute(variableClient.getVariable(identifier,
              ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier()));
          break;
      }
      return variableResponseDTO.getData().getVariable().getVariableConfig().getValue();
    } catch (IOException e) {
      throw new EngineFunctorException(String.format("Unable to fetch value of variable %s", identifier), e);
    }
  }

  private void validateAccess(ScopeLevel ambianceScopeLevel, ScopeLevel variableScopeLevel) {
    if (variableScopeLevel.ordinal() > ambianceScopeLevel.ordinal()) {
      throw new IllegalArgumentException(
          String.format("Variable of %s scope cannot be used at %s scope", variableScopeLevel, ambianceScopeLevel));
    }
  }

  private ScopeLevel getVariableScope(String scope) {
    switch (scope) {
      case "account":
        return ScopeLevel.ACCOUNT;
      case "org":
        return ScopeLevel.ORGANIZATION;
      default:
        return ScopeLevel.PROJECT;
    }
  }
}
