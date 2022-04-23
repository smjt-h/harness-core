/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.variable.services;

import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.variable.dto.VariableDTO;
import io.harness.ng.core.variable.dto.VariableResponseDTO;
import io.harness.ng.core.variable.entity.Variable;

import java.util.List;
import java.util.Optional;

public interface VariableService {
  Variable create(String accountIdentifier, VariableDTO variableDTO);
  Optional<Variable> get(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
  VariableDTO getVariableInNearestScope(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
  List<Variable> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, List<String> variableIdentifier);
  List<Variable> get(String accountIdentifier, String orgIdentifier, String projectIdentifier);
  List<VariableDTO> listVariableDTOs(String accountIdentifier, String orgIdentifier, String projectIdentifier);

  PageResponse<VariableResponseDTO> list(String accountIdentifier, String orgIdentifier, String projectIdentifier, int page, int size, String searchTerm, boolean includeVariablesFromEverySubScope);
}
