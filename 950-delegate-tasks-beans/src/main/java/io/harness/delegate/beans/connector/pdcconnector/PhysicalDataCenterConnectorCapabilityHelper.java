package io.harness.delegate.beans.connector.pdcconnector;

import io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.SocketConnectivityCapabilityGenerator;
import io.harness.delegate.task.utils.PhysicalDataCenterUtils;
import io.harness.expression.ExpressionEvaluator;

import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PhysicalDataCenterConnectorCapabilityHelper extends ConnectorCapabilityBaseHelper {
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      PhysicalDataCenterConnectorDTO physicalDataCenterConnectorDTO, ExpressionEvaluator maskingEvaluator) {
    List<String> hosts = PhysicalDataCenterUtils.getHostsAsList(physicalDataCenterConnectorDTO.hostNames);
    List<ExecutionCapability> capabilityList =
        hosts.stream()
            .map(host
                -> SocketConnectivityCapabilityGenerator.buildSocketConnectivityCapability(
                    host, PhysicalDataCenterUtils.getPortOrSSHDefault(host)))
            .collect(Collectors.toList());

    populateDelegateSelectorCapability(capabilityList, physicalDataCenterConnectorDTO.getDelegateSelectors());
    return capabilityList;
  }
}
