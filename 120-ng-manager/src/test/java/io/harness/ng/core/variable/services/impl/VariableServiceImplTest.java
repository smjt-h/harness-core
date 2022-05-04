/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.variable.services.impl;

import static io.harness.ng.core.variable.VariableType.STRING;
import static io.harness.ng.core.variable.VariableValueType.FIXED;
import static io.harness.ng.core.variable.VariableValueType.FIXED_SET;
import static io.harness.ng.core.variable.VariableValueType.REGEX;
import static io.harness.rule.OwnerRule.NISHANT;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.variable.VariableValueType;
import io.harness.ng.core.variable.dto.StringVariableConfigDTO;
import io.harness.ng.core.variable.dto.StringVariableConfigDTO.StringVariableConfigDTOKeys;
import io.harness.ng.core.variable.dto.VariableDTO;
import io.harness.ng.core.variable.dto.VariableResponseDTO;
import io.harness.ng.core.variable.entity.StringVariable;
import io.harness.ng.core.variable.entity.Variable;
import io.harness.ng.core.variable.mappers.VariableMapper;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.variable.spring.VariableRepository;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class VariableServiceImplTest extends CategoryTest {
  @Mock private VariableRepository variableRepository;
  @Mock private VariableMapper variableMapper;
  @Mock private TransactionTemplate transactionTemplate;
  @Mock private OutboxService outboxService;
  private VariableServiceImpl variableService;

  @Rule public ExpectedException exceptionRule = ExpectedException.none();
  //@Captor private ArgumentCaptor<VariableCreateEvent> variableCreateEventArgumentCaptor;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.variableService =
        new VariableServiceImpl(variableRepository, variableMapper, transactionTemplate, outboxService);
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testCreateValidateDTO_stringFixedValueMissingValue() {
    VariableDTO variableDTO = VariableDTO.builder()
                                  .type(STRING)
                                  .variableConfig(StringVariableConfigDTO.builder().valueType(FIXED).build())
                                  .build();
    exceptionRule.expect(InvalidRequestException.class);
    exceptionRule.expectMessage(String.format("Value for field [%s] must be provide when value type is [%s]",
        StringVariableConfigDTOKeys.fixedValue, VariableValueType.FIXED));
    variableDTO.getVariableConfig().validate();
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testCreateValidateDTO_stringFixedSetMissingAllowedValues() {
    VariableDTO variableDTO = VariableDTO.builder()
                                  .type(STRING)
                                  .variableConfig(StringVariableConfigDTO.builder().valueType(FIXED_SET).build())
                                  .build();
    exceptionRule.expect(InvalidRequestException.class);
    exceptionRule.expectMessage(String.format("Value(s) for field [%s] must be provide when value type is [%s]",
        StringVariableConfigDTOKeys.allowedValues, FIXED_SET));
    variableDTO.getVariableConfig().validate();
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testCreateValidateDTO_stringRegexMissingRegex() {
    VariableDTO variableDTO = VariableDTO.builder()
                                  .type(STRING)
                                  .variableConfig(StringVariableConfigDTO.builder().valueType(REGEX).build())
                                  .build();
    exceptionRule.expect(InvalidRequestException.class);
    exceptionRule.expectMessage(String.format(
        "Value for field [%s] must be provide when value type is [%s]", StringVariableConfigDTOKeys.regex, REGEX));
    variableDTO.getVariableConfig().validate();
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testCreateValidateDTO_stringRegexInvalidRegex() {
    String regex = "[a-z]\\i";
    VariableDTO variableDTO =
        VariableDTO.builder()
            .type(STRING)
            .variableConfig(StringVariableConfigDTO.builder().valueType(REGEX).regex(regex).build())
            .build();
    exceptionRule.expect(InvalidRequestException.class);
    exceptionRule.expectMessage(String.format("[%s] is not a valid regex", regex));
    variableDTO.getVariableConfig().validate();
  }

  private VariableDTO getVariableDTO(String identifier, String orgIdentifier, String projectIdentifier, String value) {
    return VariableDTO.builder()
        .identifier(identifier)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .variableConfig(StringVariableConfigDTO.builder().valueType(FIXED).fixedValue(value).build())
        .build();
  }

  private VariableResponseDTO getVariableResponseDTO(
      String identifier, String orgIdentifier, String projectIdentifier, String value) {
    return VariableResponseDTO.builder()
        .variable(getVariableDTO(identifier, orgIdentifier, projectIdentifier, value))
        .build();
  }

  private Variable getVariable(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier, String value) {
    Variable variable = StringVariable.builder().fixedValue(value).build();
    variable.setAccountIdentifier(accountIdentifier);
    variable.setOrgIdentifier(orgIdentifier);
    variable.setProjectIdentifier(projectIdentifier);
    variable.setIdentifier(identifier);
    variable.setValueType(FIXED);
    variable.setType(STRING);
    return variable;
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testCreate() {
    String identifier = randomAlphabetic(10);
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    String value = randomAlphabetic(10);
    VariableDTO variableDTO = getVariableDTO(identifier, orgIdentifier, projectIdentifier, value);
    Variable variable = getVariable(accountIdentifier, orgIdentifier, projectIdentifier, identifier, value);

    when(variableMapper.toVariable(accountIdentifier, variableDTO)).thenReturn(variable);
    when(variableMapper.writeDTO(variable)).thenReturn(variableDTO);
    when(variableRepository.save(variable)).thenReturn(variable);
    when(transactionTemplate.execute(any()))
        .thenAnswer(invocationOnMock
            -> invocationOnMock.getArgumentAt(0, TransactionCallback.class)
                   .doInTransaction(new SimpleTransactionStatus()));

    variableService.create(accountIdentifier, variableDTO);
    verify(variableMapper, times(1)).toVariable(accountIdentifier, variableDTO);
    verify(transactionTemplate, times(1)).execute(any());
    verify(variableRepository, times(1)).save(variable);
    // verify(outboxService, times(1)).save(variableCreateEventArgumentCaptor.capture());
    // VariableCreateEvent capturedVariableCreateEvent = variableCreateEventArgumentCaptor.getValue();
    // assertEquals(variableDTO, capturedVariableCreateEvent.getVariableDTO());
  }

  @Test(expected = DuplicateFieldException.class)
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testCreate_duplicateKeyException() {
    String identifier = randomAlphabetic(10);
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    String value = randomAlphabetic(10);
    VariableDTO variableDTO = getVariableDTO(identifier, orgIdentifier, projectIdentifier, value);
    Variable variable = getVariable(accountIdentifier, orgIdentifier, projectIdentifier, identifier, value);
    when(variableMapper.toVariable(accountIdentifier, variableDTO)).thenReturn(variable);
    when(variableRepository.save(variable)).thenThrow(new DuplicateKeyException(""));
    when(transactionTemplate.execute(any()))
        .thenAnswer(invocationOnMock
            -> invocationOnMock.getArgumentAt(0, TransactionCallback.class)
                   .doInTransaction(new SimpleTransactionStatus()));
    variableService.create(accountIdentifier, variableDTO);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testCreate_invalidRequestException() {
    String identifier = randomAlphabetic(10);
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    VariableDTO variableDTO = VariableDTO.builder()
                                  .identifier(identifier)
                                  .orgIdentifier(orgIdentifier)
                                  .projectIdentifier(projectIdentifier)
                                  .build();
    variableService.create(accountIdentifier, variableDTO);
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testList() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    String var1 = randomAlphabetic(5);
    Variable varA = getVariable(accountIdentifier, orgIdentifier, projectIdentifier, var1, var1);
    VariableDTO varADTO = getVariableDTO(var1, orgIdentifier, projectIdentifier, var1);
    List<Variable> variables = new ArrayList<>(Collections.singletonList(varA));
    when(variableRepository.findAllByAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
             anyString(), anyString(), anyString()))
        .thenReturn(variables);
    when(variableMapper.writeDTO(varA)).thenReturn(varADTO);
    List<VariableDTO> varList = variableService.list(accountIdentifier, orgIdentifier, projectIdentifier);
    verify(variableRepository, times(1))
        .findAllByAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier);
    assertThat(varList).hasOnlyElementsOfType(VariableDTO.class);
    assertThat(varList.size()).isEqualTo(variables.size());
    assertThat(varList).contains(varADTO);
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testGet() {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(5);
    String value = randomAlphabetic(7);
    Variable variable = getVariable(accountIdentifier, orgIdentifier, projectIdentifier, identifier, value);
    VariableResponseDTO variableResponseDTO =
        getVariableResponseDTO(identifier, orgIdentifier, projectIdentifier, value);
    when(variableRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Optional.of(variable));
    when(variableMapper.toResponseWrapper(variable)).thenReturn(variableResponseDTO);
    assertThat(variableService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier)).isNotNull();
    verify(variableRepository, times(1))
        .findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    verify(variableMapper, times(1)).toResponseWrapper(variable);
  }
}
