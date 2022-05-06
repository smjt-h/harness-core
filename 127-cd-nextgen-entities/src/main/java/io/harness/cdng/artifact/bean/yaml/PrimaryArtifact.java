/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.artifact.bean.yaml;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.visitor.helpers.artifact.ArtifactSpecWrapperVisitorHelper;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SimpleVisitorHelper(helperClass = ArtifactSpecWrapperVisitorHelper.class)
@TypeAlias("primaryArtifact")
@RecasterAlias("io.harness.cdng.artifact.bean.yaml.PrimaryArtifact")
public class PrimaryArtifact implements Visitable {
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  private String uuid;

  @NotNull @JsonProperty("type") ArtifactSourceType sourceType;

  @NotNull
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  ArtifactConfig spec;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  // Use Builder as Constructor then only external property(visible) will be filled.
  @Builder
  public PrimaryArtifact(String uuid, ArtifactSourceType sourceType, ArtifactConfig spec) {
    this.uuid = uuid;
    this.sourceType = sourceType;
    this.spec = spec;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren visitableChildren = VisitableChildren.builder().build();
    visitableChildren.add(YAMLFieldNameConstants.SPEC, spec);
    return visitableChildren;
  }

  @Value
  public static class PrimaryArtifactStepParameters {
    String type;
    ArtifactConfig spec;

    public static PrimaryArtifactStepParameters fromPrimaryArtifact(PrimaryArtifact artifact) {
      if (artifact == null) {
        return null;
      }
      return new PrimaryArtifactStepParameters(
          artifact.getSourceType() == null ? null : artifact.getSourceType().getDisplayName(), artifact.getSpec());
    }
  }
}
