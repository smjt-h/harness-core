/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.artifacts.artifactory;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.response.ArtifactBuildDetailsNG;
import io.harness.delegate.task.artifacts.response.ArtifactDelegateResponse;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
@OwnedBy(HarnessTeam.CDP)
public class ArtifactoryGenericArtifactDelegateResponse extends ArtifactDelegateResponse {
  String repositoryName;
  /** Images in repos need to be referenced via a path */
  String artifactPath;
  String repositoryFormat;
  /** Tag refers to exact tag number */
  String artifactDirectory;

  @Builder
  public ArtifactoryGenericArtifactDelegateResponse(ArtifactBuildDetailsNG buildDetails, ArtifactSourceType sourceType,
      String repositoryName, String artifactPath, String repositoryFormat, String artifactDirectory) {
    super(buildDetails, sourceType);
    this.repositoryName = repositoryName;
    this.artifactPath = artifactPath;
    this.repositoryFormat = repositoryFormat;
    this.artifactDirectory = artifactDirectory;
  }
}
