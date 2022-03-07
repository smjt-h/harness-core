package io.harness.connector.featureflagfilter;

import io.harness.beans.FeatureName;
import io.harness.delegate.beans.connector.ConnectorType;

import com.google.common.collect.Sets;

public class CdFeatureFlagFilterContext extends FeatureFlagFilterContext {
  public CdFeatureFlagFilterContext() {
    put(FeatureName.SSH_NG, Sets.newHashSet(ConnectorType.PDC));
  }
}
