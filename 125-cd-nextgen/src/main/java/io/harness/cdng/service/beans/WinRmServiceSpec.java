/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.service.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSetWrapper;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.ManifestOverrideSetWrapper;
import io.harness.cdng.service.ServiceSpec;
import io.harness.cdng.variables.beans.NGVariableOverrideSetWrapper;
import io.harness.cdng.visitor.helpers.serviceconfig.WinRmServiceSpecVisitorHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ng.core.k8s.ServiceSpecType;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.VariableExpression;
import io.harness.yaml.core.variables.NGVariable;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonTypeName(ServiceSpecType.WINRM)
@SimpleVisitorHelper(helperClass = WinRmServiceSpecVisitorHelper.class)
@TypeAlias("WinRmServiceSpec")
@RecasterAlias("io.harness.cdng.service.beans.WinRmServiceSpec")
@OwnedBy(HarnessTeam.CDP)
public class WinRmServiceSpec implements ServiceSpec, Visitable {
  List<NGVariable> variables;
  ArtifactListConfig artifacts;
  List<ManifestConfigWrapper> manifests;

  @VariableExpression(skipVariableExpression = true) List<NGVariableOverrideSetWrapper> variableOverrideSets;
  @VariableExpression(skipVariableExpression = true) List<ArtifactOverrideSetWrapper> artifactOverrideSets;
  @VariableExpression(skipVariableExpression = true) List<ManifestOverrideSetWrapper> manifestOverrideSets;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Override
  public String getType() {
    return ServiceDefinitionType.WINRM.getYamlName();
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    if (EmptyPredicate.isNotEmpty(variables)) {
      variables.forEach(ngVariable -> children.add("variables", ngVariable));
    }

    children.add("artifacts", artifacts);
    if (EmptyPredicate.isNotEmpty(artifactOverrideSets)) {
      artifactOverrideSets.forEach(artifactOverrideSet -> children.add("artifactOverrideSets", artifactOverrideSet));
    }
    return children;
  }
}
