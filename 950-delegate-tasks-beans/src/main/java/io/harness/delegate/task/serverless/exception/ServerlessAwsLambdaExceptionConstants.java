/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless.exception;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDP)
@UtilityClass
public class ServerlessAwsLambdaExceptionConstants {
  public final String CONFIG_CREDENTIAL_FAILED = "failed to setup aws credentials";

  public final String CONFIG_CREDENTIAL_FAILED_HINT = "Aws access key or Aws secret access key might be wrong,"
      + " please check it";

  public final String CONFIG_CREDENTIAL_FAILED_EXPLANATION = "serverless config credential command failed";

  public final String NO_SERVERLESS_MANIFEST_FAILED = "Not able to fetch serverless manifest file";

  public final String NO_SERVERLESS_MANIFEST_HINT = "please add a serverless manifest file inside provided path: %s";

  public final String NO_SERVERLESS_MANIFEST_EXPLANATION = "not able to find a serverless manifest file "
      + "(serverless.yaml/serverless.yml/serverless.json) inside provided path: %s";

  public final String DOWNLOAD_FROM_ARTIFACTORY_FAILED = "Failed to download artifact file";

  public final String DOWNLOAD_FROM_ARTIFACTORY_HINT = "Please check if artifact details point to an "
      + "existing artifact file";

  public final String DOWNLOAD_FROM_ARTIFACTORY_EXPLANATION = "Failed to download artifact: %s from"
      + " Artifactory: %s";

  public final String BLANK_ARTIFACT_PATH = "not able to find artifact path";

  public final String BLANK_ARTIFACT_PATH_HINT = "Please check artifactDirectory or artifactPath field";

  public final String BLANK_ARTIFACT_PATH_EXPLANATION = "artifact path is not present for artifactory identifier: %s";

  public final String SERVERLESS_MANIFEST_PROCESSING_FAILED = "failed to process serverless manifest file";

  public final String SERVERLESS_MANIFEST_PROCESSING_HINT =
      "please check that serverless manifest file has a valid yaml"
      + " or json content.";

  public final String SERVERLESS_MANIFEST_PROCESSING_EXPLANATION = "serverless manifest file has an invalid yaml or "
      + "json content.";

  public final String SERVERLESS_PLUGIN_INSTALL_FAILED = "failed to install serverless plugin";

  public final String SERVERLESS_PLUGIN_INSTALL_HINT = "please check the compatibility of serverless plugin: %s";

  public final String SERVERLESS_PLUGIN_INSTALL_EXPLANATION = "serverless plugin install --name %s failed";
}
