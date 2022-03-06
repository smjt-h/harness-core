/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.manifest.yaml.kinds;

import static io.harness.beans.SwaggerConstants.STRING_CLASSPATH;
import static io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper.StoreConfigWrapperParameters;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.cdng.visitor.helpers.manifest.ServerlessAwsManifestVisitorHelper;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.SkipAutoEvaluation;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.Wither;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ManifestType.ServerlessAws)
@FieldDefaults(level = AccessLevel.PRIVATE)
@SimpleVisitorHelper(helperClass = ServerlessAwsManifestVisitorHelper.class)
@TypeAlias("serverlessAwsManifest")
@RecasterAlias("io.harness.cdng.manifest.yaml.kinds.ServerlessAwsManifest")
public class ServerlessAwsManifest implements ManifestAttributes, Visitable {
  @EntityIdentifier String identifier;

  @Wither
  @JsonProperty("store")
  @ApiModelProperty(dataType = "io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper")
  @SkipAutoEvaluation
  ParameterField<StoreConfigWrapper> store;

  @Wither @ApiModelProperty(dataType = STRING_CLASSPATH) @SkipAutoEvaluation ParameterField<String> configOverridePath;
  // todo: check with sainath for better validation type
  // For Visitor Framework Impl
  String metadata;

  // todo: check usage of ServerlessAwsManifestVisitorHelper and ServerlessAwsManifestStepParameters

  @Override
  public String getKind() {
    return ManifestType.ServerlessAws;
  }

  @Override
  public StoreConfig getStoreConfig() {
    return store.getValue().getSpec();
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add(YAMLFieldNameConstants.STORE, store.getValue());
    return children;
  }

  @Override
  public ManifestAttributeStepParameters getManifestAttributeStepParameters() {
    return new ServerlessAwsManifestStepParameters(
        identifier, StoreConfigWrapperParameters.fromStoreConfigWrapper(store.getValue()), configOverridePath);
  }

  @Override
  public ManifestAttributes applyOverrides(ManifestAttributes overrideConfig) {
    ServerlessAwsManifest serverlessAwsManifest = (ServerlessAwsManifest) overrideConfig;
    ServerlessAwsManifest resultantManifest = this;
    if (serverlessAwsManifest.getStore() != null && serverlessAwsManifest.getStore().getValue() != null) {
      resultantManifest = resultantManifest.withStore(ParameterField.createValueField(
          store.getValue().applyOverrides(serverlessAwsManifest.getStore().getValue())));
    }
    if (serverlessAwsManifest.getConfigOverridePath() != null) {
      resultantManifest = resultantManifest.withConfigOverridePath(serverlessAwsManifest.getConfigOverridePath());
    }
    return resultantManifest;
  }

  @Value
  public static class ServerlessAwsManifestStepParameters implements ManifestAttributeStepParameters {
    String identifier;
    StoreConfigWrapperParameters store;
    ParameterField<String> configOverridePath;
  }
}
