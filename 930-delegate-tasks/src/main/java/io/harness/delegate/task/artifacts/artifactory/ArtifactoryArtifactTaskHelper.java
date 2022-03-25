/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.artifacts.artifactory;

import static io.harness.artifactory.service.ArtifactoryRegistryService.MAX_NO_OF_TAGS_PER_ARTIFACT;
import static io.harness.delegate.task.artifacts.ArtifactTaskType.GET_BUILDS;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifact.ArtifactMetadataKeys;
import io.harness.artifactory.ArtifactoryConfigRequest;
import io.harness.artifactory.ArtifactoryNgService;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.task.artifactory.ArtifactoryRequestMapper;
import io.harness.delegate.task.artifacts.mappers.ArtifactoryRequestResponseMapper;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.eraro.ErrorCode;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.utils.RepositoryFormat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDP)
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class ArtifactoryArtifactTaskHelper {
  @Inject ArtifactoryRequestMapper artifactoryRequestMapper;
  @Inject ArtifactoryNgService artifactoryNgService;
  static final String DEFAULT_ARTIFACT_FILTER = "*";
  static final String DEFAULT_ARTIFACT_DIRECTORY = "/";
  private final ArtifactoryArtifactTaskHandler artifactoryArtifactTaskHandler;

  public ArtifactTaskResponse getArtifactCollectResponse(
      ArtifactTaskParameters artifactTaskParameters, LogCallback executionLogCallback) {
    ArtifactoryDockerArtifactDelegateRequest attributes =
        (ArtifactoryDockerArtifactDelegateRequest) artifactTaskParameters.getAttributes();
    String registryUrl = attributes.getArtifactoryConnectorDTO().getArtifactoryServerUrl();
    artifactoryArtifactTaskHandler.decryptRequestDTOs(attributes);
    ArtifactTaskResponse artifactTaskResponse;
    switch (artifactTaskParameters.getArtifactTaskType()) {
      case GET_LAST_SUCCESSFUL_BUILD:
        saveLogs(executionLogCallback, "Fetching Artifact details");
        artifactTaskResponse =
            getSuccessTaskResponse(artifactoryArtifactTaskHandler.getLastSuccessfulBuild(attributes));
        ArtifactoryDockerArtifactDelegateResponse artifactoryArtifactDelegateResponse =
            (ArtifactoryDockerArtifactDelegateResponse) (artifactTaskResponse.getArtifactTaskExecutionResponse()
                                                             .getArtifactDelegateResponses()
                                                             .size()
                        != 0
                    ? artifactTaskResponse.getArtifactTaskExecutionResponse().getArtifactDelegateResponses().get(0)
                    : ArtifactoryDockerArtifactDelegateResponse.builder().build());
        String buildMetadataUrl = artifactoryArtifactDelegateResponse.getBuildDetails() != null
            ? artifactoryArtifactDelegateResponse.getBuildDetails().getBuildUrl()
            : null;
        String dockerPullCommand =
            (RepositoryFormat.docker.name().equals(artifactoryArtifactDelegateResponse.getRepositoryFormat())
                && artifactoryArtifactDelegateResponse.getBuildDetails() != null
                && artifactoryArtifactDelegateResponse.getBuildDetails().getMetadata() != null)
            ? "\nImage pull command: docker pull "
                + artifactoryArtifactDelegateResponse.getBuildDetails().getMetadata().get(ArtifactMetadataKeys.IMAGE)
            : null;
        saveLogs(executionLogCallback,
            "Fetched Artifact details"
                + "\ntype: Artifactory Artifact"
                + "\nbuild metadata url: " + buildMetadataUrl
                + "\nrepository: " + artifactoryArtifactDelegateResponse.getRepositoryName()
                + "\nartifactPath: " + artifactoryArtifactDelegateResponse.getArtifactPath()
                + "\ntag: " + artifactoryArtifactDelegateResponse.getTag()
                + "\nrepository type: " + artifactoryArtifactDelegateResponse.getRepositoryFormat()
                + (EmptyPredicate.isNotEmpty(dockerPullCommand) ? dockerPullCommand : ""));
        break;
      case GET_BUILDS:
        saveLogs(executionLogCallback, "Fetching artifact details");
        artifactTaskResponse = getSuccessTaskResponse(artifactoryArtifactTaskHandler.getBuilds(attributes));
        saveLogs(executionLogCallback,
            "Fetched " + artifactTaskResponse.getArtifactTaskExecutionResponse().getArtifactDelegateResponses().size()
                + " artifacts");
        break;
      case VALIDATE_ARTIFACT_SERVER:
        saveLogs(executionLogCallback, "Validating  Artifact Server");
        artifactTaskResponse =
            getSuccessTaskResponse(artifactoryArtifactTaskHandler.validateArtifactServer(attributes));
        saveLogs(executionLogCallback, "validated artifact server: " + registryUrl);
        break;
      default:
        saveLogs(executionLogCallback,
            "No corresponding Artifactory artifact task type [{}]: " + artifactTaskParameters.toString());
        log.error("No corresponding Artifactory artifact task type [{}]", artifactTaskParameters.toString());
        return ArtifactTaskResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .errorMessage("There is no Artifactory artifact task type impl defined for - "
                + artifactTaskParameters.getArtifactTaskType().name())
            .errorCode(ErrorCode.INVALID_ARGUMENT)
            .build();
    }
    return artifactTaskResponse;
  }

  public ArtifactTaskResponse getGenericArtifactCollectResponse(
      ArtifactTaskParameters artifactTaskParameters, LogCallback executionLogCallback) {
    ArtifactTaskResponse artifactTaskResponse;
    switch (artifactTaskParameters.getArtifactTaskType()) {
      case GET_LAST_SUCCESSFUL_BUILD:
        saveLogs(executionLogCallback, "Fetching Artifact details");
        artifactTaskResponse = getSuccessTaskResponse(getLatestArtifact(artifactTaskParameters, executionLogCallback));
        ArtifactoryGenericArtifactDelegateResponse artifactoryGenericArtifactDelegateResponse =
            (ArtifactoryGenericArtifactDelegateResponse) (artifactTaskResponse.getArtifactTaskExecutionResponse()
                                                              .getArtifactDelegateResponses()
                                                              .size()
                        != 0
                    ? artifactTaskResponse.getArtifactTaskExecutionResponse().getArtifactDelegateResponses().get(0)
                    : ArtifactoryDockerArtifactDelegateResponse.builder().build());
        String buildMetadataUrl = artifactoryGenericArtifactDelegateResponse.getBuildDetails() != null
            ? artifactoryGenericArtifactDelegateResponse.getBuildDetails().getBuildUrl()
            : null;
        saveLogs(executionLogCallback,
            "Fetched Artifact details"
                + "\ntype: Artifactory Artifact"
                + "\nbuild metadata url: " + buildMetadataUrl
                + "\nrepository: " + artifactoryGenericArtifactDelegateResponse.getRepositoryName()
                + "\nartifactPath: " + artifactoryGenericArtifactDelegateResponse.getArtifactPath()
                + "\nrepository type: " + artifactoryGenericArtifactDelegateResponse.getRepositoryFormat());
        break;
      case GET_BUILDS:
        saveLogs(executionLogCallback, "Fetching artifact details");
        artifactTaskResponse = fetchFileBuilds(artifactTaskParameters, executionLogCallback);
        saveLogs(executionLogCallback,
            "Fetched " + artifactTaskResponse.getArtifactTaskExecutionResponse().getArtifactDelegateResponses().size()
                + " artifacts");
        break;
      default:
        saveLogs(executionLogCallback,
            "No corresponding Artifactory artifact task type [{}]: " + artifactTaskParameters.toString());
        log.error("No corresponding Artifactory artifact task type [{}]", artifactTaskParameters.toString());
        return ArtifactTaskResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .errorMessage("There is no Artifactory artifact task type impl defined for - "
                + artifactTaskParameters.getArtifactTaskType().name())
            .errorCode(ErrorCode.INVALID_ARGUMENT)
            .build();
    }
    return artifactTaskResponse;
  }

  public ArtifactTaskExecutionResponse getLatestArtifact(
      ArtifactTaskParameters artifactTaskParameters, LogCallback executionLogCallback) {
    ArtifactoryGenericArtifactDelegateRequest artifactoryGenericArtifactDelegateRequest =
        (ArtifactoryGenericArtifactDelegateRequest) artifactTaskParameters.getAttributes();
    artifactoryArtifactTaskHandler.decryptRequestDTOs(artifactoryGenericArtifactDelegateRequest);
    ArtifactoryConfigRequest artifactoryConfigRequest = artifactoryRequestMapper.toArtifactoryRequest(
        artifactoryGenericArtifactDelegateRequest.getArtifactoryConnectorDTO());
    String artifactPathFilter = artifactoryGenericArtifactDelegateRequest.getArtifactPathFilter();
    String artifactDirectory = artifactoryGenericArtifactDelegateRequest.getArtifactDirectory();
    if (artifactPathFilter.isEmpty()) {
      artifactPathFilter = DEFAULT_ARTIFACT_FILTER;
    }
    if (artifactDirectory.isEmpty()) {
      saveLogs(executionLogCallback,
          "Artifact Directory is Empty, assuming Artifacts are present in root of the repository");
      artifactDirectory = DEFAULT_ARTIFACT_DIRECTORY;
    }
    BuildDetails buildDetails = artifactoryNgService.getLatestArtifact(artifactoryConfigRequest,
        artifactoryGenericArtifactDelegateRequest.getRepositoryName(), artifactDirectory, artifactPathFilter,
        MAX_NO_OF_TAGS_PER_ARTIFACT);
    ArtifactoryGenericArtifactDelegateResponse artifactoryGenericArtifactDelegateResponse =
        ArtifactoryRequestResponseMapper.toArtifactoryGenericResponse(
            buildDetails, artifactoryGenericArtifactDelegateRequest);

    return artifactoryArtifactTaskHandler.getSuccessTaskExecutionResponseGeneric(
        Collections.singletonList(artifactoryGenericArtifactDelegateResponse));
  }

  public ArtifactTaskResponse fetchFileBuilds(ArtifactTaskParameters params, LogCallback executionLogCallback) {
    ArtifactoryGenericArtifactDelegateRequest artifactoryGenericArtifactDelegateRequest =
        (ArtifactoryGenericArtifactDelegateRequest) params.getAttributes();
    artifactoryArtifactTaskHandler.decryptRequestDTOs(artifactoryGenericArtifactDelegateRequest);
    ArtifactoryConfigRequest artifactoryConfigRequest = artifactoryRequestMapper.toArtifactoryRequest(
        artifactoryGenericArtifactDelegateRequest.getArtifactoryConnectorDTO());
    String artifactDirectory = artifactoryGenericArtifactDelegateRequest.getArtifactDirectory();
    if (artifactDirectory.isEmpty()) {
      saveLogs(executionLogCallback,
          "Artifact Directory is Empty, assuming Artifacts are present in root of the repository");
      artifactDirectory = DEFAULT_ARTIFACT_DIRECTORY;
    }
    String filePath = Paths.get(artifactDirectory, DEFAULT_ARTIFACT_FILTER).toString();

    List<BuildDetails> buildDetails = artifactoryNgService.getArtifactList(artifactoryConfigRequest,
        artifactoryGenericArtifactDelegateRequest.getRepositoryName(), filePath, MAX_NO_OF_TAGS_PER_ARTIFACT);

    return ArtifactTaskResponse.builder()
        .artifactTaskExecutionResponse(ArtifactTaskExecutionResponse.builder().buildDetails(buildDetails).build())
        .commandExecutionStatus(SUCCESS)
        .build();
  }

  private ArtifactTaskResponse getSuccessTaskResponse(ArtifactTaskExecutionResponse taskExecutionResponse) {
    return ArtifactTaskResponse.builder()
        .commandExecutionStatus(SUCCESS)
        .artifactTaskExecutionResponse(taskExecutionResponse)
        .build();
  }

  private void saveLogs(LogCallback executionLogCallback, String message) {
    if (executionLogCallback != null) {
      executionLogCallback.saveExecutionLog(message);
    }
  }
  public ArtifactTaskResponse getArtifactCollectResponse(ArtifactTaskParameters artifactTaskParameters) {
    if (artifactTaskParameters.getAttributes() instanceof ArtifactoryGenericArtifactDelegateRequest) {
      return getGenericArtifactCollectResponse(artifactTaskParameters, null);
    }
    return getArtifactCollectResponse(artifactTaskParameters, null);
  }
}
