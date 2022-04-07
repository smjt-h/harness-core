/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.api.impl;

import static io.harness.rule.OwnerRule.BOJAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.dto.filestore.FileDTO;
import io.harness.ng.core.dto.filestore.NGFileType;
import io.harness.ng.core.entities.NGFile;
import io.harness.repositories.filestore.spring.FileStoreRepository;
import io.harness.rule.Owner;

import software.wings.service.intfc.FileService;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.CDP)
public class FileStoreServiceImplTest extends CategoryTest {
  private static final String FILE_ID = "fileId";
  @Mock private FileStoreRepository fileStoreRepository;
  @Mock private FileService fileService;
  @InjectMocks private FileStoreServiceImpl fileStoreService;

  @Before
  public void setup() {
    initMocks(this);
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void testUpdate() {
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             any(), any(), any(), any()))
        .thenReturn(Optional.of(createNgFile()));

    FileDTO result = fileStoreService.update(createFileDto(), null, "identifier1");

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("updatedName");
    assertThat(result.getDescription()).isEqualTo("updatedDescription");
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void testUpdateShouldThrowException() {
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             any(), any(), any(), any()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> fileStoreService.update(createFileDto(), null, "identifier1"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("File with identifier: identifier1 not found.");
  }

  private FileDTO createFileDto() {
    return FileDTO.builder()
        .type(NGFileType.FILE)
        .entityId(FILE_ID)
        .identifier("identifier1")
        .name("updatedName")
        .description("updatedDescription")
        .build();
  }
  private NGFile createNgFile() {
    return NGFile.builder()
        .type(NGFileType.FILE)
        .entityId(FILE_ID)
        .fileName("oldName")
        .description("oldDescription")
        .identifier("identifier1")
        .build();
  }
}
