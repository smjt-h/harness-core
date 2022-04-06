/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.dto.filestore;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;
import io.harness.ng.core.common.beans.NGTag;

import software.wings.beans.EntityType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "File", description = "This is details of the file entity defined in Harness.")
public class FileDTO {
  @ApiModelProperty(required = true)
  @NotBlank
  @Schema(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE)
  private String accountIdentifier;
  @EntityIdentifier(allowBlank = true)
  @Schema(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE)
  private String orgIdentifier;
  @EntityIdentifier(allowBlank = true)
  @Schema(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE)
  private String projectIdentifier;

  @NotNull
  @EntityIdentifier
  @Schema(description = "Identifier of the File")
  @FormDataParam("identifier")
  private String identifier;

  @NotNull @NGEntityName @Schema(description = "Name of the File") @FormDataParam("name") private String name;
  @NotNull
  @Schema(description = "This specifies the file usage")
  @FormDataParam("fileUsage")
  private FileUsage fileUsage;
  @NotNull @Schema(description = "This specifies the type of the File") @FormDataParam("type") private NGFileType type;
  @NotNull
  @Schema(description = "This specifies parent identifier")
  @FormDataParam("parentIdentifier")
  private String parentIdentifier;
  @Schema(description = "Description of the File") @FormDataParam("description") private String description;
  @Schema(description = "Tags") @FormDataParam("tags") private List<NGTag> tags;
  @NotNull @Schema(description = "Content of the File") @FormDataParam("entityType") private EntityType entityType;
  @NotEmpty @Schema(description = "Content of the File") @FormDataParam("entityId") private String entityId;

  @Builder
  public FileDTO(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier,
      String name, FileUsage fileUsage, NGFileType type, String parentIdentifier, String description, List<NGTag> tags,
      EntityType entityType, String entityId) {
    this.accountIdentifier = accountIdentifier;
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
    this.identifier = identifier;
    this.name = name;
    this.fileUsage = fileUsage;
    this.type = type;
    this.parentIdentifier = parentIdentifier;
    this.description = description;
    this.tags = tags;
    this.entityType = entityType;
    this.entityId = entityId;
  }

  @JsonIgnore
  public boolean isFile() {
    return type == NGFileType.FILE;
  }
}
