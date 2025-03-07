/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.artifacts.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifactory.ArtifactoryConfigRequest;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryUsernamePasswordAuthDTO;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.artifactory.ArtifactoryArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.artifactory.ArtifactoryArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.artifactory.ArtifactoryGenericArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.artifactory.ArtifactoryGenericArtifactDelegateResponse;
import io.harness.utils.FieldWithPlainTextOrSecretValueHelper;

import software.wings.helpers.ext.jenkins.BuildDetails;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.CDP)
public class ArtifactoryRequestResponseMapper {
  public ArtifactoryConfigRequest toArtifactoryInternalConfig(ArtifactoryArtifactDelegateRequest request) {
    char[] password = null;
    String username = null;
    boolean hasCredentials = false;
    if (request.getArtifactoryConnectorDTO().getAuth() != null
        && request.getArtifactoryConnectorDTO().getAuth().getCredentials() != null) {
      ArtifactoryUsernamePasswordAuthDTO credentials =
          (ArtifactoryUsernamePasswordAuthDTO) request.getArtifactoryConnectorDTO().getAuth().getCredentials();
      if (credentials.getPasswordRef() != null) {
        password = EmptyPredicate.isNotEmpty(credentials.getPasswordRef().getDecryptedValue())
            ? credentials.getPasswordRef().getDecryptedValue()
            : null;
      }
      username = FieldWithPlainTextOrSecretValueHelper.getSecretAsStringFromPlainTextOrSecretRef(
          credentials.getUsername(), credentials.getUsernameRef());
      hasCredentials = true;
    }
    return ArtifactoryConfigRequest.builder()
        .artifactoryUrl(request.getArtifactoryConnectorDTO().getArtifactoryServerUrl())
        .username(username)
        .password(password)
        .hasCredentials(hasCredentials)
        .artifactRepositoryUrl(request.getArtifactRepositoryUrl())
        .build();
  }

  public ArtifactoryArtifactDelegateResponse toArtifactoryDockerResponse(
      BuildDetailsInternal buildDetailsInternal, ArtifactoryArtifactDelegateRequest request) {
    return ArtifactoryArtifactDelegateResponse.builder()
        .buildDetails(ArtifactBuildDetailsMapper.toBuildDetailsNG(buildDetailsInternal))
        .repositoryName(request.getRepositoryName())
        .artifactPath(request.getArtifactPath())
        .repositoryFormat(request.getRepositoryFormat())
        .tag(buildDetailsInternal.getNumber())
        .sourceType(ArtifactSourceType.ARTIFACTORY_REGISTRY)
        .build();
  }

  public ArtifactoryGenericArtifactDelegateResponse toArtifactoryGenericResponse(
      BuildDetails buildDetails, ArtifactoryGenericArtifactDelegateRequest request) {
    return ArtifactoryGenericArtifactDelegateResponse.builder()
        .buildDetails(ArtifactBuildDetailsMapper.toBuildDetailsNG(buildDetails))
        .repositoryName(request.getRepositoryName())
        .artifactPath(buildDetails.getArtifactPath())
        .repositoryFormat(request.getRepositoryFormat())
        .sourceType(ArtifactSourceType.ARTIFACTORY_REGISTRY)
        .build();
  }
}
