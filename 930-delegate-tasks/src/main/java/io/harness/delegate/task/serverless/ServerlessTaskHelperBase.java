/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.filesystem.FileIo.*;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifactory.ArtifactoryConfigRequest;
import io.harness.artifactory.ArtifactoryNgService;
import io.harness.connector.service.git.NGGitService;
import io.harness.connector.task.git.GitDecryptionHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.NGDelegateLogCallback;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.artifactory.ArtifactoryRequestMapper;
import io.harness.delegate.task.git.ScmFetchFilesHelperNG;
import io.harness.filesystem.FileIo;
import io.harness.logging.LogCallback;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.serverless.model.ServerlessDelegateTaskParams;
import io.harness.shell.SshSessionConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

@Singleton
@Slf4j
@OwnedBy(CDP)
public class ServerlessTaskHelperBase {
  @Inject private ServerlessGitFetchTaskHelper serverlessGitFetchTaskHelper;
  @Inject private ScmFetchFilesHelperNG scmFetchFilesHelper;
  @Inject private GitDecryptionHelper gitDecryptionHelper;
  @Inject private NGGitService ngGitService;
  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private ArtifactoryNgService artifactoryNgService;
  @Inject private ArtifactoryRequestMapper artifactoryRequestMapper;

  private static final String ARTIFACTORY_ARTIFACT_PATH = "artifactPath";
  private static final String ARTIFACTORY_ARTIFACT_NAME = "artifactName";
  private static final String ARTIFACT_FILE_NAME = "artifactFile";

  public LogCallback getLogCallback(ILogStreamingTaskClient logStreamingTaskClient, String commandUnitName,
      boolean shouldOpenStream, CommandUnitsProgress commandUnitsProgress) {
    return new NGDelegateLogCallback(logStreamingTaskClient, commandUnitName, shouldOpenStream, commandUnitsProgress);
  }
  public void createHomeDirectory(String directoryPath) throws IOException {
    createDirectoryIfDoesNotExist(directoryPath);
    waitForDirectoryToBeAccessibleOutOfProcess(directoryPath, 10);
  }

  public void fetchManifestFilesAndWriteToDirectory(ServerlessAwsLambdaManifestConfig serverlessManifestConfig,
      String accountId, LogCallback executionLogCallback, ServerlessDelegateTaskParams serverlessDelegateTaskParams) {
    GitStoreDelegateConfig gitStoreDelegateConfig = serverlessManifestConfig.getGitStoreDelegateConfig();
    downloadFilesFromGit(
        gitStoreDelegateConfig, executionLogCallback, accountId, serverlessDelegateTaskParams.getWorkingDirectory());
    // todo: print file download statements
  }

  private void downloadFilesFromGit(GitStoreDelegateConfig gitStoreDelegateConfig, LogCallback executionLogCallback,
      String accountId, String workingDirectory) {
    try {
      // todo: print git config files
      if (gitStoreDelegateConfig.isOptimizedFilesFetch()) {
        executionLogCallback.saveExecutionLog("Using optimized file fetch");
        serverlessGitFetchTaskHelper.decryptGitStoreConfig(gitStoreDelegateConfig);
        scmFetchFilesHelper.downloadFilesUsingScm(workingDirectory, gitStoreDelegateConfig, executionLogCallback);
      } else {
        GitConfigDTO gitConfigDTO = ScmConnectorMapper.toGitConfigDTO(gitStoreDelegateConfig.getGitConfigDTO());
        gitDecryptionHelper.decryptGitConfig(gitConfigDTO, gitStoreDelegateConfig.getEncryptedDataDetails());
        SshSessionConfig sshSessionConfig = gitDecryptionHelper.getSSHSessionConfig(
            gitStoreDelegateConfig.getSshKeySpecDTO(), gitStoreDelegateConfig.getEncryptedDataDetails());
        ngGitService.downloadFiles(gitStoreDelegateConfig, workingDirectory, accountId, sshSessionConfig, gitConfigDTO);
      }
      // todo: add print statements for fetched directory
    } catch (Exception e) {
    }
  }
  public void replaceManifestWithRenderedContent(ServerlessDelegateTaskParams serverlessDelegateTaskParams,
      ServerlessAwsLambdaManifestConfig serverlessManifestConfig) throws IOException {
    String manifestFilePath =
        Paths.get(serverlessDelegateTaskParams.getWorkingDirectory(), serverlessManifestConfig.getManifestPath())
            .toString();
    updateManifestFileContent(manifestFilePath, serverlessManifestConfig.getManifestContent());
  }

  private void updateManifestFileContent(String manifestFilePath, String manifestContent) throws IOException {
    FileIo.deleteFileIfExists(manifestFilePath);
    FileIo.writeUtf8StringToFile(manifestFilePath, manifestContent);
  }

  public void fetchArtifact(ServerlessArtifactConfig serverlessArtifactConfig, LogCallback logCallback,
      String workingDirectory) throws IOException {
    if (serverlessArtifactConfig instanceof ServerlessArtifactoryArtifactConfig) {
      ServerlessArtifactoryArtifactConfig serverlessArtifactoryArtifactConfig =
          (ServerlessArtifactoryArtifactConfig) serverlessArtifactConfig;
      fetchArtifactoryArtifact(serverlessArtifactoryArtifactConfig, logCallback, workingDirectory);
    }
  }

  public void fetchArtifactoryArtifact(ServerlessArtifactoryArtifactConfig artifactoryArtifactConfig,
      LogCallback logCallback, String workingDirectory) throws IOException {
    if (EmptyPredicate.isEmpty(artifactoryArtifactConfig.getArtifactPath())) {
      // todo: handle it
    }
    ArtifactoryConnectorDTO artifactoryConnectorDTO =
        (ArtifactoryConnectorDTO) artifactoryArtifactConfig.getConnectorDTO().getConnectorConfig();
    secretDecryptionService.decrypt(
        artifactoryConnectorDTO.getAuth().getCredentials(), artifactoryArtifactConfig.getEncryptedDataDetails());
    ArtifactoryConfigRequest artifactoryConfigRequest =
        artifactoryRequestMapper.toArtifactoryRequest(artifactoryConnectorDTO);
    Map<String, String> artifactMetadata = new HashMap<>();
    String artifactPath =
        Paths.get(artifactoryArtifactConfig.getRepositoryName(), artifactoryArtifactConfig.getArtifactPath())
            .toString();
    artifactMetadata.put(ARTIFACTORY_ARTIFACT_PATH, artifactPath);
    artifactMetadata.put(ARTIFACTORY_ARTIFACT_NAME, artifactPath);
    String artifactFilePath = Paths.get(workingDirectory, ARTIFACT_FILE_NAME).toAbsolutePath().toString();
    File artifactFile = new File(artifactFilePath);
    try (InputStream artifactInputStream = artifactoryNgService.downloadArtifacts(artifactoryConfigRequest,
             artifactoryArtifactConfig.getRepositoryName(), artifactMetadata, ARTIFACTORY_ARTIFACT_PATH,
             ARTIFACTORY_ARTIFACT_NAME);
         FileOutputStream outputStream = new FileOutputStream(artifactFile)) {
      if (artifactInputStream == null) {
        // todo: handle it
      }
      if (!artifactFile.createNewFile()) {
        // todo: handle it
      }
      IOUtils.copy(artifactInputStream, outputStream);
    }
  }
}
