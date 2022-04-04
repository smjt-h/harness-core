/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;
import io.harness.file.beans.NGBaseFile;
import io.harness.mongo.CollationLocale;
import io.harness.mongo.CollationStrength;
import io.harness.mongo.index.Collation;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.core.NGAccountAccess;
import io.harness.ng.core.NGOrgAccess;
import io.harness.ng.core.NGProjectAccess;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.dto.filestore.FileUsage;
import io.harness.ng.core.dto.filestore.NGFileType;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import software.wings.beans.EntityType;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "NGFiles")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "ngFiles", noClassnameStored = true)
@Document("ngFiles")
@TypeAlias("ngFiles")
@HarnessEntity(exportable = true)
@OwnedBy(HarnessTeam.CDP)
public class NGFile
    extends NGBaseFile implements PersistentEntity, UuidAware, NGAccountAccess, NGOrgAccess, NGProjectAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_idx")
                 .field(NGFiles.accountIdentifier)
                 .field(NGFiles.orgIdentifier)
                 .field(NGFiles.projectIdentifier)
                 .field(NGFiles.parentIdentifier)
                 .field(NGFiles.identifier)
                 .unique(true)
                 .collation(
                     Collation.builder().locale(CollationLocale.ENGLISH).strength(CollationStrength.PRIMARY).build())
                 .build(),
            CompoundMongoIndex.builder()
                .name("list_files_idx")
                .field(NGFiles.accountIdentifier)
                .field(NGFiles.orgIdentifier)
                .field(NGFiles.projectIdentifier)
                .field(NGFiles.identifier)
                .field(NGFiles.parentIdentifier)
                .build())
        .build();
  }

  @org.springframework.data.annotation.Id @Id String uuid;
  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;

  @NotEmpty String accountIdentifier;
  @EntityIdentifier(allowBlank = true) String orgIdentifier;
  @EntityIdentifier(allowBlank = true) String projectIdentifier;

  @EntityIdentifier String identifier;
  @NGEntityName String name;
  @NotNull @Size(max = 1024) String description;
  @NotNull @Singular @Size(max = 128) List<NGTag> tags;
  @NotEmpty String parentId;
  @NotNull FileUsage fileUsage;
  @NotNull NGFileType type;
  @NotEmpty String parentIdentifier;
  @NotNull EntityType entityType;
  @NotEmpty String entityId;
}
