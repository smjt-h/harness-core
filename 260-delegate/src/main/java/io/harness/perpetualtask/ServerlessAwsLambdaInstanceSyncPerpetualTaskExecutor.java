/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.grpc.utils.AnyUtils;
import io.harness.perpetualtask.instancesync.ServerlessAwsLambdaInstanceSyncPerpetualTaskParams;

import java.time.Instant;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(HarnessTeam.CDP)
public class ServerlessAwsLambdaInstanceSyncPerpetualTaskExecutor implements PerpetualTaskExecutor {
  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    log.info("Running the ServerlessAwsLambda InstanceSync perpetual task executor for task id: {}", taskId);
    ServerlessAwsLambdaInstanceSyncPerpetualTaskParams taskParams =
        AnyUtils.unpack(params.getCustomizedParams(), ServerlessAwsLambdaInstanceSyncPerpetualTaskParams.class);
    return executeServerlessAwsLambdaInstanceSyncTask(taskId, taskParams);
  }

  private PerpetualTaskResponse executeServerlessAwsLambdaInstanceSyncTask(
      PerpetualTaskId taskId, ServerlessAwsLambdaInstanceSyncPerpetualTaskParams taskParams) {
    List<>
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return false;
    // todo: check the usage
  }
}
