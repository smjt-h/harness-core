/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.filestore.api.impl;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.exception.WingsException.USER;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.springdata.TransactionUtils.DEFAULT_TRANSACTION_RETRY_POLICY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.DuplicateFieldException;
import io.harness.ng.core.entities.NGFile;
import io.harness.ng.core.events.filestore.FileCreateEvent;
import io.harness.ng.core.events.filestore.FileDeleteEvent;
import io.harness.ng.core.events.filestore.FileUpdateEvent;
import io.harness.ng.core.filestore.api.FileFailsafeService;
import io.harness.ng.core.filestore.dto.FileDTO;
import io.harness.ng.core.mapper.FileDTOMapper;
import io.harness.outbox.api.OutboxService;
import io.harness.repositories.filestore.spring.FileStoreRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.serializer.HObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(CDP)
@Singleton
@Slf4j
public class FileFailsafeServiceImpl implements FileFailsafeService {
  private final RetryPolicy<Object> transactionRetryPolicy = DEFAULT_TRANSACTION_RETRY_POLICY;

  private final OutboxService outboxService;
  private final TransactionTemplate transactionTemplate;
  private final FileStoreRepository fileStoreRepository;

  @Inject
  public FileFailsafeServiceImpl(OutboxService outboxService,
      @Named(OUTBOX_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate,
      FileStoreRepository fileStoreRepository) {
    this.outboxService = outboxService;
    this.transactionTemplate = transactionTemplate;
    this.fileStoreRepository = fileStoreRepository;
  }

  @Override
  public FileDTO saveAndPublish(NGFile ngFile) {
    try {
      return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
        NGFile savedNgFile = fileStoreRepository.save(ngFile);
        FileDTO fileDTOFromSavedNGFile = FileDTOMapper.getFileDTOFromNGFile(savedNgFile);
        outboxService.save(new FileCreateEvent(ngFile.getAccountIdentifier(), fileDTOFromSavedNGFile));
        return fileDTOFromSavedNGFile;
      }));
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format("Try using another identifier, [%s] already exists", ngFile.getIdentifier()), USER, ex);
    }
  }

  @Override
  public FileDTO updateAndPublish(NGFile oldNGFile, NGFile newNGFile) {
    FileDTO oldFileDTOClone = (FileDTO) HObjectMapper.clone(FileDTOMapper.getFileDTOFromNGFile(oldNGFile));
    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      NGFile newNgFile = fileStoreRepository.save(newNGFile);
      FileDTO newFileDTO = FileDTOMapper.getFileDTOFromNGFile(newNgFile);
      outboxService.save(new FileUpdateEvent(newFileDTO.getAccountIdentifier(), newFileDTO, oldFileDTOClone));
      return newFileDTO;
    }));
  }

  @Override
  public boolean deleteAndPublish(NGFile ngFile) {
    try {
      return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
        fileStoreRepository.delete(ngFile);
        outboxService.save(
            new FileDeleteEvent(ngFile.getAccountIdentifier(), FileDTOMapper.getFileDTOFromNGFile(ngFile)));
        log.info("{} [{}] deleted.", ngFile.isFile() ? "File" : "Folder", ngFile.getName());
        return true;
      }));
    } catch (Exception ex) {
      log.error("Failed to delete {} [{}].", ngFile.isFile() ? "file" : "folder", ngFile.getName(), ex);
      return false;
    }
  }
}
