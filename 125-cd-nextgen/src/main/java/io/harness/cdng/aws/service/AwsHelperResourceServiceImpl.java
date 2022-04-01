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
import io.harness.cdng.common.resources.GitResourceServiceHelper;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.connector.awsconnector.AwsCFTaskParamsRequest;
import io.harness.delegate.beans.connector.awsconnector.AwsCFTaskResponse;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsIAMRolesResponse;
import io.harness.delegate.beans.connector.awsconnector.AwsTaskParams;
import io.harness.delegate.beans.connector.awsconnector.AwsTaskType;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.exception.AwsCFException;
import io.harness.exception.AwsIAMRolesException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.BaseNGAccess;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.CloudFormationSourceType;
import software.wings.beans.TaskType;
import software.wings.service.impl.aws.model.AwsCFTemplateParamsData;

import com.amazonaws.services.cloudformation.model.Capability;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
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
  @Inject private GitResourceServiceHelper gitResourceServiceHelper;

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

  public List<AwsCFTemplateParamsData> awsCFParameterKeys(String type, String region, Boolean isBranch, String branch,
      String templatePath, String commitId, IdentifierRef awsConnectorRef, String dataInput, String connectorDTO) {
    GitStoreDelegateConfig gitStoreDelegateConfig = null;
    BaseNGAccess access = serviceHelper.getBaseNGAccess(awsConnectorRef.getAccountIdentifier(),
        awsConnectorRef.getOrgIdentifier(), awsConnectorRef.getProjectIdentifier());

    if (CloudFormationSourceType.UNKNOWN.toString().equalsIgnoreCase(type)) {
      throw new InvalidRequestException("Unknown source type");
    } else if (CloudFormationSourceType.GIT.toString().equalsIgnoreCase(type)) {
      if (isEmpty(branch) && isEmpty(commitId)) {
        throw new InvalidRequestException("Empty Fields Connector Id or both Branch and commitID");
      }
      FetchType fetchType = isBranch ? FetchType.BRANCH : FetchType.COMMIT;

      ConnectorInfoDTO connectorInfoDTO = gitResourceServiceHelper.getConnectorInfoDTO(connectorDTO, access);
      gitStoreDelegateConfig = gitResourceServiceHelper.getGitStoreDelegateConfig(
          connectorInfoDTO, access, fetchType, branch, commitId, templatePath);
    } else {
      if (isEmpty(dataInput)) {
        throw new InvalidRequestException("Data is empty");
      }
    }

    AwsConnectorDTO awsConnector = serviceHelper.getAwsConnector(awsConnectorRef);
    List<EncryptedDataDetail> encryptedData = serviceHelper.getAwsEncryptionDetails(awsConnector, access);

    AwsCFTaskParamsRequest params = AwsCFTaskParamsRequest.builder()
                                        .awsConnector(awsConnector)
                                        .awsTaskType(AwsTaskType.CF_LIST_PARAMS)
                                        .data(dataInput)
                                        .fileStoreType(type)
                                        .encryptionDetails(encryptedData)
                                        .gitStoreDelegateConfig(gitStoreDelegateConfig)
                                        .region(region)
                                        .accountId(awsConnectorRef.getAccountIdentifier())
                                        .build();

    AwsCFTaskResponse response = executeSyncTask(params, access);
    return getCFTaskExecutionResponse(response);
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

  private List<AwsCFTemplateParamsData> getCFTaskExecutionResponse(DelegateResponseData responseData) {
    if (responseData instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) responseData;
      throw new AwsCFException("Failed to get CloudFormation template parameters"
          + " : " + errorNotifyResponseData.getErrorMessage());
    }
    AwsCFTaskResponse response = (AwsCFTaskResponse) responseData;
    if (response.getCommandExecutionStatus() != SUCCESS) {
      throw new AwsCFException("Failed to get CloudFormation template parameters");
    }
    return response.getListOfParams();
  }
}
