package io.harness.delegate.beans.executioncapability;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import java.util.List;
import java.util.Set;

public class DelegateSelectorCapabilityHelper {
  public static void populateDelegateSelectorCapability(
      List<ExecutionCapability> capabilityList, Set<String> delegateSelectors, String origin) {
    if (isNotEmpty(delegateSelectors)) {
      capabilityList.add(SelectorCapability.builder().selectors(delegateSelectors).selectorOrigin(origin).build());
    }
  }
}
