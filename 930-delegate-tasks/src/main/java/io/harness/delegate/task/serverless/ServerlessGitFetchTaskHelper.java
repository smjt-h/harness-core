/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless;

import static io.harness.git.model.GitRepositoryType.YAML;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.helper.GitApiAccessDecryptionHelper;
import io.harness.connector.service.git.NGGitService;
import io.harness.connector.task.git.GitDecryptionHelper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.git.GitFetchFilesTaskHelper;
import io.harness.delegate.task.git.ScmFetchFilesHelperNG;
import io.harness.git.GitClientV2;
import io.harness.git.model.FetchFilesByPathRequest;
import io.harness.git.model.FetchFilesResult;
import io.harness.logging.LogCallback;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.shell.SshSessionConfig;

import software.wings.delegatetasks.ExceptionMessageSanitizer;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class ServerlessGitFetchTaskHelper {
  @Inject private GitClientV2 gitClientV2;
  @Inject private GitFetchFilesTaskHelper gitFetchFilesTaskHelper;
  @Inject private ScmFetchFilesHelperNG scmFetchFilesHelper;
  @Inject private GitDecryptionHelper gitDecryptionHelper;
  @Inject private NGGitService ngGitService;
  @Inject private SecretDecryptionService secretDecryptionService;

  public static String getCompleteFilePath(String folderPath, String fileKey) {
    if (isBlank(folderPath)) {
      return fileKey;
    }
    return folderPath + fileKey;
  }

  public void printFileNames(LogCallback executionLogCallback, List<String> filePaths) {
    executionLogCallback.saveExecutionLog("\nFetching following Files :");
    gitFetchFilesTaskHelper.printFileNamesInExecutionLogs(filePaths, executionLogCallback);
  }

  public FetchFilesResult fetchFileFromRepo(GitStoreDelegateConfig gitStoreDelegateConfig, List<String> filePaths,
      String accountId, GitConfigDTO gitConfigDTO) throws IOException {
    if (gitStoreDelegateConfig.isOptimizedFilesFetch()) {
      return scmFetchFilesHelper.fetchFilesFromRepoWithScm(gitStoreDelegateConfig, filePaths);
    }
    SshSessionConfig sshSessionConfig = gitDecryptionHelper.getSSHSessionConfig(
        gitStoreDelegateConfig.getSshKeySpecDTO(), gitStoreDelegateConfig.getEncryptedDataDetails());
    FetchFilesByPathRequest fetchFilesByPathRequest =
        FetchFilesByPathRequest.builder()
            .authRequest(ngGitService.getAuthRequest(gitConfigDTO, sshSessionConfig))
            .filePaths(filePaths)
            .recursive(true)
            .accountId(accountId)
            .branch(gitStoreDelegateConfig.getBranch())
            .commitId(gitStoreDelegateConfig.getCommitId())
            .connectorId(gitStoreDelegateConfig.getConnectorName())
            .repoType(YAML)
            .repoUrl(gitConfigDTO.getUrl())
            .build();
    return gitClientV2.fetchFilesByPath(fetchFilesByPathRequest);
  }

  public void decryptGitStoreConfig(GitStoreDelegateConfig gitStoreDelegateConfig) {
    secretDecryptionService.decrypt(
        GitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(gitStoreDelegateConfig.getGitConfigDTO()),
        gitStoreDelegateConfig.getApiAuthEncryptedDataDetails());
    ExceptionMessageSanitizer.storeAllSecretsForSanitizing(
        GitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(gitStoreDelegateConfig.getGitConfigDTO()),
        gitStoreDelegateConfig.getApiAuthEncryptedDataDetails());
  }
}
