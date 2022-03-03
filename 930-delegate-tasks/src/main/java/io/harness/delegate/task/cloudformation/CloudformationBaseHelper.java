/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.cloudformation;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.cloudformation.request.CloudformationTaskInternalRequest;
import software.wings.helpers.ext.cloudformation.response.CloudformationTaskInternalResponse;

import com.amazonaws.services.cloudformation.model.Stack;

@OwnedBy(CDP)
public interface CloudformationBaseHelper {
  CloudformationTaskInternalResponse createStack(
      CloudformationTaskInternalRequest cloudFormationInternalRequest, ExecutionLogCallback executionLogCallback);

  CloudformationTaskInternalResponse updateStack(CloudformationTaskInternalRequest cloudFormationInternalRequest,
      Stack stack, ExecutionLogCallback executionLogCallback);

  CloudformationTaskInternalResponse deleteStack(String stackId, String stackName,
      CloudformationTaskInternalRequest cloudFormationInternalRequest, ExecutionLogCallback executionLogCallback);
}
