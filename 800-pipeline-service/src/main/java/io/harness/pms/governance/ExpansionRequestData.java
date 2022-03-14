package io.harness.pms.governance;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.governance.ExpansionRequestBatch;

import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(PIPELINE)
public class ExpansionRequestData {
  Map<ModuleType, ExpansionRequestBatch> moduleToRequestBatch;
  Map<String, Set<String>> uuidToFqnSet;
}
