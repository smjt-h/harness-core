package io.harness.connector.featureflagfilter;

import io.harness.beans.FeatureName;

import java.util.List;

public interface FeatureFlagFilterService {
  <T extends Enum<?>> List<T> filterEnum(
      String accountId, FeatureName featureName, Class<T> enumClass, FeatureFlagFilterContext context);
}
