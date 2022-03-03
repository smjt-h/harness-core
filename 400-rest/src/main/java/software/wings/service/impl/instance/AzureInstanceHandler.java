/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.instance;

import static io.harness.beans.FeatureName.AZURE_INFRA_PERPETUAL_TASK;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.validation.Validator.notNullCheck;

import static java.util.stream.Collectors.toSet;

import io.harness.beans.FeatureName;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.task.azure.AzureTaskExecutionResponse;
import io.harness.delegate.task.azure.response.AzureListVMTaskResponse;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.annotation.EncryptableSetting;
import software.wings.api.DeploymentInfo;
import software.wings.api.DeploymentSummary;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.ondemandrollback.OnDemandRollbackInfo;
import software.wings.beans.AzureInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.AzureVMSSInstanceInfo;
import software.wings.beans.infrastructure.instance.info.HostInstanceInfo;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.key.deployment.DeploymentKey;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.service.AzureInfraInstanceSyncPerpetualTaskCreator;
import software.wings.service.InstanceSyncPerpetualTaskCreator;

import com.google.inject.Inject;
import io.jsonwebtoken.lang.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AzureInstanceHandler extends InstanceHandler implements InstanceSyncByPerpetualTaskHandler {
  @Inject protected AzureHelperService azureHelperService;
  @Inject private AzureInfraInstanceSyncPerpetualTaskCreator perpetualTaskCreator;

  @Override
  public void syncInstances(String appId, String infraMappingId, InstanceSyncFlow instanceSyncFlow) {
    syncInstancesInternal(appId, infraMappingId, instanceSyncFlow, Optional.empty());
  }

  private void syncInstancesInternal(String appId, String infraMappingId, InstanceSyncFlow instanceSyncFlow,
      Optional<AzureTaskExecutionResponse> response) {
    InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, infraMappingId);
    notNullCheck("Infra mapping is null for id:" + infraMappingId, infrastructureMapping);

    if (!(infrastructureMapping instanceof AzureInfrastructureMapping)) {
      String msg = "Incompatible infra mapping type. Expecting AZURE_INFRA type. Found:"
          + infrastructureMapping.getInfraMappingType();
      log.error(msg);
      throw WingsException.builder().message(msg).build();
    }

    AzureInfrastructureMapping azureInfrastructureMapping = (AzureInfrastructureMapping) infrastructureMapping;

    Map<String, Instance> azureInstanceIdInstanceMap = new HashMap<>();
    loadInstanceMapBasedOnType(appId, azureInfrastructureMapping.getUuid(), azureInstanceIdInstanceMap);

    log.info(
        "Found {} azure instances for app {}, flow: {}", azureInstanceIdInstanceMap.size(), appId, instanceSyncFlow);

    boolean canUpdateDb = canUpdateInstancesInDb(instanceSyncFlow, infrastructureMapping.getAccountId());

    if (azureInstanceIdInstanceMap.size() > 0 && canUpdateDb) {
      handleInstanceSync(azureInstanceIdInstanceMap, azureInfrastructureMapping, response);
    }
  }

  protected void loadInstanceMapBasedOnType(
      String appId, String infraMappingId, Map<String, Instance> azureInstanceIdInstanceMap) {
    List<Instance> instanceList = getInstances(appId, infraMappingId);
    instanceList.forEach(instance -> {
      InstanceInfo instanceInfo = instance.getInstanceInfo();
      if (instanceInfo instanceof HostInstanceInfo) {
        HostInstanceInfo hostInstanceInfo = (HostInstanceInfo) instanceInfo;
        String hostName = hostInstanceInfo.getHostName();
        azureInstanceIdInstanceMap.put(hostName, instance);
      }

      if (instanceInfo instanceof AzureVMSSInstanceInfo) {
        AzureVMSSInstanceInfo hostInstanceInfo = (AzureVMSSInstanceInfo) instanceInfo;
        String hostName = hostInstanceInfo.getHost();
        azureInstanceIdInstanceMap.put(hostName, instance);
      }
    });
  }

  protected void handleInstanceSync(Map<String, Instance> azureInstanceIdInstanceMap,
      AzureInfrastructureMapping azureInfrastructureMapping, Optional<AzureTaskExecutionResponse> response) {
    if (azureInstanceIdInstanceMap.size() > 0) {
      SettingAttribute cloudProviderSetting =
          settingsService.get(azureInfrastructureMapping.getComputeProviderSettingId());
      List<EncryptedDataDetail> encryptedDataDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) cloudProviderSetting.getValue(), null, null);

      List<Host> activeHostList;
      if (response.isPresent() && null != response.get().getAzureTaskResponse()) {
        activeHostList = ((AzureListVMTaskResponse) response.get().getAzureTaskResponse()).getHosts();
      } else {
        activeHostList =
            azureHelperService.listHosts(azureInfrastructureMapping, cloudProviderSetting, encryptedDataDetails, null);
      }
      deleteRunningInstancesFromMap(azureInstanceIdInstanceMap, activeHostList);
    }
  }

  private void deleteRunningInstancesFromMap(
      Map<String, Instance> azureInstanceIdInstanceMap, List<Host> activeHostList) {
    Instance azureInstance = azureInstanceIdInstanceMap.values().iterator().next();
    log.info(
        "Total no of Azure instances found in DB for InfraMappingId: {} and AppId: {}: {}, No of Running instances found in azure:{}",
        azureInstance.getInfraMappingId(), azureInstance.getAppId(), azureInstanceIdInstanceMap.size(),
        activeHostList.size());

    azureInstanceIdInstanceMap.keySet().removeAll(activeHostList.stream().map(Host::getHostName).collect(toSet()));

    Set<String> instanceIdsToBeDeleted = azureInstanceIdInstanceMap.entrySet()
                                             .stream()
                                             .map(entry -> entry.getValue().getUuid())
                                             .collect(Collectors.toSet());

    if (isNotEmpty(instanceIdsToBeDeleted)) {
      log.info("Instances to be deleted {}", instanceIdsToBeDeleted.size());
      instanceService.delete(instanceIdsToBeDeleted);
    }
  }

  @Override
  public void handleNewDeployment(
      List<DeploymentSummary> deploymentSummaries, boolean rollback, OnDemandRollbackInfo onDemandRollbackInfo) {
    // All the new deployments are either handled at AzureInstanceHandler or InstanceHelper
    throw WingsException.builder().message("Deployments should be handled at InstanceHelper for azure infra.").build();
  }

  @Override
  public FeatureName getFeatureFlagToStopIteratorBasedInstanceSync() {
    return FeatureName.STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AZURE_INFRA_DEPLOYMENTS;
  }

  @Override
  public Optional<List<DeploymentInfo>> getDeploymentInfo(PhaseExecutionData phaseExecutionData,
      PhaseStepExecutionData phaseStepExecutionData, WorkflowExecution workflowExecution,
      InfrastructureMapping infrastructureMapping, String stateExecutionInstanceId, Artifact artifact) {
    log.warn("Deployments should be handled at InstanceHelper for azure type.");
    return Optional.empty();
  }

  @Override
  public DeploymentKey generateDeploymentKey(DeploymentInfo deploymentInfo) {
    return null;
  }

  @Override
  protected void setDeploymentKey(DeploymentSummary deploymentSummary, DeploymentKey deploymentKey) {
    // Do Nothing
  }

  @Override
  public FeatureName getFeatureFlagToEnablePerpetualTaskForInstanceSync() {
    return AZURE_INFRA_PERPETUAL_TASK;
  }

  @Override
  public InstanceSyncPerpetualTaskCreator getInstanceSyncPerpetualTaskCreator() {
    return perpetualTaskCreator;
  }

  @Override
  public void processInstanceSyncResponseFromPerpetualTask(
      InfrastructureMapping infrastructureMapping, DelegateResponseData response) {
    syncInstancesInternal(infrastructureMapping.getAppId(), infrastructureMapping.getUuid(),
        InstanceSyncFlow.PERPETUAL_TASK, Optional.of((AzureTaskExecutionResponse) response));
  }

  @Override
  public Status getStatus(InfrastructureMapping infrastructureMapping, DelegateResponseData response) {
    AzureTaskExecutionResponse azureTaskExecutionResponse = (AzureTaskExecutionResponse) response;
    boolean success = azureTaskExecutionResponse.getCommandExecutionStatus() == SUCCESS;
    String errorMessage = success ? null : azureTaskExecutionResponse.getErrorMessage();
    boolean canDeleteTask = success
        && (azureTaskExecutionResponse.getAzureTaskResponse() == null
            || Collections.isEmpty(
                ((AzureListVMTaskResponse) azureTaskExecutionResponse.getAzureTaskResponse()).getHosts()));
    if (canDeleteTask) {
      log.info("[Azure Infra Sync]: Got 0 instances. Infrastructure Mapping : [{}]", infrastructureMapping.getUuid());
    }
    return Status.builder().success(success).errorMessage(errorMessage).retryable(!canDeleteTask).build();
  }
}
