/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.scm.bitbucket;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.DelegateSelectable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.git.GitClientHelper;
import io.harness.gitsync.beans.GitRepositoryDTO;
import io.harness.utils.FilePathUtils;

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
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("BitbucketConnector")
@OwnedBy(HarnessTeam.DX)
@Schema(name = "BitbucketConnector", description = "This contains details of Bitbucket connectors")
public class BitbucketConnectorDTO extends ConnectorConfigDTO implements ScmConnector, DelegateSelectable {
  @NotNull @JsonProperty("type") private GitConnectionType connectionType;
  @NotNull @NotBlank private String url;
  private String validationRepo;
  @Valid @NotNull private BitbucketAuthenticationDTO authentication;
  @Valid private BitbucketApiAccessDTO apiAccess;
  private Set<String> delegateSelectors;

  @Builder
  public BitbucketConnectorDTO(GitConnectionType connectionType, String url, String validationRepo,
      BitbucketAuthenticationDTO authentication, BitbucketApiAccessDTO apiAccess, Set<String> delegateSelectors) {
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
      BitbucketHttpCredentialsSpecDTO httpCredentialsSpec =
          ((BitbucketHttpCredentialsDTO) authentication.getCredentials()).getHttpCredentialsSpec();
      if (httpCredentialsSpec != null) {
        decryptableEntities.add(httpCredentialsSpec);
      }
    } else {
      BitbucketSshCredentialsDTO sshCredential = (BitbucketSshCredentialsDTO) authentication.getCredentials();
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
    return ConnectorType.BITBUCKET;
  }

  @Override
  public String getGitConnectionUrl(String repoName) {
    if (connectionType == GitConnectionType.REPO) {
      return getUrl();
    }
    return FilePathUtils.addEndingSlashIfMissing(getUrl()) + repoName;
  }

  @Override
  public GitRepositoryDTO getGitRepositoryDetails() {
    if (GitConnectionType.REPO.equals(connectionType)) {
      return GitRepositoryDTO.builder()
          .name(GitClientHelper.getGitRepo(url))
          .org(GitClientHelper.getGitOwner(url, false))
          .build();
    }
    return GitRepositoryDTO.builder().org(GitClientHelper.getGitOwner(url, true)).build();
  }
}
