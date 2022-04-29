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
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;
import io.harness.serverless.ServerlessCommandUnitConstants;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeAlias("serverlessAwsLambdaRollbackStepParameters")
@RecasterAlias("io.harness.cdng.serverless.ServerlessAwsLambdaRollbackStepParameters")
public class ServerlessAwsLambdaRollbackStepParameters
    extends ServerlessAwsLambdaRollbackBaseStepInfo implements ServerlessSpecParameters {
  @Builder(builderMethodName = "infoBuilder")
  public ServerlessAwsLambdaRollbackStepParameters(
      ParameterField<List<TaskSelectorYaml>> delegateSelectors, String serverlessAwsLambdaRollbackFnq) {
    super(delegateSelectors, serverlessAwsLambdaRollbackFnq);
  }

  @Nonnull
  @Override
  @JsonIgnore
  public List<String> getCommandUnits() {
    return Arrays.asList(
        ServerlessCommandUnitConstants.init.toString(), ServerlessCommandUnitConstants.rollback.toString());
  }
}
