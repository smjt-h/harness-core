/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.api.impl;

import static io.harness.rule.OwnerRule.BOJAN;
import static io.harness.rule.OwnerRule.VLAD;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.FileBucket;
import io.harness.exception.InvalidArgumentsException;
import io.harness.filestore.FileStoreConstants;
import io.harness.ng.core.dto.filestore.FileDTO;
import io.harness.ng.core.dto.filestore.NGFileType;
import io.harness.ng.core.entities.NGFile;
import io.harness.repositories.filestore.spring.FileStoreRepository;
import io.harness.rule.Owner;

import software.wings.service.intfc.FileService;

import java.util.Arrays;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.CDP)
public class FileStoreServiceImplTest extends CategoryTest {
  private static final String FILE_ID = "fileId";
  private static final String ACCOUNT = "account";
  private static final String ORG = "org";
  private static final String PROJECT = "project";
  private static final String IDENTIFIER = "testFile";

  @Mock private FileStoreRepository fileStoreRepository;
  @Mock private FileService fileService;
  @InjectMocks private io.harness.ng.core.api.impl.FileStoreServiceImpl fileStoreService;

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

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldDeleteFileDoesNotExist() {
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT, ORG, PROJECT, IDENTIFIER))
        .thenReturn(Optional.empty());
    assertThatThrownBy(() -> fileStoreService.delete(ACCOUNT, ORG, PROJECT, IDENTIFIER))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage(
            "File or folder with identifier [testFile], account [account], org [org] and project [project] could not be retrieved from file store.");
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldDeleteFile() {
    String fileUuid = "fileUUID";
    NGFile file = NGFile.builder().fileName(IDENTIFIER).identifier(IDENTIFIER).fileUuid(fileUuid).build();
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT, ORG, PROJECT, IDENTIFIER))
        .thenReturn(Optional.of(file));
    boolean result = fileStoreService.delete(ACCOUNT, ORG, PROJECT, IDENTIFIER);
    assertThat(result).isTrue();
    verify(fileStoreRepository).delete(file);
    verify(fileService).deleteFile(fileUuid, FileBucket.FILE_STORE);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldDeleteFolder() {
    String fileUuid = "fileUUID";
    NGFile file = NGFile.builder().fileName(IDENTIFIER).fileUuid(fileUuid).build();
    String folder1 = "folder1";
    NGFile parentFolder = NGFile.builder()
                              .fileName(folder1)
                              .identifier(folder1)
                              .type(NGFileType.FOLDER)
                              .accountIdentifier(ACCOUNT)
                              .orgIdentifier(ORG)
                              .projectIdentifier(PROJECT)
                              .build();
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT, ORG, PROJECT, IDENTIFIER))
        .thenReturn(Optional.of(file));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT, ORG, PROJECT, folder1))
        .thenReturn(Optional.of(parentFolder));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifier(
             ACCOUNT, ORG, PROJECT, folder1))
        .thenReturn(Arrays.asList(file));
    boolean result = fileStoreService.delete(ACCOUNT, ORG, PROJECT, folder1);
    assertThat(result).isTrue();
    verify(fileStoreRepository).delete(file);
    verify(fileStoreRepository).delete(parentFolder);
    verify(fileService).deleteFile(fileUuid, FileBucket.FILE_STORE);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldDeleteFolderWithSubfolder() {
    String fileUuid1 = "fileUUID1";
    NGFile file = NGFile.builder().fileName(IDENTIFIER).identifier(IDENTIFIER).fileUuid(fileUuid1).build();
    String folder1 = "folder1";
    NGFile parentFolder = NGFile.builder()
                              .fileName(folder1)
                              .identifier(folder1)
                              .type(NGFileType.FOLDER)
                              .accountIdentifier(ACCOUNT)
                              .orgIdentifier(ORG)
                              .projectIdentifier(PROJECT)
                              .build();
    String folder2 = "folder2";
    NGFile childFolder = NGFile.builder()
                             .fileName(folder2)
                             .identifier(folder2)
                             .type(NGFileType.FOLDER)
                             .accountIdentifier(ACCOUNT)
                             .orgIdentifier(ORG)
                             .projectIdentifier(PROJECT)
                             .build();
    String file2 = "file2";
    String fileUuid2 = "fileUUID2";
    NGFile childFile = NGFile.builder().fileName(file2).fileUuid(fileUuid2).build();
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT, ORG, PROJECT, IDENTIFIER))
        .thenReturn(Optional.of(file));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT, ORG, PROJECT, folder1))
        .thenReturn(Optional.of(parentFolder));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT, ORG, PROJECT, folder2))
        .thenReturn(Optional.of(childFolder));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT, ORG, PROJECT, file2))
        .thenReturn(Optional.of(childFile));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifier(
             ACCOUNT, ORG, PROJECT, folder1))
        .thenReturn(Arrays.asList(file, childFolder));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifier(
             ACCOUNT, ORG, PROJECT, folder2))
        .thenReturn(Arrays.asList(childFile));
    boolean result = fileStoreService.delete(ACCOUNT, ORG, PROJECT, folder1);
    assertThat(result).isTrue();
    verify(fileStoreRepository).delete(file);
    verify(fileStoreRepository).delete(parentFolder);
    verify(fileStoreRepository).delete(childFile);
    verify(fileStoreRepository).delete(childFolder);
    verify(fileService).deleteFile(fileUuid1, FileBucket.FILE_STORE);
    verify(fileService).deleteFile(fileUuid2, FileBucket.FILE_STORE);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldDeleteFolderWithScopePrefix() {
    String fileUuid = "fileUUID";
    NGFile file = NGFile.builder().fileName(IDENTIFIER).identifier(IDENTIFIER).fileUuid(fileUuid).build();
    String folder1 = "folder1";
    NGFile parentFolder = NGFile.builder()
                              .fileName(folder1)
                              .identifier(folder1)
                              .type(NGFileType.FOLDER)
                              .accountIdentifier(ACCOUNT)
                              .build();
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT, null, null, IDENTIFIER))
        .thenReturn(Optional.of(file));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT, null, null, folder1))
        .thenReturn(Optional.of(parentFolder));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifier(
             ACCOUNT, null, null, folder1))
        .thenReturn(Arrays.asList(file));
    boolean result = fileStoreService.delete(ACCOUNT, ORG, PROJECT, "account." + folder1);
    assertThat(result).isTrue();
    verify(fileStoreRepository).delete(file);
    verify(fileStoreRepository).delete(parentFolder);
    verify(fileService).deleteFile(fileUuid, FileBucket.FILE_STORE);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldDeleteRootFolder() {
    String rootIdentifier = FileStoreConstants.ROOT_FOLDER_IDENTIFIER;
    assertThatThrownBy(() -> fileStoreService.delete(ACCOUNT, ORG, PROJECT, rootIdentifier))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("File or folder with identifier [" + rootIdentifier + "] can not be deleted.");
  }
}
