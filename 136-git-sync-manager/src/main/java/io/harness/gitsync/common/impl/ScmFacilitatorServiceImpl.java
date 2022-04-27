/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.impl;

import io.harness.ScopeIdentifiers;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.gitsync.common.beans.InfoForGitPush;
import io.harness.gitsync.common.dtos.ScmCommitFileResponseDTO;
import io.harness.gitsync.common.dtos.ScmCreateFileRequestDTO;
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
    ScopeIdentifiers scopeIdentifiers = scmCreateFileRequestDTO.getScopeIdentifiers();
    // TODO Check connector fetch logic
    ScmConnector scmConnector =
        gitSyncConnectorHelper.getScmConnectorForGivenRepo(scopeIdentifiers.getAccountIdentifier(),
            scopeIdentifiers.getOrgIdentifier(), scopeIdentifiers.getProjectIdentifier(),
            scmCreateFileRequestDTO.getConnectorRef(), scmCreateFileRequestDTO.getRepoName());
    CreateFileResponse createFileResponse =
        scmOrchestratorService.processScmRequestUsingConnectorSettings(scmClientFacilitatorService
            -> scmClientFacilitatorService.createFile(InfoForGitPush.builder()
                                                          .accountId(scopeIdentifiers.getAccountIdentifier())
                                                          .orgIdentifier(scopeIdentifiers.getOrgIdentifier())
                                                          .projectIdentifier(scopeIdentifiers.getProjectIdentifier())
                                                          .filePath(scmCreateFileRequestDTO.getFilePath())
                                                          .baseBranch(scmCreateFileRequestDTO.getBranchName())
                                                          .branch(scmCreateFileRequestDTO.getNewBranch())
                                                          .commitMsg(scmCreateFileRequestDTO.getCommitMessage())
                                                          .filePathV2(scmCreateFileRequestDTO.getFilePath())
                                                          .isNewBranch(scmCreateFileRequestDTO.isCommitToNewBranch())
                                                          .scmConnector(scmConnector)
                                                          .build()),
            scopeIdentifiers.getProjectIdentifier(), scopeIdentifiers.getOrgIdentifier(),
            scopeIdentifiers.getAccountIdentifier(), scmCreateFileRequestDTO.getConnectorRef());

    // Put Error Handling

    return ScmCommitFileResponseDTO.builder()
        .commitId(createFileResponse.getCommitId())
        .blobId(createFileResponse.getBlobId())
        .build();
  }

  @Override
  public ScmCommitFileResponseDTO updateFile(ScmUpdateFileRequestDTO scmUpdateFileRequestDTO) {
    ScopeIdentifiers scopeIdentifiers = scmUpdateFileRequestDTO.getScopeIdentifiers();
    ScmConnector scmConnector =
        gitSyncConnectorHelper.getScmConnectorForGivenRepo(scopeIdentifiers.getAccountIdentifier(),
            scopeIdentifiers.getOrgIdentifier(), scopeIdentifiers.getProjectIdentifier(),
            scmUpdateFileRequestDTO.getConnectorRef(), scmUpdateFileRequestDTO.getRepoName());
    UpdateFileResponse updateFileResponse =
        scmOrchestratorService.processScmRequestUsingConnectorSettings(scmClientFacilitatorService
            -> scmClientFacilitatorService.updateFile(InfoForGitPush.builder()
                                                          .accountId(scopeIdentifiers.getAccountIdentifier())
                                                          .orgIdentifier(scopeIdentifiers.getOrgIdentifier())
                                                          .projectIdentifier(scopeIdentifiers.getProjectIdentifier())
                                                          .filePath(scmUpdateFileRequestDTO.getFilePath())
                                                          .baseBranch(scmUpdateFileRequestDTO.getBranchName())
                                                          .branch(scmUpdateFileRequestDTO.getNewBranch())
                                                          .commitMsg(scmUpdateFileRequestDTO.getCommitMessage())
                                                          .filePathV2(scmUpdateFileRequestDTO.getFilePath())
                                                          .isNewBranch(scmUpdateFileRequestDTO.isCommitToNewBranch())
                                                          .scmConnector(scmConnector)
                                                          .build()),
            scopeIdentifiers.getProjectIdentifier(), scopeIdentifiers.getOrgIdentifier(),
            scopeIdentifiers.getAccountIdentifier(), scmUpdateFileRequestDTO.getConnectorRef());

    // Put Error Handling

    return ScmCommitFileResponseDTO.builder()
        .commitId(updateFileResponse.getCommitId())
        .blobId(updateFileResponse.getBlobId())
        .build();
  }
}
