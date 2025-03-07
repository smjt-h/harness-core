/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.beans.executioncapability.SocketConnectivityExecutionCapability;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SSHTaskParams implements TaskParameters, ExecutionCapabilityDemander {
  SSHKeySpecDTO sshKeySpec;
  String host;
  List<EncryptedDataDetail> encryptionDetails;
  Set<String> delegateSelectors;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    if (isEmpty(delegateSelectors)) {
      return Collections.singletonList(getSocketConnectivityCapability());
    } else {
      return Arrays.asList(getSocketConnectivityCapability(),
          SelectorCapability.builder().selectors(delegateSelectors).selectorOrigin("Task Selectors").build());
    }
  }

  private ExecutionCapability getSocketConnectivityCapability() {
    return SocketConnectivityExecutionCapability.builder()
        .hostName(host)
        .url("")
        .scheme("")
        .port(String.valueOf(sshKeySpec.getPort()))
        .build();
  }
}
