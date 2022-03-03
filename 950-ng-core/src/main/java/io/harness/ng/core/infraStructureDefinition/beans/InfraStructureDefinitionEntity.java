package io.harness.ng.core.infraStructureDefinition.beans;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.PersistentEntity;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(PIPELINE)
@Data
@Builder
@Entity(value = "infraStructureNG", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "InfraStructureKeys")
@Document("infraStructureNG")
@TypeAlias("io.harness.ng.core.infraStructureDefinition.beans.infraStructureDefinitionEntity")
public class InfraStructureDefinitionEntity implements PersistentEntity {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("unique_accountId_organizationIdentifier_projectIdentifier_infraIdentifier")
                 .unique(true)
                 .field(InfraStructureDefinitionEntity.InfraStructureKeys.accountId)
                 .field(InfraStructureDefinitionEntity.InfraStructureKeys.orgIdentifier)
                 .field(InfraStructureDefinitionEntity.InfraStructureKeys.projectIdentifier)
                 .field(InfraStructureDefinitionEntity.InfraStructureKeys.identifier)
                 .build())
        .build();
  }

  @Wither @Id @org.mongodb.morphia.annotations.Id private String id;

  @Trimmed @NotEmpty private String accountId;
  @Trimmed private String orgIdentifier;
  @Trimmed private String projectIdentifier;

  @NotEmpty @EntityIdentifier private String identifier;
  @EntityName private String name;

  @NotNull String yaml;
  @Trimmed @NotNull String environmentIdentifier;

  @Wither @CreatedDate Long createdAt;
  @Wither @LastModifiedDate Long lastModifiedAt;
  @Wither @Version Long version;
  @Builder.Default Boolean deleted = Boolean.FALSE;
}
