package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.api.FileStoreService;

import software.wings.service.intfc.FileService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(CDP)
public class FileStoreServiceImpl implements FileStoreService {
  private final FileService fileService;

  @Inject
  public FileStoreServiceImpl(FileService fileService) {
    this.fileService = fileService;
  }
}
