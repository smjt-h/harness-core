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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.beans.PageRequestDTO;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.gitsync.common.dtos.GitFileContent;
import io.harness.gitsync.common.helper.GitSyncConnectorHelper;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.ng.beans.PageRequest;
import io.harness.product.ci.scm.proto.Commit;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.product.ci.scm.proto.GetLatestCommitResponse;
import io.harness.product.ci.scm.proto.GetUserReposResponse;
import io.harness.product.ci.scm.proto.ListBranchesResponse;
import io.harness.product.ci.scm.proto.ListBranchesWithDefaultResponse;
import io.harness.product.ci.scm.proto.Repository;
import io.harness.rule.Owner;
import io.harness.service.ScmClient;
import io.harness.tasks.DecryptGitApiAccessHelper;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.DX)
public class ScmManagerFacilitatorServiceImplTest extends GitSyncTestBase {
  @Mock ScmClient scmClient;
  @Mock DecryptGitApiAccessHelper decryptGitApiAccessHelper;
  @Mock ConnectorService connectorService;
  @Mock AbstractScmClientFacilitatorServiceImpl abstractScmClientFacilitatorService;
  @Mock YamlGitConfigService yamlGitConfigService;
  @Mock GitSyncConnectorHelper gitSyncConnectorHelper;
  @InjectMocks @Inject ScmManagerFacilitatorServiceImpl scmManagerFacilitatorService;
  String accountIdentifier = "accountIdentifier";
  String projectIdentifier = "projectIdentifier";
  String orgIdentifier = "orgIdentifier";
  String connectorIdentifierRef = "connectorIdentifierRef";
  String repoURL = "repoURL";
  String yamlGitConfigIdentifier = "yamlGitConfigIdentifier";
  String filePath = "filePath";
  String connectorRef = "connectorRef";
  final String branch = "branch";
  String repoName = "repoName";
  String commitId = "commitId";
  String defaultBranch = "default";
  FileContent fileContent = FileContent.newBuilder().build();
  GithubConnectorDTO githubConnector;
  ConnectorInfoDTO connectorInfo;
  final ListBranchesResponse listBranchesResponse =
      ListBranchesResponse.newBuilder().addBranches("master").addBranches("feature").build();

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(decryptGitApiAccessHelper.decryptScmApiAccess(any(), any(), any(), any()))
        .thenReturn(GithubConnectorDTO.builder().build());
    when(scmClient.getFileContent(any(), any())).thenReturn(fileContent);
    when(scmClient.listBranches(any())).thenReturn(listBranchesResponse);
    githubConnector = GithubConnectorDTO.builder().apiAccess(GithubApiAccessDTO.builder().build()).build();
    connectorInfo = ConnectorInfoDTO.builder().connectorConfig(githubConnector).build();
    doReturn(Optional.of(ConnectorResponseDTO.builder().connector(connectorInfo).build()))
        .when(connectorService)
        .get(anyString(), anyString(), anyString(), anyString());
    doReturn(githubConnector).when(gitSyncConnectorHelper).getDecryptedConnector(any(), any());
    doReturn(githubConnector)
        .when(gitSyncConnectorHelper)
        .getDecryptedConnector(anyString(), anyString(), anyString(), anyString());
    doReturn(githubConnector)
        .when(gitSyncConnectorHelper)
        .getDecryptedConnectorByRef(anyString(), anyString(), anyString(), anyString());
    doReturn(githubConnector)
        .when(gitSyncConnectorHelper)
        .getDecryptedConnectorForGivenRepo(anyString(), anyString(), anyString(), anyString(), anyString());
    when(abstractScmClientFacilitatorService.getYamlGitConfigDTO(
             accountIdentifier, orgIdentifier, projectIdentifier, yamlGitConfigIdentifier))
        .thenReturn(YamlGitConfigDTO.builder().build());
    when(abstractScmClientFacilitatorService.getConnectorIdentifierRef(any(), anyString(), anyString(), anyString()))
        .thenReturn(IdentifierRef.builder().build());
    when(yamlGitConfigService.get(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(YamlGitConfigDTO.builder()
                        .accountIdentifier(accountIdentifier)
                        .projectIdentifier(projectIdentifier)
                        .organizationIdentifier(orgIdentifier)
                        .gitConnectorRef(connectorIdentifierRef)
                        .build());
  }

  @Test
  @Owner(developers = HARI)
  @Category(UnitTests.class)
  public void listBranchesForRepoByConnectorTest() {
    final List<String> branches =
        scmManagerFacilitatorService.listBranchesForRepoByConnector(accountIdentifier, orgIdentifier, projectIdentifier,
            connectorIdentifierRef, repoURL, PageRequest.builder().pageIndex(0).pageSize(10).build(), "");
    assertThat(branches).isEqualTo(listBranchesResponse.getBranchesList());
  }

  @Test
  @Owner(developers = HARI)
  @Category(UnitTests.class)
  public void getFileContentTest() {
    final GitFileContent gitFileContent = scmManagerFacilitatorService.getFileContent(
        yamlGitConfigIdentifier, accountIdentifier, orgIdentifier, projectIdentifier, filePath, branch, null);
    assertThat(gitFileContent)
        .isEqualTo(
            GitFileContent.builder().content(fileContent.getContent()).objectId(fileContent.getBlobId()).build());
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void getFileTest() {
    final ArgumentCaptor<ScmConnector> scmConnectorArgumentCaptor = ArgumentCaptor.forClass(ScmConnector.class);
    final ArgumentCaptor<GitFilePathDetails> gitFilePathDetailsArgumentCaptor =
        ArgumentCaptor.forClass(GitFilePathDetails.class);
    FileContent gitFileContent = scmManagerFacilitatorService.getFile(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorRef, repoName, branch, filePath, null);
    assertThat(gitFileContent).isEqualTo(fileContent);

    gitFileContent = scmManagerFacilitatorService.getFile(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorRef, repoName, branch, filePath, commitId);
    verify(scmClient, times(2))
        .getFileContent(scmConnectorArgumentCaptor.capture(), gitFilePathDetailsArgumentCaptor.capture());

    List<ScmConnector> scmConnectors = scmConnectorArgumentCaptor.getAllValues();
    List<GitFilePathDetails> gitFilePathDetails = gitFilePathDetailsArgumentCaptor.getAllValues();

    assertThat(scmConnectors.get(0)).isEqualTo(githubConnector);
    assertThat(scmConnectors.get(1)).isEqualTo(githubConnector);
    assertThat(gitFilePathDetails.get(0))
        .isEqualTo(GitFilePathDetails.builder().filePath(filePath).branch(branch).ref(null).build());
    assertThat(gitFilePathDetails.get(1))
        .isEqualTo(GitFilePathDetails.builder().filePath(filePath).branch(null).ref(commitId).build());
    assertThat(gitFileContent).isEqualTo(fileContent);
  }

  @Test
  @Owner(developers = HARI)
  @Category(UnitTests.class)
  public void isSaasGitTest() {
    List<String> repoURLs = new ArrayList<>(Arrays.asList(
        "www.github.com", "http://www.gitlab.com", "www.github.harness.com", "harness.github.com", "github.com"));
    List<Boolean> expected = new ArrayList<>(Arrays.asList(true, true, false, false, true));
    List<Boolean> actual = new ArrayList<>();
    repoURLs.forEach(repoURL -> actual.add(GitUtils.isSaasGit(repoURL).isSaasGit()));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testGetLatestCommit() {
    String commitId = "commitId";
    ArgumentCaptor<String> branchNameCapture = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> ref = ArgumentCaptor.forClass(String.class);
    when(scmClient.getLatestCommit(any(), branchNameCapture.capture(), ref.capture()))
        .thenReturn(
            GetLatestCommitResponse.newBuilder().setCommit(Commit.newBuilder().setSha(commitId).build()).build());
    YamlGitConfigDTO yamlGitConfigDTO = YamlGitConfigDTO.builder().branch("default").build();
    final Commit returnedCommit = scmManagerFacilitatorService.getLatestCommit(yamlGitConfigDTO, "branch1");
    assertThat(returnedCommit.getSha()).isEqualTo(commitId);
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void testListUserRepos() {
    Repository repoDetails = Repository.newBuilder().setName(repoName).build();
    when(scmClient.getUserRepos(any(), any()))
        .thenReturn(GetUserReposResponse.newBuilder().addRepos(repoDetails).build());
    final GetUserReposResponse userReposResponse =
        scmManagerFacilitatorService.listUserRepos(accountIdentifier, orgIdentifier, projectIdentifier,
            (ScmConnector) connectorInfo.getConnectorConfig(), PageRequestDTO.builder().build());
    assertThat(userReposResponse.getReposCount()).isEqualTo(1);
    assertThat(userReposResponse.getRepos(0).getName()).isEqualTo(repoName);
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void testListBranches() {
    when(scmClient.listBranchesWithDefault(any(), any()))
        .thenReturn(ListBranchesWithDefaultResponse.newBuilder()
                        .addAllBranches(Arrays.asList(branch))
                        .setDefaultBranch(defaultBranch)
                        .build());
    final ListBranchesWithDefaultResponse listBranchesWithDefaultResponse =
        scmManagerFacilitatorService.listBranches(accountIdentifier, orgIdentifier, projectIdentifier,
            (ScmConnector) connectorInfo.getConnectorConfig(), PageRequestDTO.builder().build());
    assertThat(listBranchesWithDefaultResponse.getBranchesCount()).isEqualTo(1);
    assertThat(listBranchesWithDefaultResponse.getDefaultBranch()).isEqualTo(defaultBranch);
    assertThat(listBranchesWithDefaultResponse.getBranchesList().get(0)).isEqualTo(branch);
  }
}
