/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.dto.filestore.node;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.filestore.NGFileType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = DirectoryNodeDTO.class, name = "DirectoryNode")
  , @JsonSubTypes.Type(value = FileNodeDTO.class, name = "FileNodeDTO")
})
@OwnedBy(HarnessTeam.CDP)
@Schema(name = "NodeDTO", description = "This is the view of the file node entity defined in Harness")
public abstract class NodeDTO {
  protected NGFileType type;
  protected List<NodeDTO> children = new ArrayList<>();

  protected NodeDTO(NGFileType type) {
    this.type = type;
  }
}
