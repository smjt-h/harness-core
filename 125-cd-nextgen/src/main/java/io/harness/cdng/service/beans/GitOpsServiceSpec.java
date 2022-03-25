package io.harness.cdng.service.beans;

import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSetWrapper;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.ManifestOverrideSetWrapper;
import io.harness.cdng.service.ServiceSpec;
import io.harness.cdng.variables.beans.NGVariableOverrideSetWrapper;
import io.harness.cdng.visitor.helpers.serviceconfig.GitOpsServiceSpecVisitorHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.variables.NGVariable;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonTypeName(ServiceSpecType.GITOPS)
@SimpleVisitorHelper(helperClass = GitOpsServiceSpecVisitorHelper.class)
@TypeAlias("gitOpsServiceSpec")
public class GitOpsServiceSpec implements ServiceSpec, Visitable {
  // For Visitor Framework Impl
  String metadata;

  List<NGVariable> variables;
  @Override
  public String getType() {
    return ServiceSpecType.GITOPS;
  }

  @Override
  public ArtifactListConfig getArtifacts() {
    return null;
  }

  @Override
  public List<ManifestConfigWrapper> getManifests() {
    return null;
  }

  @Override
  public List<ManifestOverrideSetWrapper> getManifestOverrideSets() {
    return null;
  }

  @Override
  public List<ArtifactOverrideSetWrapper> getArtifactOverrideSets() {
    return null;
  }

  @Override
  public List<NGVariableOverrideSetWrapper> getVariableOverrideSets() {
    return null;
  }

  @Override
  public List<NGVariable> getVariables() {
    return null;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    if (EmptyPredicate.isNotEmpty(variables)) {
      variables.forEach(ngVariable -> children.add("variables", ngVariable));
    }
    return children;
  }
}
