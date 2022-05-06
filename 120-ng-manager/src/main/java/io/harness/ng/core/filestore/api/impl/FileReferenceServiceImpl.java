/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.filestore.api.impl;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.core.EntityDetail.EntityDetailKeys;
import static io.harness.ng.core.Resource.ResourceKeys;
import static io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage.EntitySetupUsageKeys;

import static java.lang.String.format;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.UnexpectedException;
import io.harness.ng.core.beans.SearchPageParams;
import io.harness.ng.core.entities.NGFile;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import io.harness.ng.core.entitysetupusage.service.EntitySetupUsageService;
import io.harness.ng.core.filestore.NGFileType;
import io.harness.ng.core.filestore.api.FileReferenceService;
import io.harness.repositories.filestore.spring.FileStoreRepository;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

@OwnedBy(CDP)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class FileReferenceServiceImpl implements FileReferenceService {
  public static final String REFERRED_BY_IDENTIFIER_KEY =
      EntitySetupUsageKeys.referredByEntity + "." + EntityDetailKeys.entityRef + "." + ResourceKeys.identifier;

  private final EntitySetupUsageService entitySetupUsageService;
  private final FileStoreRepository fileStoreRepository;

  @Override
  public Long countEntitiesReferencingFile(NGFile file) {
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRefFromEntityIdentifiers(
        file.getIdentifier(), file.getAccountIdentifier(), file.getOrgIdentifier(), file.getProjectIdentifier());
    String referredEntityFQN = identifierRef.getFullyQualifiedName();
    try {
      return entitySetupUsageService.referredByEntityCount(
          file.getAccountIdentifier(), referredEntityFQN, EntityType.FILES);
    } catch (Exception ex) {
      log.error("Encountered exception while requesting the Entity Reference records of [{}], with exception.",
          file.getIdentifier(), ex);
      throw new UnexpectedException("Error while verifying file is referenced by other entities.", ex);
    }
  }

  @Override
  public Page<EntitySetupUsageDTO> getReferencedBy(SearchPageParams pageParams, NGFile file, EntityType entityType) {
    IdentifierRef identifierRef = IdentifierRef.builder()
                                      .accountIdentifier(file.getAccountIdentifier())
                                      .orgIdentifier(file.getOrgIdentifier())
                                      .projectIdentifier(file.getProjectIdentifier())
                                      .identifier(file.getIdentifier())
                                      .build();
    String referredEntityFQN = identifierRef.getFullyQualifiedName();
    return entitySetupUsageService.listAllEntityUsage(pageParams.getPage(), pageParams.getSize(),
        file.getAccountIdentifier(), referredEntityFQN, EntityType.FILES, entityType, pageParams.getSearchTerm(),
        Sort.by(Sort.Direction.ASC, REFERRED_BY_IDENTIFIER_KEY));
  }

  @Override
  public Long validateIsReferencedBy(NGFile fileOrFolder) {
    Long count = countEntitiesReferencingFile(fileOrFolder);
    if (NGFileType.FOLDER.equals(fileOrFolder.getType())) {
      count += anyFileInFolderHasReferences(fileOrFolder);
      if (count > 0L) {
        throw new InvalidArgumentsException(format(
            "Folder [%s], or its subfolders, contain file(s) referenced by %s other entities and can not be deleted.",
            fileOrFolder.getIdentifier(), count));
      }
    } else {
      if (count > 0L) {
        throw new InvalidArgumentsException(
            format("File [%s] is referenced by %s other entities and can not be deleted.", fileOrFolder.getIdentifier(),
                count));
      }
    }
    return count;
  }

  private long anyFileInFolderHasReferences(NGFile folder) {
    List<NGFile> childrenFiles = listFilesByParent(folder);
    if (isEmpty(childrenFiles)) {
      return 0L;
    }
    return childrenFiles.stream()
        .filter(Objects::nonNull)
        .map(this::countEntitiesReferencingFile)
        .reduce((i, j) -> i + j)
        .orElse(0L);
  }

  private List<NGFile> listFilesByParent(NGFile parent) {
    return fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifier(
        parent.getAccountIdentifier(), parent.getOrgIdentifier(), parent.getProjectIdentifier(),
        parent.getIdentifier());
  }
}
