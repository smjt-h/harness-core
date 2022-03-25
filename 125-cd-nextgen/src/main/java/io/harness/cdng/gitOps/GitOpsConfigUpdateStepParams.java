package io.harness.cdng.gitOps;

import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;

import software.wings.beans.command.GitOpsDummyCommandUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.Builder;

public class GitOpsConfigUpdateStepParams extends GitOpsConfigUpdateBaseStepInfo implements GitOpsSpecParams {
  @Builder(builderMethodName = "infoBuilder")
  public GitOpsConfigUpdateStepParams(ParameterField<List<TaskSelectorYaml>> delegateSelectors, ParameterField<Map<String, String>> stringMap, ParameterField<StoreConfigWrapper> store) {
    super(delegateSelectors, stringMap, store);
  }

  @Nonnull
  @Override
  @JsonIgnore
  public List<String> getCommandUnits() {
    return Arrays.asList(
        GitOpsDummyCommandUnit.FetchFiles, GitOpsDummyCommandUnit.UpdateFiles, GitOpsDummyCommandUnit.Commit);
  }
}
