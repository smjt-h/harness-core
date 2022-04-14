/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.scm.adapter;

import io.harness.delegate.beans.connector.scm.AzureRepoConnectionType;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoConnectorDTO;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoSshCredentialsDTO;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoUsernameTokenDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.*;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;

public class AzureRepoTOGitMapper {
  public static GitConfigDTO mapToGitConfigDTO(AzureRepoConnectorDTO azureRepoConnectorDTO) {
    final GitAuthType authType = azureRepoConnectorDTO.getAuthentication().getAuthType();
    final GitConnectionType connectionType = azureRepoConnectorDTO.getConnectionType();
    final String url = azureRepoConnectorDTO.getUrl();
    final String validationRepo = azureRepoConnectorDTO.getValidationRepo();
    if (authType == GitAuthType.HTTP) {
      final AzureRepoHttpCredentialsDTO httpCredentialsSpec =
          ((AzureRepoHttpCredentialsDTO) azureRepoConnectorDTO.getAuthentication().getCredentials());
      final AzureRepoUsernameTokenDTO usernamePasswordDTO =
          (AzureRepoUsernameTokenDTO) httpCredentialsSpec.getHttpCredentialsSpec();
      return GitConfigCreater.getGitConfigForHttp(connectionType, url, validationRepo,
          usernamePasswordDTO.getUsername(), usernamePasswordDTO.getUsernameRef(), usernamePasswordDTO.getTokenRef(),
          azureRepoConnectorDTO.getDelegateSelectors());
    } else if (authType == GitAuthType.SSH) {
      final AzureRepoSshCredentialsDTO sshCredentials =
          (AzureRepoSshCredentialsDTO) azureRepoConnectorDTO.getAuthentication().getCredentials();
      final SecretRefData sshKeyRef = sshCredentials.getSshKeyRef();
      return GitConfigCreater.getGitConfigForSsh(
          connectionType, url, validationRepo, sshKeyRef, azureRepoConnectorDTO.getDelegateSelectors());
    }
    throw new InvalidRequestException("Unknown auth type: " + authType);
  }
}
