/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.dto.filestore.node;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.filestore.NGFileType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(CDP)
@Schema(name = "FolderNode", description = "This contains folder details")
public final class FolderNodeDTO extends FileStoreNodeDTO {
  @Schema(description = "Node children") private final List<FileStoreNodeDTO> children = new ArrayList<>();

  @Builder
  public FolderNodeDTO(String identifier, String name, Long lastModifiedAt, String lastModifiedBy) {
    super(NGFileType.FOLDER, identifier, name, lastModifiedAt, lastModifiedBy);
  }

  public FileStoreNodeDTO addChild(FileStoreNodeDTO child) {
    children.add(child);
    return child;
  }
}
