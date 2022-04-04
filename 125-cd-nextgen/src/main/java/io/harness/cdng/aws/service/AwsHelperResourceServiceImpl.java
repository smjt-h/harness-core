/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.aws.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.common.resources.AwsResourceServiceHelper;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsIAMRolesResponse;
import io.harness.delegate.beans.connector.awsconnector.AwsTaskParams;
import io.harness.delegate.beans.connector.awsconnector.AwsTaskType;
import io.harness.exception.AwsIAMRolesException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.ng.core.BaseNGAccess;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.TaskType;

import com.amazonaws.services.cloudformation.model.Capability;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@OwnedBy(CDP)
public class AwsHelperResourceServiceImpl implements AwsHelperResourceService {
  private Map<String, String> regionMap;
  private static final String regionNode = "awsRegionIdToName";
  private static final String yamlResourceFilePath = "aws/aws.yaml";
  private final AwsResourceServiceHelper serviceHelper;

  @Inject
  AwsHelperResourceServiceImpl(AwsResourceServiceHelper serviceHelper) {
    this.serviceHelper = serviceHelper;
  }

  @Override
  public List<String> getCapabilities() {
    return Stream.of(Capability.values()).map(Capability::toString).collect(Collectors.toList());
  }

  @Override
  public Set<String> getCFStates() {
    return EnumSet.allOf(StackStatus.class).stream().map(Enum::name).collect(Collectors.toSet());
  }

  @Override
  public Map<String, String> getRegions() {
    if (regionMap == null) {
      try {
        ClassLoader classLoader = this.getClass().getClassLoader();
        String parsedYamlFile = Resources.toString(
            Objects.requireNonNull(classLoader.getResource(yamlResourceFilePath)), StandardCharsets.UTF_8);
        getMapRegionFromYaml(parsedYamlFile);
      } catch (IOException e) {
        throw new InvalidArgumentsException("Failed to read the region yaml file:" + e);
      }
    }
    return regionMap;
  }

  protected void getMapRegionFromYaml(String parsedYamlfile) {
    try {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      JsonNode node = mapper.readTree(parsedYamlfile);
      JsonNode regions = node.path(regionNode);
      mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
      regionMap = mapper.readValue(String.valueOf(regions), new TypeReference<Map<String, String>>() {});
    } catch (IOException e) {
      throw new InvalidArgumentsException("Failed to Deserialize the region yaml file:" + e);
    }
  }

  @Override
  public Map<String, String> getRolesARNs(
      IdentifierRef awsConnectorRef, String orgIdentifier, String projectIdentifier) {
    AwsConnectorDTO awsConnector = serviceHelper.getAwsConnector(awsConnectorRef);
    BaseNGAccess access =
        serviceHelper.getBaseNGAccess(awsConnectorRef.getAccountIdentifier(), orgIdentifier, projectIdentifier);
    List<EncryptedDataDetail> encryptedData = serviceHelper.getAwsEncryptionDetails(awsConnector, access);
    AwsTaskParams params = AwsTaskParams.builder()
                               .awsTaskType(AwsTaskType.LIST_IAM_ROLES)
                               .awsConnector(awsConnector)
                               .encryptionDetails(encryptedData)
                               .build();
    AwsIAMRolesResponse response = executeSyncTask(params, access);
    return response.getRoles();
  }

  protected AwsIAMRolesResponse executeSyncTask(AwsTaskParams awsTaskParams, BaseNGAccess baseNGAccess) {
    DelegateResponseData responseData =
        serviceHelper.getResponseData(baseNGAccess, awsTaskParams, TaskType.NG_AWS_TASK.name());
    return getTaskExecutionResponse(responseData);
  }

  private AwsIAMRolesResponse getTaskExecutionResponse(DelegateResponseData responseData) {
    if (responseData instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) responseData;
      throw new AwsIAMRolesException("Failed to get IAM roles"
          + " : " + errorNotifyResponseData.getErrorMessage());
    }
    AwsIAMRolesResponse response = (AwsIAMRolesResponse) responseData;
    if (response.getCommandExecutionStatus() != SUCCESS) {
      throw new AwsIAMRolesException("Failed to get IAM roles");
    }
    return response;
  }
}
