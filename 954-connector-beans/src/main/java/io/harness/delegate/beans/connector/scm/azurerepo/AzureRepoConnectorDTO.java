/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.scm.azurerepo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.DelegateSelectable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.gitsync.beans.GitRepositoryDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("AzureRepoConnector")
@OwnedBy(HarnessTeam.PL)
@Schema(name = "AzureRepoConfig", description = "This contains details of AzureRepo connector")
public class AzureRepoConnectorDTO extends ConnectorConfigDTO implements ScmConnector, DelegateSelectable {
  @NotNull
  @JsonProperty("type")
  @Schema(description = "Account | Repository connector type")
  GitConnectionType connectionType;
  @NotBlank @NotNull @Schema(description = "SSH | HTTP URL based on type of connection") String url;
  @Schema(description = "The repo to validate AzureRepo credentials. Only valid for Account type connector")
  String validationRepo;
  @Valid
  @NotNull
  @Schema(description = "Details for authentication mechanism for Azure Repo connector")
  AzureRepoAuthenticationDTO authentication;
  @Valid
  @Schema(description = "API access details, to be used in Harness Triggers and Git Experience")
  AzureRepoApiAccessDTO apiAccess;
  @Schema(description = "Selected Connectivity Modes") Set<String> delegateSelectors;

  @Builder
  public AzureRepoConnectorDTO(GitConnectionType connectionType, String url, String validationRepo,
      AzureRepoAuthenticationDTO authentication, AzureRepoApiAccessDTO apiAccess, Set<String> delegateSelectors,
      boolean executeOnDelegate) {
    this.connectionType = connectionType;
    this.url = url;
    this.validationRepo = validationRepo;
    this.authentication = authentication;
    this.apiAccess = apiAccess;
    this.delegateSelectors = delegateSelectors;
  }

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    List<DecryptableEntity> decryptableEntities = new ArrayList<>();
    if (authentication.getAuthType() == GitAuthType.HTTP) {
      AzureRepoHttpCredentialsSpecDTO httpCredentialsSpec =
          ((AzureRepoHttpCredentialsDTO) authentication.getCredentials()).getHttpCredentialsSpec();
      if (httpCredentialsSpec != null) {
        decryptableEntities.add(httpCredentialsSpec);
      }
    } else {
      AzureRepoSshCredentialsDTO sshCredential = (AzureRepoSshCredentialsDTO) authentication.getCredentials();
      if (sshCredential != null) {
        decryptableEntities.add(sshCredential);
      }
    }
    if (apiAccess != null && apiAccess.getSpec() != null) {
      decryptableEntities.add(apiAccess.getSpec());
    }
    return decryptableEntities;
  }

  @Override
  @JsonIgnore
  public ConnectorType getConnectorType() {
    return ConnectorType.AZURE_REPO;
  }

  @Override
  public String getGitConnectionUrl(String repoName) {
    return "";
  }

  @Override
  public GitRepositoryDTO getGitRepositoryDetails() {
    return GitRepositoryDTO.builder().build();
  }
}