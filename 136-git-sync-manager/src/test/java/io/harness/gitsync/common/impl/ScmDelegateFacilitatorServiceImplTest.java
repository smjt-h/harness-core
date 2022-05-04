/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.impl;

import static io.harness.rule.OwnerRule.BHAVYA;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.HARI;
import static io.harness.rule.OwnerRule.MOHIT_GARG;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.PageRequestDTO;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.beans.gitsync.GitPRCreateRequest;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.delegate.task.scm.GitFileTaskResponseData;
import io.harness.delegate.task.scm.ScmGitFileTaskParams;
import io.harness.delegate.task.scm.ScmGitRefTaskResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.gitsync.common.dtos.GitFileContent;
import io.harness.gitsync.common.helper.GitSyncConnectorHelper;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.ng.beans.PageRequest;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.product.ci.scm.proto.GetUserReposResponse;
import io.harness.product.ci.scm.proto.ListBranchesResponse;
import io.harness.product.ci.scm.proto.ListBranchesWithDefaultResponse;
import io.harness.product.ci.scm.proto.Repository;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.service.DelegateGrpcClientWrapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.DX)
public class ScmDelegateFacilitatorServiceImplTest extends GitSyncTestBase {
  @Mock SecretManagerClientService secretManagerClientService;
  @Mock ConnectorService connectorService;
  @Mock DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Mock YamlGitConfigService yamlGitConfigService;
  @Mock GitSyncConnectorHelper gitSyncConnectorHelper;
  ScmDelegateFacilitatorServiceImpl scmDelegateFacilitatorService;
  FileContent fileContent = FileContent.newBuilder().build();
  String accountIdentifier = "accountIdentifier";
  String projectIdentifier = "projectIdentifier";
  String orgIdentifier = "orgIdentifier";
  String connectorIdentifierRef = "connectorIdentifierRef";
  String repoURL = "repoURL";
  String yamlGitConfigIdentifier = "yamlGitConfigIdentifier";
  String filePath = "filePath";
  String branch = "branch";
  String connectorRef = "connectorRef";
  String repoName = "repoName";
  String commitId = "commitId";
  String defaultBranch = "default";
  GithubConnectorDTO githubConnector;
  final ListBranchesResponse listBranchesResponse =
      ListBranchesResponse.newBuilder().addBranches("master").addBranches("feature").build();

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    scmDelegateFacilitatorService = new ScmDelegateFacilitatorServiceImpl(connectorService, null, yamlGitConfigService,
        secretManagerClientService, delegateGrpcClientWrapper, null, gitSyncConnectorHelper);
    when(secretManagerClientService.getEncryptionDetails(any(), any())).thenReturn(Collections.emptyList());
    githubConnector = GithubConnectorDTO.builder().apiAccess(GithubApiAccessDTO.builder().build()).build();
    ConnectorInfoDTO connectorInfo = ConnectorInfoDTO.builder().connectorConfig(githubConnector).build();
    doReturn(Optional.of(ConnectorResponseDTO.builder().connector(connectorInfo).build()))
        .when(connectorService)
        .get(anyString(), anyString(), anyString(), anyString());
    doReturn((ScmConnector) connectorInfo.getConnectorConfig())
        .when(gitSyncConnectorHelper)
        .getScmConnector(any(), any(), any(), any());
    when(yamlGitConfigService.get(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(YamlGitConfigDTO.builder()
                        .accountIdentifier(accountIdentifier)
                        .projectIdentifier(projectIdentifier)
                        .organizationIdentifier(orgIdentifier)
                        .gitConnectorRef(connectorIdentifierRef)
                        .build());
    when(gitSyncConnectorHelper.getScmConnector(
             anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn((ScmConnector) connectorInfo.getConnectorConfig());
    doReturn(githubConnector)
        .when(gitSyncConnectorHelper)
        .getScmConnectorForGivenRepo(anyString(), anyString(), anyString(), anyString(), anyString());
  }

  @Test
  @Owner(developers = HARI)
  @Category(UnitTests.class)
  public void listBranchesForRepoByConnectorTest() {
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(
            ScmGitRefTaskResponseData.builder().listBranchesResponse(listBranchesResponse.toByteArray()).build());
    final List<String> branches = scmDelegateFacilitatorService.listBranchesForRepoByConnector(accountIdentifier,
        orgIdentifier, projectIdentifier, connectorIdentifierRef, repoURL,
        PageRequest.builder().pageIndex(0).pageSize(10).build(), "");
    assertThat(branches).isEqualTo(listBranchesResponse.getBranchesList());
  }

  @Test
  @Owner(developers = HARI)
  @Category(UnitTests.class)
  public void getFileContentTest() {
    final ArgumentCaptor<DelegateTaskRequest> delegateTaskRequestArgumentCaptor =
        ArgumentCaptor.forClass(DelegateTaskRequest.class);

    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(GitFileTaskResponseData.builder().fileContent(fileContent.toByteArray()).build());
    FileContent gitFileContent = scmDelegateFacilitatorService.getFile(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorRef, repoName, branch, filePath, null);
    assertThat(gitFileContent).isEqualTo(fileContent);

    gitFileContent = scmDelegateFacilitatorService.getFile(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorRef, repoName, branch, filePath, commitId);
    assertThat(gitFileContent).isEqualTo(fileContent);

    verify(delegateGrpcClientWrapper, times(2)).executeSyncTask(delegateTaskRequestArgumentCaptor.capture());

    List<DelegateTaskRequest> delegateTaskRequestList = delegateTaskRequestArgumentCaptor.getAllValues();

    ScmGitFileTaskParams scmGitFileTaskParams =
        (ScmGitFileTaskParams) delegateTaskRequestList.get(0).getTaskParameters();
    assertThat(scmGitFileTaskParams.getBranch()).isEqualTo(branch);
    assertThat(scmGitFileTaskParams.getScmConnector()).isEqualTo(githubConnector);
    assertThat(scmGitFileTaskParams.getGitFilePathDetails())
        .isEqualTo(GitFilePathDetails.builder().filePath(filePath).branch(branch).ref(null).build());

    scmGitFileTaskParams = (ScmGitFileTaskParams) delegateTaskRequestList.get(1).getTaskParameters();
    assertThat(scmGitFileTaskParams.getBranch()).isEqualTo(branch);
    assertThat(scmGitFileTaskParams.getScmConnector()).isEqualTo(githubConnector);
    assertThat(scmGitFileTaskParams.getGitFilePathDetails())
        .isEqualTo(GitFilePathDetails.builder().filePath(filePath).branch(null).ref(commitId).build());
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void getFileTest() {
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(GitFileTaskResponseData.builder().fileContent(fileContent.toByteArray()).build());
    final GitFileContent gitFileContent = scmDelegateFacilitatorService.getFileContent(
        yamlGitConfigIdentifier, accountIdentifier, orgIdentifier, projectIdentifier, filePath, branch, null);
    assertThat(gitFileContent)
        .isEqualTo(
            GitFileContent.builder().content(fileContent.getContent()).objectId(fileContent.getBlobId()).build());
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testCreatePRWithSameSourceTargetBranch() {
    GitPRCreateRequest createPRRequest =
        GitPRCreateRequest.builder().sourceBranch("branch").targetBranch("branch").build();
    assertThatThrownBy(() -> scmDelegateFacilitatorService.createPullRequest(createPRRequest))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void getListUserRepos() {
    GetUserReposResponse getUserReposResponse =
        GetUserReposResponse.newBuilder().addRepos(Repository.newBuilder().setName(repoName).build()).build();
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(
            ScmGitRefTaskResponseData.builder().getUserReposResponse(getUserReposResponse.toByteArray()).build());
    getUserReposResponse = scmDelegateFacilitatorService.listUserRepos(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorRef, PageRequestDTO.builder().build());
    assertThat(getUserReposResponse.getReposCount()).isEqualTo(1);
    assertThat(getUserReposResponse.getRepos(0).getName()).isEqualTo(repoName);
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void testListBranches() {
    ListBranchesWithDefaultResponse listBranchesWithDefaultResponse = ListBranchesWithDefaultResponse.newBuilder()
                                                                          .addAllBranches(Arrays.asList(branch))
                                                                          .setDefaultBranch(defaultBranch)
                                                                          .build();
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(ScmGitRefTaskResponseData.builder()
                        .getListBranchesWithDefaultResponse(listBranchesWithDefaultResponse.toByteArray())
                        .build());
    listBranchesWithDefaultResponse = scmDelegateFacilitatorService.listBranches(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorRef, repoName, PageRequestDTO.builder().build());
    assertThat(listBranchesWithDefaultResponse.getBranchesCount()).isEqualTo(1);
    assertThat(listBranchesWithDefaultResponse.getDefaultBranch()).isEqualTo(defaultBranch);
    assertThat(listBranchesWithDefaultResponse.getBranchesList().get(0)).isEqualTo(branch);
  }
}
