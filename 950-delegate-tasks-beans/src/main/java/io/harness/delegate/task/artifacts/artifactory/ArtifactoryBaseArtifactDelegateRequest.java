/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.artifacts.artifactory;

import static io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper.populateDelegateSelectorCapability;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.capability.EncryptedDataDetailsCapabilityHelper;
import io.harness.delegate.task.artifacts.ArtifactSourceDelegateRequest;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;

/**
 * DTO object to be passed to delegate tasks.
 */
@Value
@NonFinal
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
@OwnedBy(HarnessTeam.CDP)
public class ArtifactoryBaseArtifactDelegateRequest implements ArtifactSourceDelegateRequest {
  /** Repository name */
  String repositoryName;
  String artifactPath;
  /** Repository format - package type */
  String repositoryFormat;
  String connectorRef;
  /** Encrypted details for decrypting.*/
  List<EncryptedDataDetail> encryptedDataDetails;
  /** Artifactory Connector*/
  ArtifactoryConnectorDTO artifactoryConnectorDTO;
  /** Artifact Source type.*/
  ArtifactSourceType sourceType;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> capabilities =
        new ArrayList<>(EncryptedDataDetailsCapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(
            encryptedDataDetails, maskingEvaluator));
    populateDelegateSelectorCapability(capabilities, artifactoryConnectorDTO.getDelegateSelectors());
    capabilities.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        artifactoryConnectorDTO.getArtifactoryServerUrl(), maskingEvaluator));
    return capabilities;
  }

  public ArtifactoryBaseArtifactDelegateRequest(String repositoryName, String artifactPath, String repositoryFormat, String connectorRef, List<EncryptedDataDetail> encryptedDataDetails, ArtifactoryConnectorDTO artifactoryConnectorDTO, ArtifactSourceType sourceType) {
    this.repositoryName = repositoryName;
    this.artifactPath = artifactPath;
    this.repositoryFormat = repositoryFormat;
    this.connectorRef = connectorRef;
    this.encryptedDataDetails = encryptedDataDetails;
    this.artifactoryConnectorDTO = artifactoryConnectorDTO;
    this.sourceType = sourceType;
  }
}
