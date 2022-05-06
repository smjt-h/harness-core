/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.clienttools;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@RequiredArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public enum ClientTool {
  //  CF was not being handled like other client tools before, so will add it in a separate PR.
  //  Installation is more complex than with other tools and need to work with CD team to understand how to do it.
  //  These are only placeholders for now.
  //  CF("cf", "", "./client-tools/helm/", "./cf --version", "", ImmutableList.copyOf(CfVersion.values()),
  //  CfVersion.V7),
  HELM("helm", "public/shared/tools/helm/release/%s/bin/%s/amd64/helm", "./client-tools/helm/", "./helm version -c",
      "storage/harness-download/harness-helm/release/%s/bin/%s/amd64/helm", ImmutableList.copyOf(HelmVersion.values()),
      HelmVersion.V3_8),
  KUBECTL("kubectl", "public/shared/tools/kubectl/release/%s/bin/%s/amd64/kubectl", "./client-tools/kubectl/",
      "./kubectl version --short --client",
      "storage/harness-download/kubernetes-release/release/%s/bin/%s/amd64/kubectl",
      ImmutableList.copyOf(KubectlVersion.values()), KubectlVersion.V1_19),
  KUSTOMIZE("kustomize", "public/shared/tools/kustomize/release/%s/bin/%s/amd64/kustomize", "./client-tools/kustomize/",
      "./kustomize version --short", "storage/harness-download/harness-kustomize/release/%s/bin/%s/amd64/kustomize",
      ImmutableList.copyOf(KustomizeVersion.values()), KustomizeVersion.V4),
  OC("oc", "public/shared/tools/oc/release/%s/bin/%s/amd64/oc", "./client-tools/oc/", "./oc version --client",
      "storage/harness-download/harness-oc/release/%s/bin/%s/amd64/oc", ImmutableList.copyOf(OcVersion.values()),
      OcVersion.V4_2),
  SCM("scm", "public/shared/tools/scm/release/%s/bin/%s/amd64/scm", "./client-tools/scm/", "./scm --version",
      "storage/harness-download/harness-scm/release/%s/bin/%s/amd64/scm", ImmutableList.copyOf(ScmVersion.values()),
      ScmVersion.DEFAULT),
  TERRAFORM_CONFIG_INSPECT("terraform-config-inspect",
      "public/shared/tools/terraform-config-inspect/%s/%s/amd64/terraform-config-inspect",
      "./client-tools/tf-config-inspect", "./terraform-config-inspect",
      "storage/harness-download/harness-terraform-config-inspect/%s/%s/amd64/terraform-config-inspect",
      ImmutableList.copyOf(TerraformConfigInspectVersion.values()), TerraformConfigInspectVersion.V1_1),
  GO_TEMPLATE("go-template", "public/shared/tools/go-template/release/%s/bin/%s/amd64/go-template",
      "./client-tools/go-template/", "./go-template -v",
      "storage/harness-download/snapshot-go-template/release/%s/bin/%s/amd64/go-template",
      ImmutableList.copyOf(GoTemplateVersion.values()), GoTemplateVersion.V0_4),
  HARNESS_PYWINRM("harness-pywinrm", "public/shared/tools/harness-pywinrm/release/%s/bin/%s/amd64/harness-pywinrm",
      "./client-tools/harness-pywinrm/", "./harness-pywinrm -v",
      "storage/harness-download/snapshot-harness-pywinrm/release/%s/bin/%s/amd64/harness-pywinrm",
      ImmutableList.copyOf(HarnessPywinrmVersion.values()), HarnessPywinrmVersion.V0_4),
  CHARTMUSEUM("chartmuseum", "public/shared/tools/chartmuseum/release/%s/bin/%s/amd64/chartmuseum",
      "./client-tools/chartmuseum/", "./chartmuseum -v",
      "storage/harness-download/harness-chartmuseum/release/%s/bin/%s/amd64/chartmuseum",
      ImmutableList.copyOf(ChartmuseumVersion.values()), ChartmuseumVersion.V0_12);

  @ToString.Include private final String binaryName;
  private final String cdnPath;
  private final String baseDir;
  private final String validateCommand;
  private final String onPremPath;
  private final List<ClientToolVersion> versions;
  private final ClientToolVersion latestVersion;
}
