/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.infra.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.yaml.InfrastructureKind;
import io.harness.steps.environment.EnvironmentOutcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonTypeName(InfrastructureKind.KUBERNETES_GCP)
@TypeAlias("cdng.infra.beans.K8sGcpInfrastructureOutcome")
@OwnedBy(HarnessTeam.CDP)
@RecasterAlias("io.harness.cdng.infra.beans.K8sGcpInfrastructureOutcome")
public class K8sGcpInfrastructureOutcome implements InfrastructureOutcome {
  String connectorRef;
  String namespace;
  String cluster;
  String releaseName;
  EnvironmentOutcome environment;
  String infrastructureKey;

  @Override
  public String getKind() {
    return InfrastructureKind.KUBERNETES_GCP;
  }
}
