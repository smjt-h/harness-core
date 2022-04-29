/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.remote;

import static io.harness.rule.OwnerRule.BOJAN;
import static io.harness.rule.OwnerRule.IVAN;
import static io.harness.rule.OwnerRule.VLAD;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.beans.SearchPageParams;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.filestore.FileDTO;
import io.harness.ng.core.dto.filestore.filter.FilesFilterPropertiesDTO;
import io.harness.ng.core.dto.filestore.node.FolderNodeDTO;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.filestore.service.FileStoreServiceImpl;
import io.harness.rule.Owner;

import com.google.api.client.util.Lists;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import javax.ws.rs.core.Response;
import jersey.repackaged.com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@OwnedBy(HarnessTeam.CDP)
@RunWith(MockitoJUnitRunner.class)
public class FileStoreResourceTest extends CategoryTest {
  private static final String ACCOUNT = "account";
  private static final String ORG = "org";
  private static final String PROJECT = "project";
  private static final String IDENTIFIER = "testFile";

  @Mock private FileStoreServiceImpl fileStoreService;

  private FileStoreResource fileStoreResource;

  @Before
  public void setup() {
    fileStoreResource = new FileStoreResource(fileStoreService);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldDeleteFile() {
    fileStoreResource.delete(ACCOUNT, ORG, PROJECT, IDENTIFIER);
    verify(fileStoreService).delete(ACCOUNT, ORG, PROJECT, IDENTIFIER);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testDownloadFile() {
    File file = new File("returnedFile-download-path");
    when(fileStoreService.downloadFile(ACCOUNT, ORG, PROJECT, IDENTIFIER)).thenReturn(file);
    Response response = fileStoreResource.downloadFile(ACCOUNT, ORG, PROJECT, IDENTIFIER);
    File returnedFile = (File) response.getEntity();

    assertThat(returnedFile).isNotNull();
    assertThat(returnedFile.getPath()).isEqualTo(file.getPath());
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testDownloadFileWithException() {
    when(fileStoreService.downloadFile(ACCOUNT, ORG, PROJECT, IDENTIFIER))
        .thenThrow(new InvalidRequestException("Unable to download file"));

    assertThatThrownBy(() -> fileStoreResource.downloadFile(ACCOUNT, ORG, PROJECT, IDENTIFIER))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Unable to download file");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListFolderNodes() {
    FolderNodeDTO folderDTO = FolderNodeDTO.builder().build();
    when(fileStoreService.listFolderNodes(ACCOUNT, ORG, PROJECT, folderDTO))
        .thenReturn(FolderNodeDTO.builder().name("returnedFolderName").identifier("returnedFolderIdentifier").build());
    ResponseDTO<FolderNodeDTO> folderNodeDTOResponseDTO =
        fileStoreResource.listFolderNodes(ACCOUNT, ORG, PROJECT, folderDTO);
    FolderNodeDTO data = folderNodeDTOResponseDTO.getData();

    assertThat(data).isNotNull();
    assertThat(data.getName()).isEqualTo("returnedFolderName");
    assertThat(data.getIdentifier()).isEqualTo("returnedFolderIdentifier");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListFolderNodesWithException() {
    FolderNodeDTO folderDTO = FolderNodeDTO.builder().build();
    when(fileStoreService.listFolderNodes(ACCOUNT, ORG, PROJECT, folderDTO))
        .thenThrow(new InvalidRequestException("Unable to list folder nodes"));

    assertThatThrownBy(() -> fileStoreResource.listFolderNodes(ACCOUNT, ORG, PROJECT, folderDTO))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Unable to list folder nodes");
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldListReferencedBy() {
    EntitySetupUsageDTO entitySetupUsage = EntitySetupUsageDTO.builder().build();
    int page = 1;
    int size = 10;
    SearchPageParams pageParams = SearchPageParams.builder().page(page).size(size).build();
    final Page<EntitySetupUsageDTO> entityServiceUsageList =
        new PageImpl<>(Collections.singletonList(entitySetupUsage));
    when(fileStoreService.listReferencedBy(pageParams, ACCOUNT, ORG, PROJECT, IDENTIFIER, EntityType.PIPELINES))
        .thenReturn(entityServiceUsageList);
    ResponseDTO<Page<EntitySetupUsageDTO>> response =
        fileStoreResource.getReferencedBy(page, size, ACCOUNT, ORG, PROJECT, IDENTIFIER, EntityType.PIPELINES, null);
    verify(fileStoreService).listReferencedBy(pageParams, ACCOUNT, ORG, PROJECT, IDENTIFIER, EntityType.PIPELINES);
    assertThat(response).isNotNull();
    assertThat(response.getData().getContent()).containsExactly(entitySetupUsage);
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void testListFilesWithFilter() {
    ArrayList<FileDTO> fileDTOS = Lists.newArrayList();
    fileDTOS.add(FileDTO.builder().name("file1").build());
    when(fileStoreService.listFilesWithFilter(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(new PageImpl<>(fileDTOS));

    ResponseDTO<Page<FileDTO>> response = fileStoreResource.listFilesWithFilter(
        PageRequest.builder().pageSize(1).pageIndex(0).build(), ACCOUNT, ORG, PROJECT, "", "", null);
    Page<FileDTO> pageResponse = response.getData();

    assertThat(pageResponse).isNotNull();
    assertThat(pageResponse.isEmpty()).isFalse();
    assertThat(pageResponse.getContent().get(0).getName()).isEqualTo("file1");
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void testListFilesWithFilterException() {
    when(fileStoreService.listFilesWithFilter(any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new InvalidRequestException("Can not apply both filter properties and saved filter together"));

    assertThatThrownBy(
        ()
            -> fileStoreResource.listFilesWithFilter(PageRequest.builder().pageSize(1).pageIndex(0).build(), ACCOUNT,
                ORG, PROJECT, "filterIdentifier", "", new FilesFilterPropertiesDTO()))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Can not apply both filter properties and saved filter together");
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void testListCreatedByUserNames() {
    when(fileStoreService.getCreatedByList(any(), any(), any()))
        .thenReturn(Sets.newHashSet("test@test.com", "test1@test.com"));

    ResponseDTO<Set<String>> response = fileStoreResource.getCreatedByList(ACCOUNT, ORG, PROJECT);
    Set<String> returnedList = response.getData();

    assertThat(returnedList).isNotNull();
    assertThat(returnedList.isEmpty()).isFalse();
    assertThat(returnedList.size()).isEqualTo(2);
  }
}
