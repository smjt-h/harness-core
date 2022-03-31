/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.beans.FileBucket.CONFIGS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.UUIDGenerator;
import io.harness.file.beans.NGBaseFile;
import io.harness.ng.core.api.FileStoreService;
import io.harness.ng.core.dto.filestore.FileDTO;
import io.harness.repositories.filestore.spring.FileStoreRepository;

import software.wings.service.intfc.FileService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.validation.Valid;

@Singleton
@OwnedBy(CDP)
public class FileStoreServiceImpl implements FileStoreService {
  private final FileService fileService;
  private final FileStoreRepository fileStoreRepository;

  @Inject
  public FileStoreServiceImpl(FileService fileService, FileStoreRepository fileStoreRepository) {
    this.fileService = fileService;
    this.fileStoreRepository = fileStoreRepository;
  }

  @Override
  public FileDTO create(@Valid FileDTO fileDto) {
    // save entities into configs.files and configs.files by using fileService
    NGBaseFile baseFile = new NGBaseFile();
    baseFile.setFileName(fileDto.getName());
    baseFile.setAccountId(fileDto.getAccountIdentifier());
    baseFile.setFileUuid(UUIDGenerator.generateUuid());
    String fileId = fileService.saveFile(
        baseFile, new ByteArrayInputStream(fileDto.getContent().getBytes(StandardCharsets.UTF_8)), CONFIGS);
    // use mapper to create NGFile from fileDto and NGBaseFile
    // save NGFile into nfFile by using fileStoreRepository or fileStoreRepositoryCustom
    return null;
  }

  @Override
  public FileDTO get(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    // get entity from DB by using fileStoreRepository or fileStoreRepositoryCustom
    return null;
  }

  @Override
  public FileDTO update(@Valid FileDTO fileDto) {
    // update entities in configs.files and configs.files by using fileService
    // update entity in DB by using fileStoreRepository or fileStoreRepositoryCustom
    return null;
  }

  @Override
  public boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    // delete entities in configs.files and configs.files by using fileService
    // delete entity in DB by using fileStoreRepository or fileStoreRepositoryCustom
    return false;
  }
}
