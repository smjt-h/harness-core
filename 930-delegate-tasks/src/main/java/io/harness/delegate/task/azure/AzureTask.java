/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.azure;

import io.harness.azure.client.AzureManagementClient;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorCredentialDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureTaskParams;
import io.harness.delegate.beans.connector.azureconnector.AzureTaskType;
import io.harness.delegate.beans.connector.azureconnector.AzureValidateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class AzureTask extends AbstractDelegateRunnableTask {
  @Inject private AzureNgConfigMapper azureNgConfigMapper;
  @Inject private AzureManagementClient azureManagementClient;

  public AzureTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new UnsupportedOperationException("Object Array parameters not supported");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    if (!(parameters instanceof AzureTaskParams)) {
      throw new InvalidRequestException("Task Params are not of expected type: AzureTaskParameters");
    }
    AzureTaskParams azureTaskParams = (AzureTaskParams) parameters;
    if (azureTaskParams.getAzureTaskType() == AzureTaskType.VALIDATE) {
      return handleValidateTask(azureTaskParams);
    } else {
      throw new InvalidRequestException("Task type not identified");
    }
  }

  public DelegateResponseData handleValidateTask(AzureTaskParams azureTaskParams) {
    final AzureConnectorDTO azureConnector = azureTaskParams.getAzureConnector();
    final AzureConnectorCredentialDTO credential = azureConnector.getCredential();
    azureManagementClient.validateAzureConnection(
        azureNgConfigMapper.mapAzureConfigWithDecryption(credential, azureTaskParams.getEncryptionDetails()));
    ConnectorValidationResult connectorValidationResult = ConnectorValidationResult.builder()
                                                              .status(ConnectivityStatus.SUCCESS)
                                                              .delegateId(getDelegateId())
                                                              .testedAt(System.currentTimeMillis())
                                                              .build();
    return AzureValidateTaskResponse.builder().connectorValidationResult(connectorValidationResult).build();
  }
}
