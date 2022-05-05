/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.intfc.verification.CVActivityLogger;

@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public interface DelegateCVActivityLogService {
  Logger getLogger(String accountId, String cvConfigId, long dataCollectionMinute, String stateExecutionId,
      String prefix, long... prefixTimestampParams);
  default Logger getLogger(String accountId, String cvConfigId, long dataCollectionMinute, String stateExecutionId) {
    return getLogger(accountId, cvConfigId, dataCollectionMinute, stateExecutionId, "");
  }

  interface Logger extends CVActivityLogger {}
}
