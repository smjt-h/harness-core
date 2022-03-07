package io.harness.connector.featureflagfilter;

import io.harness.beans.FeatureName;

import com.google.common.collect.Sets;

public class TestFeatureFlagFilterContext extends FeatureFlagFilterContext {
  public TestFeatureFlagFilterContext() {
    put(FeatureName.SSH_NG, Sets.newHashSet(WeekdaysEnum.SATURDAY, WeekdaysEnum.SUNDAY));
  }
}
