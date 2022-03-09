package io.harness.cdng.provision.cloudformation;

import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

public class CloudformationStepHelperImpl implements CloudformationStepHelper {
  @Override
  public TaskChainResponse startChainLink(CloudformationStepExecutor cloudformationStepExecutor, Ambiance ambiance,
      StepElementParameters stepElementParameters) {
    return null;
  }

  @Override
  public TaskChainResponse executeNextLink(CloudformationStepExecutor cloudformationStepExecutor, Ambiance ambiance,
      StepElementParameters stepElementParameters, PassThroughData passThroughData,
      ThrowingSupplier<ResponseData> responseDataSupplier) {
    return null;
  }
}
