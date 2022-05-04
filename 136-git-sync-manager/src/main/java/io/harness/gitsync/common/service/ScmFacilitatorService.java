/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.dtos.GitBranchesResponseDTO;
import io.harness.gitsync.common.dtos.GitRepositoryResponseDTO;
import io.harness.ng.beans.PageRequest;

import java.util.List;

@OwnedBy(HarnessTeam.PL)
public interface ScmFacilitatorService {
  List<String> listBranchesUsingConnector(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String connectorIdentifierRef, String repoURL, PageRequest pageRequest, String searchTerm);

  List<GitRepositoryResponseDTO> listReposByRefConnector(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String connectorRef, PageRequest pageRequest, String searchTerm);

  GitBranchesResponseDTO listBranchesV2(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String connectorRef, String repoName, PageRequest pageRequest, String searchTerm);
}
