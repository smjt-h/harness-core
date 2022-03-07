package io.harness.connector.featureflagfilter;

import io.harness.beans.FeatureName;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public class FeatureFlagFilterContext {
  private final EnumMap<FeatureName, Set<Enum<?>>> enumTypeFeatureFlagMap = new EnumMap<>(FeatureName.class);

  protected void put(FeatureName featureName, Set<Enum<?>> enums) {
    Set<Enum<?>> existingEnums = enumTypeFeatureFlagMap.get(featureName);
    if (existingEnums != null) {
      existingEnums.addAll(enums);
    }
    enumTypeFeatureFlagMap.put(featureName, enums);
  }

  public Map<FeatureName, Set<Enum<?>>> getEnumTypeFeatureFlagMap() {
    return enumTypeFeatureFlagMap;
  }
}
