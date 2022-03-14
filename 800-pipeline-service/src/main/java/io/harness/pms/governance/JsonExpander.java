/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.governance;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;
import io.harness.pms.contracts.governance.ExpansionRequestBatch;
import io.harness.pms.contracts.governance.ExpansionResponseBatch;
import io.harness.pms.contracts.governance.JsonExpansionServiceGrpc.JsonExpansionServiceBlockingStub;
import io.harness.pms.utils.CompletableFutures;
import io.harness.pms.utils.PmsGrpcClientUtils;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@OwnedBy(PIPELINE)
public class JsonExpander {
  @Inject Map<ModuleType, JsonExpansionServiceBlockingStub> jsonExpansionServiceBlockingStubMap;
  Executor executor = Executors.newFixedThreadPool(5);

  public Set<ExpansionResponseBatch> fetchExpansionResponses(
      Map<ModuleType, ExpansionRequestBatch> expansionRequestBatches) {
    CompletableFutures<ExpansionResponseBatch> completableFutures = new CompletableFutures<>(executor);

    for (ModuleType module : expansionRequestBatches.keySet()) {
      completableFutures.supplyAsync(() -> {
        JsonExpansionServiceBlockingStub blockingStub = jsonExpansionServiceBlockingStubMap.get(module);
        return PmsGrpcClientUtils.retryAndProcessException(blockingStub::expand, expansionRequestBatches.get(module));
      });
    }

    try {
      return new HashSet<>(completableFutures.allOf().get(5, TimeUnit.MINUTES));
    } catch (Exception ex) {
      throw new UnexpectedException("Error fetching JSON expansion responses from services", ex);
    }
  }
}
