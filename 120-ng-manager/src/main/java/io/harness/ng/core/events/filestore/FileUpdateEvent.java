/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.events.filestore;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.audit.ResourceTypeConstants.FILE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.Event;
import io.harness.ng.core.AccountScope;
import io.harness.ng.core.OrgScope;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceConstants;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.filestore.dto.FileDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@OwnedBy(CDP)
@Getter
@NoArgsConstructor
public class FileUpdateEvent implements Event {
  public static final String FILE_UPDATED_EVENT = "FileUpdated";
  private FileDTO newFile;
  private FileDTO oldFile;
  private String accountIdentifier;

  @Builder
  public FileUpdateEvent(String accountIdentifier, FileDTO newFile, FileDTO oldFile) {
    this.newFile = newFile;
    this.oldFile = oldFile;
    this.accountIdentifier = accountIdentifier;
  }

  @JsonIgnore
  @Override
  public ResourceScope getResourceScope() {
    if (isNotEmpty(newFile.getOrgIdentifier())) {
      if (isEmpty(newFile.getProjectIdentifier())) {
        return new OrgScope(accountIdentifier, newFile.getOrgIdentifier());
      } else {
        return new ProjectScope(accountIdentifier, newFile.getOrgIdentifier(), newFile.getProjectIdentifier());
      }
    }
    return new AccountScope(accountIdentifier);
  }

  @JsonIgnore
  @Override
  public Resource getResource() {
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, newFile.getName());
    return Resource.builder().identifier(newFile.getIdentifier()).type(FILE).labels(labels).build();
  }

  @JsonIgnore
  @Override
  public String getEventType() {
    return FILE_UPDATED_EVENT;
  }
}
