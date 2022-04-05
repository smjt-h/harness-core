/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.service.git.NGGitServiceImpl;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.git.GitFetchFilesTaskHelper;
import io.harness.delegate.task.git.ScmFetchFilesHelperNG;
import io.harness.git.GitClientV2;
import io.harness.git.model.AuthInfo;
import io.harness.git.model.AuthRequest;
import io.harness.git.model.FetchFilesResult;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.rule.Owner;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.shell.SshSessionConfig;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(CDP)
public class ServerlessGitFetchTaskHelperTest extends CategoryTest {
  @Mock GitClientV2 gitClientV2;
  @Mock GitFetchFilesTaskHelper gitFetchFilesTaskHelper;
  @Mock ScmFetchFilesHelperNG scmFetchFilesHelper;
  @Mock NGGitServiceImpl ngGitService;
  @Mock SecretDecryptionService secretDecryptionService;
  @InjectMocks ServerlessGitFetchTaskHelper serverlessGitFetchTaskHelper;

  String accountId = "accountId";
  String url = "url";
  String branch = "branch";
  String commitId = "commitId";
  String connectorName = "connectorName";
  GitConfigDTO gitConfigDTO = GitConfigDTO.builder().url(url).build();
  SSHKeySpecDTO sshKeySpecDTO = SSHKeySpecDTO.builder().build();
  SshSessionConfig sshSessionConfig = SshSessionConfig.Builder.aSshSessionConfig().build();
  GitStoreDelegateConfig gitStoreDelegateConfig = GitStoreDelegateConfig.builder()
                                                      .sshKeySpecDTO(sshKeySpecDTO)
                                                      .encryptedDataDetails(Arrays.asList())
                                                      .optimizedFilesFetch(false)
                                                      .branch(branch)
                                                      .commitId(commitId)
                                                      .connectorName(connectorName)
                                                      .build();
  List<String> filePaths = Arrays.asList();
  AuthRequest authRequest = new AuthRequest(AuthInfo.AuthType.SSH_KEY);
  FetchFilesResult fetchFilesResult = FetchFilesResult.builder().build();

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void getCompleteFilePathTest() {
    assertThat(ServerlessGitFetchTaskHelper.getCompleteFilePath("a", "b")).isEqualTo("ab");
  }

  @Test
  @Owner(developers = PIYUSH_BHUWALKA)
  @Category(UnitTests.class)
  public void getCompleteFilePathTestWhenFolderEmpty() {
    assertThat(ServerlessGitFetchTaskHelper.getCompleteFilePath(null, "b")).isEqualTo("b");
  }
}