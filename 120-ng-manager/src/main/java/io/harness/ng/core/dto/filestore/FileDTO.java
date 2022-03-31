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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.InputStream;
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

  @FormDataParam("identifier")
  @NotNull
  @EntityIdentifier
  @Schema(description = "Identifier of the File")
  private String identifier;
  @FormDataParam("name") @NotNull @NGEntityName @Schema(description = "Name of the File") private String name;
  @FormDataParam("type") @NotNull @Schema(description = "This specifies the type of File") private NGFileType type;
  @FormDataParam("description") @Schema(description = "Description of the File") private String description;
  @FormDataParam("tags") @Schema(description = "Tags") private List<NGTag> tags;
  @FormDataParam("entityType") @NotNull @Schema(description = "Content of the File") private EntityType entityType;
  @FormDataParam("entityId") @NotEmpty @Schema(description = "Content of the File") private String entityId;
  @FormDataParam("content") @Schema(description = "Content of the File") private InputStream content;

  @Builder
  public FileDTO(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier,
      String name, NGFileType type, String description, List<NGTag> tags, EntityType entityType, String entityId,
      InputStream content) {
    this.accountIdentifier = accountIdentifier;
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
    this.identifier = identifier;
    this.name = name;
    this.type = type;
    this.description = description;
    this.tags = tags;
    this.entityType = entityType;
    this.entityId = entityId;
    this.content = content;
  }
}
