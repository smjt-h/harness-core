/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.aws.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.common.resources.AwsResourceServiceHelper;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.connector.awsconnector.*;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.exception.AwsIAMRolesException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.BaseNGAccess;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.CloudFormationSourceType;

import software.wings.beans.TaskType;

import com.amazonaws.services.cloudformation.model.Capability;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import software.wings.service.impl.GitConfigHelperService;
import software.wings.service.impl.aws.model.AwsCFTemplateParamsData;
import software.wings.utils.GitUtilsManager;


import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@OwnedBy(CDP)
public class AwsHelperResourceServiceImpl implements AwsHelperResourceService {
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

  public List<AwsCFTemplateParamsData> awsCFParameterKeys(
          String type, String region, String sourceRepoSettingId, String templatePath, String commitId,
          String sourceRepoBranch, IdentifierRef awsConnectorRef,
          String orgIdentifier, String projectIdentifier, String data, ConnectorInfoDTO connectorDTO
  ) {
    GitStoreDelegateConfig gitStoreDelegateConfig = null;
    switch (CloudFormationSourceType.getSourceType(type)) {
      case CloudFormationSourceType.UNKNOWN.toString():
        throw new InvalidRequestException("Unknown source type");
        break;
      case CloudFormationSourceType.GIT.name():
        if (isEmpty(sourceRepoSettingId) || (isEmpty(sourceRepoBranch) && isEmpty(commitId))) {
          throw new InvalidRequestException("Empty Fields Connector Id or both Branch and commitID");
        }

        GitConfigDTO gitConfigDTO = ScmConnectorMapper.toGitConfigDTO((ScmConnector) connectorDTO.getConnectorConfig());
        GitStoreDelegateConfig.builder()
                .gitConfigDTO(gitConfigDTO)
                .branch(sourceRepoBranch)
                .commitId(commitId)
                .path(templatePath)
                .connectorName(connectorDTO.getName())
                .build();
        break;
      default:
        if (isEmpty(data)) {
          throw new InvalidRequestException("Empty Data Field, Template body or Template url");
        }
    }
    AwsConnectorDTO awsConnector = serviceHelper.getAwsConnector(awsConnectorRef);
    BaseNGAccess access =
            serviceHelper.getBaseNGAccess(awsConnectorRef.getAccountIdentifier(), orgIdentifier, projectIdentifier);
    List<EncryptedDataDetail> encryptedData = serviceHelper.getAwsEncryptionDetails(awsConnector, access);

    AwsCFTaskParamsRequest params = AwsCFTaskParamsRequest.builder()
            .awsConnector(awsConnector)
            .awsTaskType(AwsTaskType.CF_LIST_PARAMS)
            .data(data)
            .encryptionDetails(encryptedData)
            .gitStoreDelegateConfig(gitStoreDelegateConfig)
            .region(region)
            .build();

      executeSyncTask(params, access);
      return null;
  }

  protected AwsIAMRolesResponse executeSyncTask(AwsTaskParams awsTaskParams, BaseNGAccess baseNGAccess) {
    DelegateResponseData responseData =
        serviceHelper.getResponseData(baseNGAccess, awsTaskParams, TaskType.NG_AWS_TASK.name());
    return getTaskExecutionResponse(responseData);
  }

  protected AwsCFTaskResponse executeSyncTask(AwsCFTaskParamsRequest awsTaskParams, BaseNGAccess baseNGAccess) {
    DelegateResponseData responseData =
            serviceHelper.getResponseData(baseNGAccess, awsTaskParams, TaskType.NG_AWS_TASK.name());
    return (AwsCFTaskResponse) responseData;
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
