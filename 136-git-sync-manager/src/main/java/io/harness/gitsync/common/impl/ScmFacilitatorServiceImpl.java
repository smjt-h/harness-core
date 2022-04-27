/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.gitsync.common.beans.InfoForGitPush;
import io.harness.gitsync.common.dtos.CreatePRDTO;
import io.harness.gitsync.common.dtos.ScmCommitFileResponseDTO;
import io.harness.gitsync.common.dtos.ScmCreateFileRequestDTO;
import io.harness.gitsync.common.dtos.ScmCreatePRRequestDTO;
import io.harness.gitsync.common.dtos.ScmCreatePRResponseDTO;
import io.harness.gitsync.common.dtos.ScmUpdateFileRequestDTO;
import io.harness.gitsync.common.helper.GitSyncConnectorHelper;
import io.harness.gitsync.common.service.ScmFacilitatorService;
import io.harness.gitsync.common.service.ScmOrchestratorService;
import io.harness.ng.beans.PageRequest;
import io.harness.product.ci.scm.proto.CreateFileResponse;
import io.harness.product.ci.scm.proto.UpdateFileResponse;

import com.google.inject.Inject;
import java.util.List;

@OwnedBy(HarnessTeam.PL)
public class ScmFacilitatorServiceImpl implements ScmFacilitatorService {
  GitSyncConnectorHelper gitSyncConnectorHelper;
  ScmOrchestratorService scmOrchestratorService;

  @Inject
  public ScmFacilitatorServiceImpl(
      GitSyncConnectorHelper gitSyncConnectorHelper, ScmOrchestratorService scmOrchestratorService) {
    this.gitSyncConnectorHelper = gitSyncConnectorHelper;
    this.scmOrchestratorService = scmOrchestratorService;
  }

  @Override
  public List<String> listBranchesUsingConnector(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String connectorIdentifierRef, String repoURL, PageRequest pageRequest,
      String searchTerm) {
    return scmOrchestratorService.processScmRequestUsingConnectorSettings(scmClientFacilitatorService
        -> scmClientFacilitatorService.listBranchesForRepoByConnector(accountIdentifier, orgIdentifier,
            projectIdentifier, connectorIdentifierRef, repoURL, pageRequest, searchTerm),
        projectIdentifier, orgIdentifier, accountIdentifier, connectorIdentifierRef, null, null);
  }

  @Override
  public ScmCommitFileResponseDTO createFile(ScmCreateFileRequestDTO scmCreateFileRequestDTO) {
    Scope scope = scmCreateFileRequestDTO.getScope();
    // TODO Put validations over request here
    ScmConnector scmConnector = gitSyncConnectorHelper.getScmConnectorForGivenRepo(scope.getAccountIdentifier(),
        scope.getOrgIdentifier(), scope.getProjectIdentifier(), scmCreateFileRequestDTO.getConnectorRef(),
        scmCreateFileRequestDTO.getRepoName());
    CreateFileResponse createFileResponse =
        scmOrchestratorService.processScmRequestUsingConnectorSettings(scmClientFacilitatorService
            -> scmClientFacilitatorService.createFile(
                InfoForGitPush.builder()
                    .accountId(scope.getAccountIdentifier())
                    .orgIdentifier(scope.getOrgIdentifier())
                    .projectIdentifier(scope.getProjectIdentifier())
                    .filePath(scmCreateFileRequestDTO.getFilePath())
                    .baseBranch(scmCreateFileRequestDTO.getBranchName())
                    .branch(scmCreateFileRequestDTO.isCommitToNewBranch() ? scmCreateFileRequestDTO.getNewBranch()
                                                                          : scmCreateFileRequestDTO.getBranchName())
                    .commitMsg(scmCreateFileRequestDTO.getCommitMessage())
                    .completeFilePath(scmCreateFileRequestDTO.getFilePath())
                    .isNewBranch(scmCreateFileRequestDTO.isCommitToNewBranch())
                    .scmConnector(scmConnector)
                    .yaml(scmCreateFileRequestDTO.getFileContent())
                    .build()),
            scope.getProjectIdentifier(), scope.getOrgIdentifier(), scope.getAccountIdentifier(),
            scmCreateFileRequestDTO.getConnectorRef());

    // Put Error Handling

    return ScmCommitFileResponseDTO.builder()
        .commitId(createFileResponse.getCommitId())
        .blobId(createFileResponse.getBlobId())
        .build();
  }

  @Override
  public ScmCommitFileResponseDTO updateFile(ScmUpdateFileRequestDTO scmUpdateFileRequestDTO) {
    Scope scope = scmUpdateFileRequestDTO.getScope();
    // TODO Put validations over request here
    ScmConnector scmConnector = gitSyncConnectorHelper.getScmConnectorForGivenRepo(scope.getAccountIdentifier(),
        scope.getOrgIdentifier(), scope.getProjectIdentifier(), scmUpdateFileRequestDTO.getConnectorRef(),
        scmUpdateFileRequestDTO.getRepoName());
    UpdateFileResponse updateFileResponse =
        scmOrchestratorService.processScmRequestUsingConnectorSettings(scmClientFacilitatorService
            -> scmClientFacilitatorService.updateFile(
                InfoForGitPush.builder()
                    .accountId(scope.getAccountIdentifier())
                    .orgIdentifier(scope.getOrgIdentifier())
                    .projectIdentifier(scope.getProjectIdentifier())
                    .filePath(scmUpdateFileRequestDTO.getFilePath())
                    .baseBranch(scmUpdateFileRequestDTO.getBranchName())
                    .branch(scmUpdateFileRequestDTO.isCommitToNewBranch() ? scmUpdateFileRequestDTO.getNewBranch()
                                                                          : scmUpdateFileRequestDTO.getBranchName())
                    .commitMsg(scmUpdateFileRequestDTO.getCommitMessage())
                    .completeFilePath(scmUpdateFileRequestDTO.getFilePath())
                    .isNewBranch(scmUpdateFileRequestDTO.isCommitToNewBranch())
                    .scmConnector(scmConnector)
                    .yaml(scmUpdateFileRequestDTO.getFileContent())
                    .build()),
            scope.getProjectIdentifier(), scope.getOrgIdentifier(), scope.getAccountIdentifier(),
            scmUpdateFileRequestDTO.getConnectorRef());

    // Put Error Handling

    if (scmUpdateFileRequestDTO.isCreatePR()) {
      createPR(ScmCreatePRRequestDTO.builder()
                   .connectorRef(scmUpdateFileRequestDTO.getConnectorRef())
                   .repoName(scmUpdateFileRequestDTO.getRepoName())
                   .scope(scmUpdateFileRequestDTO.getScope())
                   .sourceBranch(scmUpdateFileRequestDTO.getBranchName())
                   .targetBranch(scmUpdateFileRequestDTO.getNewBranch())
                   .build());
    }

    return ScmCommitFileResponseDTO.builder()
        .commitId(updateFileResponse.getCommitId())
        .blobId(updateFileResponse.getBlobId())
        .build();
  }

  @Override
  public ScmCreatePRResponseDTO createPR(ScmCreatePRRequestDTO scmCreatePRRequestDTO) {
    Scope scope = scmCreatePRRequestDTO.getScope();
    CreatePRDTO createPRDTO = scmOrchestratorService.processScmRequestUsingConnectorSettings(scmClientFacilitatorService
        -> scmClientFacilitatorService.createPullRequest(scope, scmCreatePRRequestDTO.getConnectorRef(),
            scmCreatePRRequestDTO.getRepoName(), scmCreatePRRequestDTO.getSourceBranch(),
            scmCreatePRRequestDTO.getTargetBranch(), scmCreatePRRequestDTO.getTitle()),
        scope.getProjectIdentifier(), scope.getOrgIdentifier(), scope.getAccountIdentifier(),
        scmCreatePRRequestDTO.getConnectorRef());
    return ScmCreatePRResponseDTO.builder().prNumber(createPRDTO.getPrNumber()).build();
  }
}
