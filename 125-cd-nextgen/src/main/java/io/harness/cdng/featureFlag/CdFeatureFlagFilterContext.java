package io.harness.cdng.featureFlag;

import io.harness.beans.FeatureName;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.connector.featureflagfilter.FeatureFlagFilterContext;

import com.google.common.collect.Sets;

public class CdFeatureFlagFilterContext extends FeatureFlagFilterContext {
  public CdFeatureFlagFilterContext() {
    put(FeatureName.SSH_NG, Sets.newHashSet(ServiceDefinitionType.SSH));
  }
}
