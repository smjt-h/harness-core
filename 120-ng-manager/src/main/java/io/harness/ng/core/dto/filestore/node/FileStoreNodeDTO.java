/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.dto.filestore.node;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.filestore.NGFileType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = FolderNodeDTO.class, name = "FolderNode")
  , @JsonSubTypes.Type(value = FileNodeDTO.class, name = "FileNode")
})
@OwnedBy(HarnessTeam.CDP)
@Schema(name = "FileStoreNode", description = "This is the view of the file store node entity defined in Harness")
public abstract class FileStoreNodeDTO {
  @NotNull @Schema(description = "Identifier of the File Store Node") protected String identifier;
  @NotNull @Schema(description = "Name of the File Store Node") protected String name;
  @NotNull @Schema(description = "Type of the File Store Node") protected NGFileType type;
  @Schema(description = "Last modified time for the File Store Node") protected Long lastModifiedAt;
  @Schema(description = "This is the user who last modified the File Store Node") protected String lastModifiedBy;

  protected FileStoreNodeDTO(
      NGFileType type, String identifier, String name, Long lastModifiedAt, String lastModifiedBy) {
    this.type = type;
    this.identifier = identifier;
    this.name = name;
    this.lastModifiedAt = lastModifiedAt;
    this.lastModifiedBy = lastModifiedBy;
  }
}
