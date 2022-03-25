package io.harness.cdng.gitOps;

import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.yaml.ParameterField;

import software.wings.beans.command.GitOpsDummyCommandUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;

public interface GitOpsSpecParams extends SpecParameters {
  @JsonIgnore ParameterField<List<TaskSelectorYaml>> getDelegateSelectors();

  @Nonnull
  @JsonIgnore
  default List<String> getCommandUnits() {
    return Arrays.asList(
        GitOpsDummyCommandUnit.FetchFiles, GitOpsDummyCommandUnit.UpdateFiles, GitOpsDummyCommandUnit.Commit);
  }
}
