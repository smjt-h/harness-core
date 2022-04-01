/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.artifacts.artifactory;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifactory.ArtifactoryConfigRequest;
import io.harness.artifactory.service.ArtifactoryRegistryService;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.comparator.BuildDetailsInternalComparatorDescending;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.task.artifacts.ArtifactSourceDelegateRequest;
import io.harness.delegate.task.artifacts.DelegateArtifactTaskHandler;
import io.harness.delegate.task.artifacts.mappers.ArtifactoryRequestResponseMapper;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.CDP)
public class ArtifactoryArtifactTaskHandler
    extends DelegateArtifactTaskHandler<ArtifactSourceDelegateRequest> {
  private final ArtifactoryRegistryService artifactoryRegistryService;
  private final SecretDecryptionService secretDecryptionService;
  private final ArtifactoryArtifactTaskHelper artifactoryArtifactTaskHelper;

  @Override
  public ArtifactTaskExecutionResponse getLastSuccessfulBuild(
          ArtifactSourceDelegateRequest artifactSourceDelegateRequest) {
    ArtifactoryDockerArtifactDelegateRequest attributesRequest = (ArtifactoryDockerArtifactDelegateRequest) artifactSourceDelegateRequest;
    BuildDetailsInternal lastSuccessfulBuild;
    ArtifactoryConfigRequest artifactoryConfig =
        ArtifactoryRequestResponseMapper.toArtifactoryInternalConfig(attributesRequest);

    if (isRegex(attributesRequest)) {
      lastSuccessfulBuild = artifactoryRegistryService.getLastSuccessfulBuildFromRegex(artifactoryConfig,
          attributesRequest.getRepositoryName(), attributesRequest.getArtifactPath(),
          attributesRequest.getRepositoryFormat(), attributesRequest.getTagRegex());
    } else {
      lastSuccessfulBuild =
          artifactoryRegistryService.verifyBuildNumber(artifactoryConfig, attributesRequest.getRepositoryName(),
              attributesRequest.getArtifactPath(), attributesRequest.getRepositoryFormat(), attributesRequest.getTag());
    }

    artifactoryRegistryService.verifyArtifactManifestUrl(lastSuccessfulBuild, artifactoryConfig);

    ArtifactoryDockerArtifactDelegateResponse artifactoryDockerArtifactDelegateResponse =
        ArtifactoryRequestResponseMapper.toArtifactoryDockerResponse(lastSuccessfulBuild, attributesRequest);

    return getSuccessTaskExecutionResponse(Collections.singletonList(artifactoryDockerArtifactDelegateResponse));
  }

  @Override
  public ArtifactTaskExecutionResponse getBuilds(ArtifactSourceDelegateRequest artifactSourceDelegateRequest) {

    if(artifactSourceDelegateRequest instanceof ArtifactoryGenericArtifactDelegateRequest) {
      return artifactoryArtifactTaskHelper.fetchFileBuilds((ArtifactoryGenericArtifactDelegateRequest) artifactSourceDelegateRequest, null);
    } else {
      ArtifactoryDockerArtifactDelegateRequest attributesRequest = (ArtifactoryDockerArtifactDelegateRequest) artifactSourceDelegateRequest;
      List<BuildDetailsInternal> builds = artifactoryRegistryService.getBuilds(
              ArtifactoryRequestResponseMapper.toArtifactoryInternalConfig(attributesRequest),
              attributesRequest.getRepositoryName(), attributesRequest.getArtifactPath(),
              attributesRequest.getRepositoryFormat(), ArtifactoryRegistryService.MAX_NO_OF_TAGS_PER_ARTIFACT);
      List<ArtifactoryDockerArtifactDelegateResponse> artifactoryDockerArtifactDelegateResponseList =
              builds.stream()
                      .sorted(new BuildDetailsInternalComparatorDescending())
                      .map(build -> ArtifactoryRequestResponseMapper.toArtifactoryDockerResponse(build, attributesRequest))
                      .collect(Collectors.toList());
      return getSuccessTaskExecutionResponse(artifactoryDockerArtifactDelegateResponseList);
    }
  }

  @Override
  public ArtifactTaskExecutionResponse validateArtifactServer(
          ArtifactSourceDelegateRequest artifactSourceDelegateRequest) {
    ArtifactoryDockerArtifactDelegateRequest attributesRequest = (ArtifactoryDockerArtifactDelegateRequest) artifactSourceDelegateRequest;
    boolean isServerValidated = artifactoryRegistryService.validateCredentials(
        ArtifactoryRequestResponseMapper.toArtifactoryInternalConfig(attributesRequest));
    return ArtifactTaskExecutionResponse.builder().isArtifactServerValid(isServerValidated).build();
  }

  private ArtifactTaskExecutionResponse getSuccessTaskExecutionResponse(
      List<ArtifactoryDockerArtifactDelegateResponse> responseList) {
    return ArtifactTaskExecutionResponse.builder()
        .artifactDelegateResponses(responseList)
        .isArtifactSourceValid(true)
        .isArtifactServerValid(true)
        .build();
  }

  public ArtifactTaskExecutionResponse getSuccessTaskExecutionResponseGeneric(
      List<ArtifactoryGenericArtifactDelegateResponse> responseList) {
    return ArtifactTaskExecutionResponse.builder()
        .artifactDelegateResponses(responseList)
        .isArtifactSourceValid(true)
        .isArtifactServerValid(true)
        .build();
  }

  boolean isRegex(ArtifactoryDockerArtifactDelegateRequest artifactDelegateRequest) {
    return EmptyPredicate.isNotEmpty(artifactDelegateRequest.getTagRegex());
  }

  public void decryptRequestDTOs(ArtifactSourceDelegateRequest artifactoryRequest) {
    if (artifactoryRequest instanceof ArtifactoryGenericArtifactDelegateRequest) {
      ArtifactoryGenericArtifactDelegateRequest artifactoryGenericArtifactDelegateRequest = (ArtifactoryGenericArtifactDelegateRequest) artifactoryRequest;
      if (artifactoryGenericArtifactDelegateRequest.getArtifactoryConnectorDTO().getAuth() != null) {
        secretDecryptionService.decrypt(artifactoryGenericArtifactDelegateRequest.getArtifactoryConnectorDTO().getAuth().getCredentials(),
                artifactoryGenericArtifactDelegateRequest.getEncryptedDataDetails());
      }
    } else {
      ArtifactoryDockerArtifactDelegateRequest artifactoryDockerArtifactDelegateRequest = (ArtifactoryDockerArtifactDelegateRequest) artifactoryRequest;
      if (artifactoryDockerArtifactDelegateRequest.getArtifactoryConnectorDTO().getAuth() != null) {
        secretDecryptionService.decrypt(artifactoryDockerArtifactDelegateRequest.getArtifactoryConnectorDTO().getAuth().getCredentials(),
                artifactoryDockerArtifactDelegateRequest.getEncryptedDataDetails());
      }
    }
  }

}
