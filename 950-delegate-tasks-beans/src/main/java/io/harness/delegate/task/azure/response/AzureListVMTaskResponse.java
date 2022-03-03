/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.azure.response;

import io.harness.delegate.task.azure.AzureTaskResponse;

import software.wings.beans.infrastructure.Host;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AzureListVMTaskResponse implements AzureTaskResponse {
  private List<Host> hosts;

  @Builder
  public AzureListVMTaskResponse(List<Host> hosts) {
    this.hosts = hosts;
  }
}
