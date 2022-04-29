/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.serverless;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.cdng.visitor.helpers.cdstepinfo.ServerlessAwsLambdaRollbackStepInfoVisitorHelper;
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
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SimpleVisitorHelper(helperClass = ServerlessAwsLambdaRollbackStepInfoVisitorHelper.class)
@JsonTypeName(StepSpecTypeConstants.SERVERLESS_AWS_LAMBDA_ROLLBACK)
@TypeAlias("serverlessAwsLambdaRollbackStepInfo")
@RecasterAlias("io.harness.cdng.serverless.ServerlessAwsLambdaRollbackStepInfo")
public class ServerlessAwsLambdaRollbackStepInfo
    extends ServerlessAwsLambdaRollbackBaseStepInfo implements CDStepInfo, Visitable {
  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Builder(builderMethodName = "infoBuilder")
  public ServerlessAwsLambdaRollbackStepInfo(
      ParameterField<List<TaskSelectorYaml>> delegateSelectors, String serverlessAwsLambdaRollbackFnq) {
    super(delegateSelectors, serverlessAwsLambdaRollbackFnq);
  }

  @Override
  public StepType getStepType() {
    return ServerlessAwsLambdaRollbackStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK;
  }

  @Override
  public SpecParameters getSpecParameters() {
    return ServerlessAwsLambdaRollbackStepParameters.infoBuilder()
        .delegateSelectors(this.getDelegateSelectors())
        .build();
  }
}
