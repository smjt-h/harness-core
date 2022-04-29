/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.featureFlag;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.ff.filters.EnumFeatureFlagFilter;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.CDP)
@Singleton
public class CdEnumFilter extends EnumFeatureFlagFilter {
  @Inject private CDFeatureFlagHelper cdFeatureFlagHelper;

  public CdEnumFilter() {
    put(FeatureName.SSH_NG, Sets.newHashSet(ServiceDefinitionType.SSH, ServiceDefinitionType.WINRM));
  }

  @Override
  public boolean isFeatureFlagEnabled(FeatureName featureName, String accountId) {
    return cdFeatureFlagHelper.isEnabled(accountId, featureName);
  }
}
