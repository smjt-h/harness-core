/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.SocketConnectivityExecutionCapability;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.ng.core.dto.secrets.WinRmCredentialsSpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDP)
@Value
@Builder
public class WinRmTaskParams implements TaskParameters, ExecutionCapabilityDemander {
  WinRmCredentialsSpecDTO spec;
  String host;
  List<EncryptedDataDetail> encryptionDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return Collections.singletonList(SocketConnectivityExecutionCapability.builder()
                                         .hostName(host)
                                         .url("")
                                         .scheme("")
                                         .port(String.valueOf(spec.getPort()))
                                         .build());
  }
}
