/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.network.SafeHttpCall.execute;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.PageResponse;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.delegate.task.azure.response.AzureListVMTaskResponse;
import io.harness.grpc.utils.AnyUtils;
import io.harness.logging.CommandExecutionStatus;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.perpetualtask.instancesync.AzureInfraInstanceSyncPerpetualTaskParams;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;

import software.wings.beans.AzureInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.helpers.ext.azure.AzureHelperService;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Response;

@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class AzureInfraInstanceSyncExecutor implements PerpetualTaskExecutor {
  @Inject private AzureHelperService azureHelperService;
  @Inject private DelegateAgentManagerClient delegateAgentManagerClient;
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    final AzureInfraInstanceSyncPerpetualTaskParams instanceSyncParams =
        AnyUtils.unpack(params.getCustomizedParams(), AzureInfraInstanceSyncPerpetualTaskParams.class);

    final AzureInfrastructureMapping azureInfrastructureMapping = (AzureInfrastructureMapping) kryoSerializer.asObject(
        instanceSyncParams.getAzureInfrastructureMapping().toByteArray());
    final List<EncryptedDataDetail> encryptedDataDetails =
        cast(kryoSerializer.asObject(instanceSyncParams.getEncryptedData().toByteArray()));

    final SettingAttribute computeProviderSetting =
        cast(kryoSerializer.asObject(instanceSyncParams.getComputeProviderSetting().toByteArray()));

    AzureTaskExecutionResponse response =
        getInstances(azureInfrastructureMapping, encryptedDataDetails, computeProviderSetting);
    try {
      execute(delegateAgentManagerClient.publishInstanceSyncResult(
          taskId.getId(), azureInfrastructureMapping.getAccountId(), response));
    } catch (Exception e) {
      log.error(String.format(
                    "[Azure Infra Sync]: Failed to publish the instance collection result to manager for taskId [%s]",
                    taskId.getId()),
          e);
    }

    return getPerpetualTaskResponse(response);
  }

  private AzureTaskExecutionResponse getInstances(AzureInfrastructureMapping azureInfrastructureMapping,
      List<EncryptedDataDetail> encryptedDataDetails, SettingAttribute computeProviderSetting) {
    AzureTaskExecutionResponse.AzureTaskExecutionResponseBuilder builder = AzureTaskExecutionResponse.builder();
    try {
      PageResponse<Host> response =
          azureHelperService.listHosts(azureInfrastructureMapping, computeProviderSetting, encryptedDataDetails, null);
      List<Host> hosts = response.getResponse();
      builder.commandExecutionStatus(CommandExecutionStatus.SUCCESS);
      builder.azureTaskResponse(AzureListVMTaskResponse.builder().hosts(hosts).build());
    } catch (Exception ex) {
      log.error("[Azure Infra Sync]: Failed to list hosts for infraId: {}", azureInfrastructureMapping.getUuid(), ex);
      builder.errorMessage(ex.getMessage());
      builder.commandExecutionStatus(CommandExecutionStatus.FAILURE);
    }

    AzureTaskExecutionResponse response = builder.build();
    return response;
  }

  private PerpetualTaskResponse getPerpetualTaskResponse(AzureTaskExecutionResponse response) {
    String message = "success";
    if (response.getCommandExecutionStatus() == CommandExecutionStatus.FAILURE) {
      message = response.getErrorMessage();
    }

    return PerpetualTaskResponse.builder().responseCode(Response.SC_OK).responseMessage(message).build();
  }

  @SuppressWarnings("unchecked")
  private static <T extends List<?>> T cast(Object obj) {
    return (T) obj;
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return false;
  }
}
