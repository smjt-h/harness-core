/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.manifest.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.WithIdentifier;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@OwnedBy(CDP)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = K8sManifestOutcome.class, name = ManifestType.K8Manifest)
  , @JsonSubTypes.Type(value = ValuesManifestOutcome.class, name = ManifestType.VALUES),
      @JsonSubTypes.Type(value = HelmChartManifestOutcome.class, name = ManifestType.HelmChart),
      @JsonSubTypes.Type(value = KustomizeManifestOutcome.class, name = ManifestType.Kustomize),
      @JsonSubTypes.Type(value = KustomizePatchesManifestOutcome.class, name = ManifestType.KustomizePatches),
      @JsonSubTypes.Type(value = OpenshiftManifestOutcome.class, name = ManifestType.OpenshiftTemplate),
      @JsonSubTypes.Type(value = OpenshiftParamManifestOutcome.class, name = ManifestType.OpenshiftParam),
      @JsonSubTypes.Type(value = ServerlessAwsLambdaManifestOutcome.class, name = ManifestType.ServerlessAwsLambda)
})
public interface ManifestOutcome extends Outcome, WithIdentifier {
  String getIdentifier();
  String getType();
  StoreConfig getStore();

  default int getOrder() {
    return -1;
  }
}
