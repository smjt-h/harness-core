/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.filestore.api.impl;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.VLAD;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.beans.SearchPageParams;
import io.harness.ng.core.entities.NGFile;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;
import io.harness.ng.core.filestore.NGFileType;
import io.harness.repositories.filestore.spring.FileStoreRepository;
import io.harness.rule.Owner;

import java.util.Arrays;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

@OwnedBy(CDP)
@RunWith(MockitoJUnitRunner.class)
public class FileReferenceServiceTest extends CategoryTest {
  private static final String ACCOUNT_IDENTIFIER = "accountIdentifier";

  @Mock private EntitySetupUsageService entitySetupUsageService;
  @Mock private FileStoreRepository fileStoreRepository;

  @InjectMocks private FileReferenceServiceImpl fileReferenceService;

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldVerifyFileNotReferencedByOtherEntities() {
    NGFile file = NGFile.builder().identifier("testFile").accountIdentifier(ACCOUNT_IDENTIFIER).build();
    Long result = fileReferenceService.countEntitiesReferencingFile(file);
    assertThat(result).isEqualTo(0l);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldVerifyFileIsReferencedByOtherEntities() {
    String identifier = "testFile";
    NGFile file = NGFile.builder().identifier(identifier).accountIdentifier(ACCOUNT_IDENTIFIER).build();
    Long count = 123l;
    when(entitySetupUsageService.referredByEntityCount(
             ACCOUNT_IDENTIFIER, ACCOUNT_IDENTIFIER + "/" + identifier, EntityType.FILES))
        .thenReturn(count);
    Long result = fileReferenceService.countEntitiesReferencingFile(file);
    assertThat(result).isEqualTo(count);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldFetchReferencedBy() {
    String identifier = "testFile";
    NGFile file = NGFile.builder().identifier(identifier).accountIdentifier(ACCOUNT_IDENTIFIER).build();
    io.harness.ng.core.beans.SearchPageParams searchPageParams = SearchPageParams.builder().page(1).size(10).build();
    Page<EntitySetupUsageDTO> references = mock(Page.class);
    when(entitySetupUsageService.listAllEntityUsage(searchPageParams.getPage(), searchPageParams.getSize(),
             file.getAccountIdentifier(), ACCOUNT_IDENTIFIER + "/" + identifier, EntityType.FILES, EntityType.PIPELINES,
             searchPageParams.getSearchTerm(),
             Sort.by(Sort.Direction.ASC, FileReferenceServiceImpl.REFERRED_BY_IDENTIFIER_KEY)))
        .thenReturn(references);
    Page<EntitySetupUsageDTO> result =
        fileReferenceService.getReferencedBy(searchPageParams, file, EntityType.PIPELINES);
    assertThat(result).isEqualTo(references);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldVerifyFolderIsReferencedByOtherEntities() {
    String identifier1 = "testFolder1";
    NGFile folder1 =
        NGFile.builder().identifier(identifier1).accountIdentifier(ACCOUNT_IDENTIFIER).type(NGFileType.FOLDER).build();
    String identifier2 = "testFolder2";
    NGFile folder2 = NGFile.builder()
                         .identifier(identifier2)
                         .accountIdentifier(ACCOUNT_IDENTIFIER)
                         .parentIdentifier(identifier1)
                         .type(NGFileType.FOLDER)
                         .build();
    Long count1 = 123L;
    Long count2 = 234L;
    when(entitySetupUsageService.referredByEntityCount(
             ACCOUNT_IDENTIFIER, ACCOUNT_IDENTIFIER + "/" + identifier1, EntityType.FILES))
        .thenReturn(count1);
    when(entitySetupUsageService.referredByEntityCount(
             ACCOUNT_IDENTIFIER, ACCOUNT_IDENTIFIER + "/" + identifier2, EntityType.FILES))
        .thenReturn(count2);
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifier(
             folder1.getAccountIdentifier(), folder1.getOrgIdentifier(), folder1.getProjectIdentifier(),
             folder1.getIdentifier()))
        .thenReturn(Arrays.asList(folder2));

    assertThatThrownBy(() -> fileReferenceService.validateIsReferencedBy(folder1))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("Folder [testFolder1], or its subfolders, contain file(s) referenced by " + (count1 + count2)
            + " other entities and can not be deleted.");
  }
}
