/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.filestore.api.impl;

import static io.harness.ng.core.entities.NGFile.builder;
import static io.harness.repositories.filestore.FileStoreRepositoryCriteriaCreator.createCriteriaByScopeAndParentIdentifier;
import static io.harness.repositories.filestore.FileStoreRepositoryCriteriaCreator.createFilesFilterCriteria;
import static io.harness.repositories.filestore.FileStoreRepositoryCriteriaCreator.createSortByLastModifiedAtDesc;
import static io.harness.rule.OwnerRule.BOJAN;
import static io.harness.rule.OwnerRule.FILIP;
import static io.harness.rule.OwnerRule.IVAN;
import static io.harness.rule.OwnerRule.VLAD;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.FileStoreConstants;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ChecksumType;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.FileUploadLimit;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.file.beans.NGBaseFile;
import io.harness.ng.core.dto.filestore.CreatedBy;
import io.harness.ng.core.dto.filestore.filter.FilesFilterPropertiesDTO;
import io.harness.ng.core.dto.filestore.node.FileNodeDTO;
import io.harness.ng.core.dto.filestore.node.FolderNodeDTO;
import io.harness.ng.core.entities.NGFile;
import io.harness.ng.core.filestore.NGFileType;
import io.harness.ng.core.filestore.api.FileFailsafeService;
import io.harness.ng.core.filestore.dto.FileDTO;
import io.harness.ng.core.filestore.dto.FileFilterDTO;
import io.harness.ng.core.mapper.FileDTOMapper;
import io.harness.repositories.filestore.spring.FileStoreRepository;
import io.harness.rule.Owner;

import software.wings.app.MainConfiguration;
import software.wings.service.intfc.FileService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jersey.repackaged.com.google.common.collect.Lists;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.CDP)
@RunWith(MockitoJUnitRunner.class)
public class FileStoreServiceImplTest extends CategoryTest {
  private static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
  private static final String ORG_IDENTIFIER = "orgIdentifier";
  private static final String PROJECT_IDENTIFIER = "projectIdentifier";
  private static final String IDENTIFIER = "identifier";
  private static final String FILE_IDENTIFIER = "fileIdentifier";
  private static final String FILE_NAME = "fileName";

  @Mock private FileStoreRepository fileStoreRepository;
  @Mock private FileService fileService;
  @Mock private MainConfiguration configuration;
  @Mock private FileReferenceServiceImpl fileReferenceService;
  @Mock private FileFailsafeService fileFailsafeService;

  @InjectMocks private FileStoreServiceImpl fileStoreService;

  @Before
  public void setup() {
    when(configuration.getFileUploadLimits()).thenReturn(new FileUploadLimit());

    when(fileStoreRepository.save(any())).thenAnswer(invocation -> invocation.getArguments()[0]);

    givenThatDatabaseIsEmpty();

    when(fileReferenceService.countEntitiesReferencingFile(any())).thenReturn(0l);
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void testUpdate() {
    NGFile ngFile = createNgFile();
    FileDTO fileDto = createFileDto();
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             any(), any(), any(), any()))
        .thenReturn(Optional.of(ngFile));
    when(fileFailsafeService.updateAndPublish(ngFile, FileDTOMapper.updateNGFile(fileDto, ngFile))).thenReturn(fileDto);

    FileDTO result = fileStoreService.update(fileDto, null, "identifier1");

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
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage(
            "Not found file/folder with identifier [identifier1], accountIdentifier [null], orgIdentifier [null] and projectIdentifier [null]");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testDownloadFileWithNullIdentifier() {
    assertThatThrownBy(
        () -> fileStoreService.downloadFile(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, null))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessageContaining("File identifier cannot be null or empty");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testDownloadFileWithNotExistingFile() {
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, FILE_IDENTIFIER))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> fileStoreService.downloadFile(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, FILE_IDENTIFIER))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessageContaining(format(
            "Not found file/folder with identifier [%s], accountIdentifier [%s], orgIdentifier [%s] and projectIdentifier [%s]",
            FILE_IDENTIFIER, ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER));
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testDownloadFileWithFolder() {
    NGFile ngFile = createNgFile();
    ngFile.setType(NGFileType.FOLDER);
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, FILE_IDENTIFIER))
        .thenReturn(Optional.of(ngFile));

    assertThatThrownBy(
        () -> fileStoreService.downloadFile(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, FILE_IDENTIFIER))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessageContaining(format("Downloading folder not supported, fileIdentifier: %s", FILE_IDENTIFIER));
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testDownloadFile() {
    NGFile ngFile = createNgFile();
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, FILE_IDENTIFIER))
        .thenReturn(Optional.of(ngFile));
    File fileToReturn = new File("download-file-path");
    when(fileService.download(eq(ngFile.getFileUuid()), any(File.class), any(FileBucket.class)))
        .thenReturn(fileToReturn);

    File file = fileStoreService.downloadFile(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, FILE_IDENTIFIER);

    assertThat(file).isNotNull();
    assertThat(file.getPath()).isEqualTo(fileToReturn.getPath());
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldSaveNgFile() {
    // Given
    givenThatFileNotExistInDB();
    final FileDTO fileDto = aFileDto();

    // When
    fileStoreService.create(fileDto, getStreamWithDummyContent(), false);

    // Then
    NGFile expected = builder()
                          .identifier(fileDto.getIdentifier())
                          .accountIdentifier(fileDto.getAccountIdentifier())
                          .description(fileDto.getDescription())
                          .name(fileDto.getName())
                          .type(fileDto.getType())
                          .checksumType(ChecksumType.MD5)
                          .draft(false)
                          .tags(Collections.emptyList())
                          .size(0L)
                          .parentIdentifier(fileDto.getParentIdentifier())
                          .build();

    verify(fileFailsafeService).saveAndPublish(expected);
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldSaveFileUsingFileService() {
    // Given
    givenThatFileNotExistInDB();
    final FileDTO fileDto = aFileDto();

    // When
    fileStoreService.create(fileDto, getStreamWithDummyContent(), false);

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
    givenThatFileNotExistInDB();
    final FileDTO folderDto = aFolderDto();

    // When
    fileStoreService.create(folderDto, null, false);

    // Then
    verifyZeroInteractions(fileService);
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldNotInvokeSaveFileOnFileServiceForDraftFile() {
    // Given
    givenThatFileNotExistInDB();
    final FileDTO fileDto = aFileDto();

    // When
    fileStoreService.create(fileDto, getStreamWithDummyContent(), true);

    // Then
    verifyZeroInteractions(fileService);
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldSaveDraftNgFile() {
    // Given
    givenThatFileNotExistInDB();
    final FileDTO fileDto = aFileDto();

    // When
    fileStoreService.create(fileDto, getStreamWithDummyContent(), true);

    // Then
    NGFile expected = NGFile.builder()
                          .identifier(fileDto.getIdentifier())
                          .accountIdentifier(fileDto.getAccountIdentifier())
                          .description(fileDto.getDescription())
                          .name(fileDto.getName())
                          .type(fileDto.getType())
                          .draft(true)
                          .tags(Collections.emptyList())
                          .parentIdentifier(fileDto.getParentIdentifier())
                          .build();

    verify(fileFailsafeService).saveAndPublish(expected);
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldThrowExceptionWhenFolderAlreadyExistsInDatabase() {
    givenThatFileExistsInDatabase();

    FileDTO folderDto = aFolderDto();

    assertThatThrownBy(() -> fileStoreService.create(folderDto, null, false))
        .isInstanceOf(DuplicateFieldException.class)
        .hasMessageContaining("Try creating another folder, folder with identifier [identifier] already exists.",
            folderDto.getIdentifier());
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldHandleDuplicateIdentifierExceptionForFile() {
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             any(), any(), any(), any()))
        .thenReturn(Optional.of(builder().build()));
    FileDTO fileDTO = aFileDto();

    assertThatThrownBy(() -> fileStoreService.create(fileDTO, getStreamWithDummyContent(), false))
        .isInstanceOf(DuplicateFieldException.class)
        .hasMessageContaining("Try creating another file, file with identifier [identifier] already exists.");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldHandleDuplicateNameExceptionForFile() {
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifierAndName(
             any(), any(), any(), any(), any()))
        .thenReturn(Optional.of(builder().build()));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             any(), any(), any(), any()))
        .thenReturn(Optional.empty());
    FileDTO fileDTO = aFileDto();

    assertThatThrownBy(() -> fileStoreService.create(fileDTO, getStreamWithDummyContent(), false))
        .isInstanceOf(DuplicateFieldException.class)
        .hasMessageContaining(
            "Try creating another file, file with name [file-name] already exists in the parent folder [parent-identifier].",
            fileDTO.getIdentifier());
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldDeleteFileDoesNotExist() {
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifierAndName(
             any(), any(), any(), any(), any()))
        .thenReturn(Optional.empty());
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER))
        .thenReturn(Optional.empty());
    assertThatThrownBy(
        () -> fileStoreService.delete(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage(
            "Not found file/folder with identifier [identifier], accountIdentifier [accountIdentifier], orgIdentifier [orgIdentifier] and projectIdentifier [projectIdentifier]");
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldDeleteFile() {
    String fileUuid = "fileUUID";
    NGFile file = builder().name(IDENTIFIER).identifier(IDENTIFIER).fileUuid(fileUuid).build();
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER))
        .thenReturn(Optional.of(file));
    when(fileFailsafeService.deleteAndPublish(any())).thenReturn(true);
    boolean result = fileStoreService.delete(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER);
    assertThat(result).isTrue();
    verify(fileFailsafeService).deleteAndPublish(file);
    verify(fileService).deleteFile(fileUuid, FileBucket.FILE_STORE);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldDeleteFolder() {
    String fileUuid = "fileUUID";
    NGFile file = builder().name(IDENTIFIER).fileUuid(fileUuid).build();
    String folder1 = "folder1";
    NGFile parentFolder = builder()
                              .name(folder1)
                              .identifier(folder1)
                              .type(NGFileType.FOLDER)
                              .accountIdentifier(ACCOUNT_IDENTIFIER)
                              .orgIdentifier(ORG_IDENTIFIER)
                              .projectIdentifier(PROJECT_IDENTIFIER)
                              .build();
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER))
        .thenReturn(Optional.of(file));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, folder1))
        .thenReturn(Optional.of(parentFolder));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, folder1))
        .thenReturn(Arrays.asList(file));
    when(fileFailsafeService.deleteAndPublish(any())).thenReturn(true);
    boolean result = fileStoreService.delete(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, folder1);
    assertThat(result).isTrue();
    verify(fileFailsafeService).deleteAndPublish(file);
    verify(fileFailsafeService).deleteAndPublish(parentFolder);
    verify(fileService).deleteFile(fileUuid, FileBucket.FILE_STORE);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldNotDeleteFolderWithReferences() {
    String fileUuid = "fileUUID";
    NGFile file = builder().name(IDENTIFIER).fileUuid(fileUuid).build();
    String folder1 = "folder1";
    NGFile parentFolder = builder()
                              .name(folder1)
                              .identifier(folder1)
                              .type(NGFileType.FOLDER)
                              .accountIdentifier(ACCOUNT_IDENTIFIER)
                              .orgIdentifier(ORG_IDENTIFIER)
                              .projectIdentifier(PROJECT_IDENTIFIER)
                              .build();
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER))
        .thenReturn(Optional.of(file));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, folder1))
        .thenReturn(Optional.of(parentFolder));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, folder1))
        .thenReturn(Arrays.asList(file));
    when(fileFailsafeService.deleteAndPublish(any())).thenReturn(true);
    when(fileReferenceService.validateIsReferencedBy(parentFolder))
        .thenThrow(new InvalidArgumentsException(format(
            "Folder [%s], or its subfolders, contain file(s) referenced by %s other entities and can not be deleted.",
            parentFolder.getIdentifier(), 1L)));
    assertThatThrownBy(() -> fileStoreService.delete(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, folder1))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage(
            "Folder [folder1], or its subfolders, contain file(s) referenced by 1 other entities and can not be deleted.");
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldDeleteFolderWithSubfolder() {
    String fileUuid1 = "fileUUID1";
    NGFile file = builder().name(IDENTIFIER).identifier(IDENTIFIER).fileUuid(fileUuid1).build();
    String folder1 = "folder1";
    NGFile parentFolder = builder()
                              .name(folder1)
                              .identifier(folder1)
                              .type(NGFileType.FOLDER)
                              .accountIdentifier(ACCOUNT_IDENTIFIER)
                              .orgIdentifier(ORG_IDENTIFIER)
                              .projectIdentifier(PROJECT_IDENTIFIER)
                              .build();
    String folder2 = "folder2";
    NGFile childFolder = builder()
                             .name(folder2)
                             .identifier(folder2)
                             .type(NGFileType.FOLDER)
                             .accountIdentifier(ACCOUNT_IDENTIFIER)
                             .orgIdentifier(ORG_IDENTIFIER)
                             .projectIdentifier(PROJECT_IDENTIFIER)
                             .build();
    String file2 = "file2";
    String fileUuid2 = "fileUUID2";
    NGFile childFile = builder().name(file2).fileUuid(fileUuid2).build();
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER))
        .thenReturn(Optional.of(file));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, folder1))
        .thenReturn(Optional.of(parentFolder));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, folder2))
        .thenReturn(Optional.of(childFolder));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, file2))
        .thenReturn(Optional.of(childFile));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, folder1))
        .thenReturn(Arrays.asList(file, childFolder));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, folder2))
        .thenReturn(Arrays.asList(childFile));
    when(fileFailsafeService.deleteAndPublish(any())).thenReturn(true);
    boolean result = fileStoreService.delete(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, folder1);
    assertThat(result).isTrue();
    verify(fileFailsafeService).deleteAndPublish(file);
    verify(fileFailsafeService).deleteAndPublish(parentFolder);
    verify(fileFailsafeService).deleteAndPublish(childFile);
    verify(fileFailsafeService).deleteAndPublish(childFolder);
    verify(fileService).deleteFile(fileUuid1, FileBucket.FILE_STORE);
    verify(fileService).deleteFile(fileUuid2, FileBucket.FILE_STORE);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldDeleteFolderWithScopePrefix() {
    String fileUuid = "fileUUID";
    NGFile file = builder().name(IDENTIFIER).identifier(IDENTIFIER).fileUuid(fileUuid).build();
    String folder1 = "folder1";
    NGFile parentFolder = builder()
                              .name(folder1)
                              .identifier(folder1)
                              .type(NGFileType.FOLDER)
                              .accountIdentifier(ACCOUNT_IDENTIFIER)
                              .build();
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, null, null, IDENTIFIER))
        .thenReturn(Optional.of(file));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, null, null, folder1))
        .thenReturn(Optional.of(parentFolder));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifier(
             ACCOUNT_IDENTIFIER, null, null, folder1))
        .thenReturn(Arrays.asList(file));
    assertThatThrownBy(
        () -> fileStoreService.delete(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, "account." + folder1))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage(
            "Not found file/folder with identifier [account.folder1], accountIdentifier [accountIdentifier], orgIdentifier [orgIdentifier] and projectIdentifier [projectIdentifier]");
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldDeleteRootFolder() {
    String rootIdentifier = FileStoreConstants.ROOT_FOLDER_IDENTIFIER;
    assertThatThrownBy(
        () -> fileStoreService.delete(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, rootIdentifier))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("Root folder [Root] can not be deleted.");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListFolderNodes() {
    FolderNodeDTO folderNodeDTO = FolderNodeDTO.builder().identifier(FILE_IDENTIFIER).name(FILE_NAME).build();
    when(fileStoreRepository.findAllAndSort(
             createCriteriaByScopeAndParentIdentifier(
                 Scope.of(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER), FILE_IDENTIFIER),
             createSortByLastModifiedAtDesc()))
        .thenReturn(getNgFiles());

    FolderNodeDTO populatedFolderNodeDTO =
        fileStoreService.listFolderNodes(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, folderNodeDTO);

    assertThat(populatedFolderNodeDTO).isNotNull();
    assertThat(populatedFolderNodeDTO.getName()).isEqualTo(FILE_NAME);
    assertThat(populatedFolderNodeDTO.getIdentifier()).isEqualTo(FILE_IDENTIFIER);
    assertThat(populatedFolderNodeDTO.getChildren().size()).isEqualTo(3);
    assertThat(populatedFolderNodeDTO.getChildren())
        .contains(FileNodeDTO.builder().name("fileName").identifier("fileIdentifier").build(),
            FolderNodeDTO.builder().name("folderName1").identifier("folderIdentifier1").build(),
            FolderNodeDTO.builder().name("folderName2").identifier("folderIdentifier2").build());
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListFilesAndFolders() {
    FileFilterDTO fileFilterDTO =
        FileFilterDTO.builder().searchTerm("searchTerm").identifiers(Collections.emptyList()).build();
    final ArgumentCaptor<Criteria> criteriaArgumentCaptor = ArgumentCaptor.forClass(Criteria.class);

    when(fileStoreRepository.findAll(any(), any()))
        .thenReturn(new PageImpl<>(Lists.newArrayList(builder().name("filename1").build())));
    Page<FileDTO> pageResponse = fileStoreService.listFilesAndFolders(
        ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, fileFilterDTO, PageRequest.of(0, 10));

    verify(fileStoreRepository, times(1)).findAll(criteriaArgumentCaptor.capture(), any());

    assertThat(pageResponse).isNotNull();
    assertThat(pageResponse.getContent().size()).isEqualTo(1);
    assertThat(pageResponse.getContent().get(0).getName()).isEqualTo("filename1");

    Criteria criteria = criteriaArgumentCaptor.getValue();
    assertThat(criteria).isNotNull();
    assertThat(criteria.getCriteriaObject().toString())
        .isEqualTo(
            "Document{{accountIdentifier=accountIdentifier, orgIdentifier=orgIdentifier, projectIdentifier=projectIdentifier, $or=[Document{{name=searchTerm}}, Document{{identifier=searchTerm}}, Document{{tags.key=searchTerm}}, Document{{tags.value=searchTerm}}]}}");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListFilesWithFoldersException() {
    FileFilterDTO fileFilterDTO =
        FileFilterDTO.builder().searchTerm("searchTerm").identifiers(Collections.emptyList()).build();
    assertThatThrownBy(()
                           -> fileStoreService.listFilesAndFolders(
                               null, ORG_IDENTIFIER, PROJECT_IDENTIFIER, fileFilterDTO, PageRequest.of(0, 10)))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("Account identifier cannot be null or empty");
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void testListFilesWithoutFilterShouldReturnAll() {
    when(fileStoreRepository.findAllAndSort(
             createTestCriteriaForListFiles(null, ""), createSortByLastModifiedAtDesc(), PageRequest.of(0, 10)))
        .thenReturn(new PageImpl<>(Lists.newArrayList(builder().name("filename1").build())));
    Page<FileDTO> pageResponse = fileStoreService.listFilesWithFilter(
        ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, "", "", null, PageRequest.of(0, 10));

    assertThat(pageResponse).isNotNull();
    assertThat(pageResponse.getContent().size()).isEqualTo(1);
    assertThat(pageResponse.getContent().get(0).getName()).isEqualTo("filename1");
  }

  private Criteria createTestCriteriaForListFiles(FilesFilterPropertiesDTO filterProperties, String searchTerm) {
    Scope scope = Scope.of(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER);
    return createFilesFilterCriteria(scope, filterProperties, searchTerm);
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void testListFilesWithFilterException() {
    assertThatThrownBy(
        ()
            -> fileStoreService.listFilesWithFilter(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER,
                "filterIdentifier", "", new FilesFilterPropertiesDTO(), PageRequest.of(0, 10)))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Can not apply both filter properties and saved filter together");
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void testListCreatedByShouldReturnAll() {
    when(fileStoreRepository.aggregate(any(), any()))
        .thenReturn(new AggregationResults<>(
            Lists.newArrayList(new CreatedBy("testuser1"), new CreatedBy("testuser2")), new Document()));
    Set<String> createdByList =
        fileStoreService.getCreatedByList(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER);

    assertThat(createdByList).isNotNull();
    assertThat(createdByList.size()).isEqualTo(2);
    assertThat(createdByList.stream().findFirst().get()).isEqualTo("testuser1");
  }

  private static FileDTO aFileDto() {
    return FileDTO.builder()
        .identifier("identifier")
        .accountIdentifier("account-ident")
        .description("some description")
        .name("file-name")
        .type(NGFileType.FILE)
        .parentIdentifier("parent-identifier")
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

  private void givenThatFileExistsInDatabase() {
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Optional.of(builder().build()));
  }

  private void givenThatDatabaseIsEmpty() {
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Optional.empty());
  }

  private static InputStream getStreamWithDummyContent() {
    return new ByteArrayInputStream("File content".getBytes());
  }

  private FileDTO createFileDto() {
    return FileDTO.builder()
        .type(NGFileType.FILE)
        .identifier("identifier1")
        .name("updatedName")
        .description("updatedDescription")
        .build();
  }

  private NGFile createNgFile() {
    return builder()
        .type(NGFileType.FILE)
        .name("oldName")
        .description("oldDescription")
        .identifier("identifier1")
        .build();
  }

  private List<NGFile> getNgFiles() {
    return Arrays.asList(builder()
                             .type(NGFileType.FOLDER)
                             .name("folderName1")
                             .identifier("folderIdentifier1")
                             .parentIdentifier(FILE_IDENTIFIER)
                             .createdBy("testuser1")
                             .build(),
        builder()
            .type(NGFileType.FOLDER)
            .name("folderName2")
            .identifier("folderIdentifier2")
            .parentIdentifier(FILE_IDENTIFIER)
            .createdBy("testuser1")
            .build(),
        builder()
            .type(NGFileType.FILE)
            .name("fileName")
            .identifier("fileIdentifier")
            .parentIdentifier(FILE_IDENTIFIER)
            .createdBy("testuser2")
            .build());
  }

  private void givenThatFileNotExistInDB() {
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifierAndName(
             any(), any(), any(), any(), any()))
        .thenReturn(Optional.empty());
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             any(), any(), any(), any()))
        .thenReturn(Optional.empty());
  }
}
