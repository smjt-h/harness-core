package io.harness.delegate.beans.connector.pdcconnector;

import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.expression.ExpressionEvaluator;

import java.util.List;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class PhysicalDataCenterConnectorValidationParams
    implements ConnectorValidationParams, ExecutionCapabilityDemander {
  PhysicalDataCenterConnectorDTO physicalDataCenterConnectorDTO;
  String connectorName;

  @Override
  public ConnectorType getConnectorType() {
    return ConnectorType.PDC;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return PhysicalDataCenterConnectorCapabilityHelper.fetchRequiredExecutionCapabilities(
        physicalDataCenterConnectorDTO, maskingEvaluator);
  }
}
