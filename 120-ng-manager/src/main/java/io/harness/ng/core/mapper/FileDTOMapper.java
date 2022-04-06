/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.mapper;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.file.beans.NGBaseFile;
import io.harness.ng.core.dto.filestore.FileDTO;
import io.harness.ng.core.entities.NGFile;

import java.util.Collections;
import lombok.experimental.UtilityClass;

@OwnedBy(CDP)
@UtilityClass
public class FileDTOMapper {
  public NGFile getNGFileFromDTO(FileDTO fileDto, NGBaseFile baseFile) {
    return NGFile.builder()
        .accountIdentifier(fileDto.getAccountIdentifier())
        .orgIdentifier(fileDto.getOrgIdentifier())
        .projectIdentifier(fileDto.getProjectIdentifier())
        .identifier(fileDto.getIdentifier())
        .fileUsage(fileDto.getFileUsage())
        .type(fileDto.getType())
        .parentIdentifier(fileDto.getParentIdentifier())
        .description(fileDto.getDescription())
        .tags(!EmptyPredicate.isEmpty(fileDto.getTags()) ? fileDto.getTags() : Collections.emptyList())
        .entityId(fileDto.getEntityId())
        .entityType(fileDto.getEntityType())
        .fileUuid(baseFile.getFileUuid())
        .fileName(baseFile.getFileName())
        .checksumType(baseFile.getChecksumType())
        .checksum(baseFile.getChecksum())
        .size(baseFile.getSize())
        .build();
  }

  public FileDTO getFileDTOFromNGFile(NGFile ngFile) {
    return FileDTO.builder()
        .accountIdentifier(ngFile.getAccountIdentifier())
        .orgIdentifier(ngFile.getOrgIdentifier())
        .projectIdentifier(ngFile.getProjectIdentifier())
        .identifier(ngFile.getIdentifier())
        .name(ngFile.getFileName())
        .fileUsage(ngFile.getFileUsage())
        .type(ngFile.getType())
        .parentIdentifier(ngFile.getParentIdentifier())
        .description(ngFile.getDescription())
        .tags(ngFile.getTags())
        .entityType(ngFile.getEntityType())
        .entityId(ngFile.getEntityId())
        .build();
  }
}
