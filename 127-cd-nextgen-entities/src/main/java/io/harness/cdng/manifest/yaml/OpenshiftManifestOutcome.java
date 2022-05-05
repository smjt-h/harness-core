/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.manifest.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@OwnedBy(CDP)
@JsonTypeName(ManifestType.OpenshiftTemplate)
@TypeAlias("openshiftManifestOutcome")
@FieldNameConstants(innerTypeName = "OpenshiftManifestOutcomeKeys")
@RecasterAlias("io.harness.cdng.manifest.yaml.OpenshiftManifestOutcome")
public class OpenshiftManifestOutcome implements ManifestOutcome {
  String identifier;
  String type = ManifestType.OpenshiftTemplate;
  StoreConfig store;
  ParameterField<Boolean> skipResourceVersioning;
}
