/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.variable.mappers;

import io.harness.ng.core.variable.VariableType;
import io.harness.ng.core.variable.dto.VariableConfigDTO;
import io.harness.ng.core.variable.dto.VariableDTO;
import io.harness.ng.core.variable.dto.VariableResponseDTO;
import io.harness.ng.core.variable.entity.Variable;

import com.google.inject.Inject;
import java.util.Map;

public class VariableMapper {
  @Inject private Map<VariableType, VariableDTOToEntityMapper> variableDTOToEntityMapperMap;
  @Inject private Map<VariableType, VariableEntityToDTOMapper> variableEntityToDTOMapperMap;

  public Variable toVariable(String accountIdentifier, VariableDTO variableDTO) {
    VariableDTOToEntityMapper mapper = variableDTOToEntityMapperMap.get(variableDTO.getType());
    Variable variable = mapper.toVariableEntity(variableDTO.getVariableConfig());
    variable.setIdentifier(variableDTO.getIdentifier());
    variable.setAccountIdentifier(accountIdentifier);
    variable.setOrgIdentifier(variableDTO.getOrgIdentifier());
    variable.setProjectIdentifier(variableDTO.getProjectIdentifier());
    variable.setName(variableDTO.getName());
    variable.setDescription(variableDTO.getDescription());
    variable.setType(variableDTO.getType());
    variable.setValueType(variableDTO.getVariableConfig().getVariableValueType());
    return variable;
  }

  public VariableDTO writeDTO(Variable variable) {
    VariableEntityToDTOMapper mapper = variableEntityToDTOMapperMap.get(variable.getType());
    VariableConfigDTO variableConfigDTO = mapper.createVariableDTO(variable);
    return VariableDTO.builder()
        .identifier(variable.getIdentifier())
        .name(variable.getName())
        .description(variable.getDescription())
        .orgIdentifier(variable.getOrgIdentifier())
        .projectIdentifier(variable.getProjectIdentifier())
        .type(variable.getType())
        .variableConfig(variableConfigDTO)
        .build();
  }

  public VariableResponseDTO toResponseWrapper(Variable variable) {
    return VariableResponseDTO.builder()
        .variable(writeDTO(variable))
        .createdAt(variable.getCreatedAt())
        .lastModifiedAt(variable.getLastModifiedAt())
        .build();
  }
}
