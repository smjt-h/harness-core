/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.artifacts.artifactory;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;

/**
 * DTO object to be passed to delegate tasks.
 */
@Value
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.CDP)
public class ArtifactoryGenericArtifactDelegateRequest extends ArtifactoryBaseArtifactDelegateRequest {
  /** Images in repos need to be referenced via a path. */
  String artifactDirectory;
  String artifactPathFilter;
  ArtifactSourceType sourceType;

  public ArtifactoryGenericArtifactDelegateRequest(ArtifactoryBaseArtifactDelegateRequestBuilder<?, ?> b, String artifactDirectory, String artifactPathFilter, ArtifactSourceType sourceType) {
    super(b);
    this.artifactDirectory = artifactDirectory;
    this.artifactPathFilter = artifactPathFilter;
    this.sourceType = sourceType;
  }

  public ArtifactoryGenericArtifactDelegateRequest(String repositoryName, String artifactPath, String repositoryFormat, String connectorRef, List<EncryptedDataDetail> encryptedDataDetails, ArtifactoryConnectorDTO artifactoryConnectorDTO, ArtifactSourceType sourceType, String artifactDirectory, String artifactPathFilter, ArtifactSourceType sourceType1) {
    super(repositoryName, artifactPath, repositoryFormat, connectorRef, encryptedDataDetails, artifactoryConnectorDTO, sourceType);
    this.artifactDirectory = artifactDirectory;
    this.artifactPathFilter = artifactPathFilter;
    this.sourceType = sourceType1;
  }
}
