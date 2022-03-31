/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.mapper;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.file.beans.NGBaseFile;
import io.harness.ng.core.dto.filestore.FileDTO;
import io.harness.ng.core.entities.NGFile;

import lombok.experimental.UtilityClass;

@OwnedBy(CDP)
@UtilityClass
public class FileDTOMapper {
  // add all other methods for mapping

  public NGFile getNGFileFromDTO(FileDTO dto, NGBaseFile baseFile) {
    return NGFile.builder().build();
  }

  public FileDTO getFileDTOFromNGFile(NGFile ngFile) {
    return FileDTO.builder().build();
  }
}
