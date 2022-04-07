/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.api.impl;

import static io.harness.rule.OwnerRule.BOJAN;
import static io.harness.rule.OwnerRule.FILIP;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ChecksumType;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.FileUploadLimit;
import io.harness.exception.DuplicateEntityException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.file.beans.NGBaseFile;
import io.harness.ng.core.dto.filestore.FileDTO;
import io.harness.ng.core.dto.filestore.NGFileType;
import io.harness.ng.core.entities.NGFile;
import io.harness.repositories.filestore.spring.FileStoreRepository;
import io.harness.rule.Owner;

import software.wings.app.MainConfiguration;
import software.wings.service.intfc.FileService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DuplicateKeyException;

@OwnedBy(HarnessTeam.CDP)
public class FileStoreServiceImplCreateTest extends CategoryTest {
  @Mock private FileStoreRepository fileStoreRepository;

  @Mock private FileUploadLimit fileUploadLimit;

  @Mock private FileService fileService;

  @Mock private MainConfiguration configuration;

  @InjectMocks private FileStoreServiceImpl fileStoreService;

  @Before
  public void setup() {
    initMocks(this);
    when(configuration.getFileUploadLimits()).thenReturn(new FileUploadLimit());

    when(fileStoreRepository.save(any())).thenAnswer(invocation -> invocation.getArguments()[0]);
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldSaveNgFile() {
    // Given
    final FileDTO fileDto = aFileDto();

    // When
    fileStoreService.create(fileDto, getStreamWithDummyContent());

    // Then
    NGFile expected = NGFile.builder()
                          .identifier(fileDto.getIdentifier())
                          .accountIdentifier(fileDto.getAccountIdentifier())
                          .description(fileDto.getDescription())
                          .fileName(fileDto.getName())
                          .type(fileDto.getType())
                          .checksumType(ChecksumType.MD5)
                          .build();

    verify(fileStoreRepository).save(expected);
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldSaveFileUsingFileService() {
    // Given
    final FileDTO fileDto = aFileDto();

    // When
    fileStoreService.create(fileDto, getStreamWithDummyContent());

    // Then
    NGBaseFile baseFile = new NGBaseFile();
    baseFile.setFileName(fileDto.getName());
    baseFile.setAccountId(fileDto.getAccountIdentifier());

    verify(fileService).saveFile(eq(baseFile), notNull(InputStream.class), eq(FileBucket.FILE_STORE));
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldNotInvokeSaveFileOnFileServiceForFolder() {
    // Given
    final FileDTO folderDto = aFolderDto();

    // When
    fileStoreService.create(folderDto, null);

    // Then
    verifyZeroInteractions(fileService);
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldHandleDuplicateKeyExceptionForFolder() {
    when(fileStoreRepository.save(any())).thenThrow(DuplicateKeyException.class);

    FileDTO folderDto = aFolderDto();

    assertThatThrownBy(() -> fileStoreService.create(folderDto, null))
        .isInstanceOf(DuplicateEntityException.class)
        .hasMessageContaining(
            "Try creating another folder, folder with identifier [%s] already exists in the parent folder",
            folderDto.getIdentifier());
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldHandleDuplicateKeyExceptionForFile() {
    when(fileStoreRepository.save(any())).thenThrow(DuplicateKeyException.class);

    FileDTO fileDTO = aFileDto();

    assertThatThrownBy(() -> fileStoreService.create(fileDTO, getStreamWithDummyContent()))
        .isInstanceOf(DuplicateEntityException.class)
        .hasMessageContaining(
            "Try creating another file, file with identifier [%s] already exists in the parent folder",
            fileDTO.getIdentifier());
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void shouldHandleEmptyFileExceptionForFile() {
    FileDTO fileDTO = aFileDto();

    assertThatThrownBy(() -> fileStoreService.create(fileDTO, null))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessageContaining("File content is empty. Identifier: " + fileDTO.getIdentifier(), fileDTO.getIdentifier());
  }

  private static FileDTO aFileDto() {
    return FileDTO.builder()
        .identifier("identifier")
        .accountIdentifier("account-ident")
        .description("some description")
        .name("file-name")
        .type(NGFileType.FILE)
        .build();
  }

  private static FileDTO aFolderDto() {
    return FileDTO.builder()
        .identifier("identifier")
        .accountIdentifier("account-ident")
        .description("some description")
        .name("folder-name")
        .type(NGFileType.FOLDER)
        .build();
  }

  private static InputStream getStreamWithDummyContent() {
    return new ByteArrayInputStream("File content".getBytes());
  }
}
