/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.serverless;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Value
@Builder
@TypeAlias("serverlessAwsLambdaRollbackOutcome")
@JsonTypeName("ServerlessAwsLambdaRollbackOutcome")
@RecasterAlias("io.harness.cdng.serverless.ServerlessAwsLambdaRollbackOutcome")
public class ServerlessAwsLambdaRollbackOutcome implements Outcome, ExecutionSweepingOutput {
  String service;
  String region;
  String stage;
  String rollbackTimeStamp;
}
