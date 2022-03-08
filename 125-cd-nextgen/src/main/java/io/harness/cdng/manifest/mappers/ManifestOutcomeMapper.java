/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.manifest.mappers;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.cdng.manifest.ManifestType.*;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.steps.ManifestStepParameters;
import io.harness.cdng.manifest.yaml.*;
import io.harness.cdng.manifest.yaml.kinds.*;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CDP)
public class ManifestOutcomeMapper {
  public List<ManifestOutcome> toManifestOutcome(
      List<ManifestAttributes> manifestAttributesList, ManifestStepParameters parameters) {
    return manifestAttributesList.stream()
        .map(manifest -> toManifestOutcome(manifest, parameters))
        .collect(Collectors.toCollection(LinkedList::new));
  }

  public ManifestOutcome toManifestOutcome(ManifestAttributes manifestAttributes, ManifestStepParameters parameters) {
    if (manifestAttributes.getStoreConfig() != null) {
      ManifestOutcomeValidator.validateStore(
          manifestAttributes.getStoreConfig(), manifestAttributes.getKind(), manifestAttributes.getIdentifier(), true);
    }

    switch (manifestAttributes.getKind()) {
      case K8Manifest:
        return getK8sOutcome(manifestAttributes);
      case VALUES:
        return getValuesOutcome(manifestAttributes, parameters);
      case HelmChart:
        return getHelmChartOutcome(manifestAttributes);
      case Kustomize:
        return getKustomizeOutcome(manifestAttributes);
      case KustomizePatches:
        return getKustomizePatchesOutcome(manifestAttributes, parameters);
      case OpenshiftTemplate:
        return getOpenshiftOutcome(manifestAttributes);
      case OpenshiftParam:
        return getOpenshiftParamOutcome(manifestAttributes, parameters);
      case ServerlessAwsLambda:
        return getServerlessAwsOutcome(manifestAttributes, parameters);
      default:
        throw new UnsupportedOperationException(
            format("Unknown Artifact Config type: [%s]", manifestAttributes.getKind()));
    }
  }

  private K8sManifestOutcome getK8sOutcome(ManifestAttributes manifestAttributes) {
    K8sManifest k8sManifest = (K8sManifest) manifestAttributes;

    return K8sManifestOutcome.builder()
        .identifier(k8sManifest.getIdentifier())
        .store(k8sManifest.getStoreConfig())
        .skipResourceVersioning(k8sManifest.getSkipResourceVersioning())
        .build();
  }

  private ValuesManifestOutcome getValuesOutcome(ManifestAttributes manifestAttributes, ManifestStepParameters params) {
    ValuesManifest attributes = (ValuesManifest) manifestAttributes;
    return ValuesManifestOutcome.builder()
        .identifier(attributes.getIdentifier())
        .store(attributes.getStoreConfig())
        .order(params.getOrder())
        .build();
  }

  private HelmChartManifestOutcome getHelmChartOutcome(ManifestAttributes manifestAttributes) {
    HelmChartManifest helmChartManifest = (HelmChartManifest) manifestAttributes;

    return HelmChartManifestOutcome.builder()
        .identifier(helmChartManifest.getIdentifier())
        .store(helmChartManifest.getStoreConfig())
        .chartName(helmChartManifest.getChartName())
        .chartVersion(helmChartManifest.getChartVersion())
        .helmVersion(helmChartManifest.getHelmVersion())
        .skipResourceVersioning(helmChartManifest.getSkipResourceVersioning())
        .commandFlags(helmChartManifest.getCommandFlags())
        .build();
  }

  private KustomizeManifestOutcome getKustomizeOutcome(ManifestAttributes manifestAttributes) {
    KustomizeManifest kustomizeManifest = (KustomizeManifest) manifestAttributes;
    return KustomizeManifestOutcome.builder()
        .identifier(kustomizeManifest.getIdentifier())
        .store(kustomizeManifest.getStoreConfig())
        .skipResourceVersioning(kustomizeManifest.getSkipResourceVersioning())
        .pluginPath(kustomizeManifest.getPluginPath())
        .build();
  }

  private KustomizePatchesManifestOutcome getKustomizePatchesOutcome(
      ManifestAttributes manifestAttributes, ManifestStepParameters params) {
    KustomizePatchesManifest attributes = (KustomizePatchesManifest) manifestAttributes;
    return KustomizePatchesManifestOutcome.builder()
        .identifier(attributes.getIdentifier())
        .store(attributes.getStoreConfig())
        .order(params.getOrder())
        .build();
  }

  private OpenshiftManifestOutcome getOpenshiftOutcome(ManifestAttributes manifestAttributes) {
    OpenshiftManifest openshiftManifest = (OpenshiftManifest) manifestAttributes;

    return OpenshiftManifestOutcome.builder()
        .identifier(openshiftManifest.getIdentifier())
        .store(openshiftManifest.getStoreConfig())
        .skipResourceVersioning(openshiftManifest.getSkipResourceVersioning())
        .build();
  }

  private OpenshiftParamManifestOutcome getOpenshiftParamOutcome(
      ManifestAttributes manifestAttributes, ManifestStepParameters params) {
    OpenshiftParamManifest attributes = (OpenshiftParamManifest) manifestAttributes;

    return OpenshiftParamManifestOutcome.builder()
        .identifier(attributes.getIdentifier())
        .store(attributes.getStoreConfig())
        .order(params.getOrder())
        .build();
  }

  private ServerlessAwsLambdaManifestOutcome getServerlessAwsOutcome(
      ManifestAttributes manifestAttributes, ManifestStepParameters param) {
    ServerlessAwsLambdaManifest attributes = (ServerlessAwsLambdaManifest) manifestAttributes;
    return ServerlessAwsLambdaManifestOutcome.builder()
        .identifier(attributes.getIdentifier())
        .store(attributes.getStoreConfig())
        .configOverridePath(attributes.getConfigOverridePath())
        .order(param.getOrder())
        .build();
  }
}
