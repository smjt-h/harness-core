package io.harness.cdng.gitOps;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.visitor.helpers.cdstepinfo.GitOpsConfigUpdateStepInfoVisitorHelper;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.GITOPS_CONFIG_UPDATE)
@SimpleVisitorHelper(helperClass = GitOpsConfigUpdateStepInfoVisitorHelper.class)
@TypeAlias("GitOpsConfigUpdateStepInfo")
@RecasterAlias("io.harness.cdng.gitOps.GitOpsConfigUpdateStepInfo")
public class GitOpsConfigUpdateStepInfo extends GitOpsConfigUpdateBaseStepInfo implements CDStepInfo, Visitable {
  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Builder(builderMethodName = "infoBuilder")
  public GitOpsConfigUpdateStepInfo(ParameterField<List<TaskSelectorYaml>> delegateSelectors, ParameterField<Map<String, String>> stringMap, ParameterField<StoreConfigWrapper> store) {
    super(delegateSelectors, stringMap, store);
  }

  @Override
  public StepType getStepType() {
    return GitOpsConfigUpdate.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK_CHAIN;
  }

  @Override
  public SpecParameters getSpecParameters() {
    return GitOpsConfigUpdateStepParams.infoBuilder()
        .delegateSelectors(delegateSelectors)
        .stringMap(stringMap)
        .store(store)
        .build();
  }
}
