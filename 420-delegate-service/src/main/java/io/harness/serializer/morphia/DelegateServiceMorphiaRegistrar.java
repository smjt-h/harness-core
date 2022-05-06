/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer.morphia;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateGroup;
import io.harness.delegate.beans.DelegateMtlsEndpoint;
import io.harness.delegate.beans.DelegateRing;
import io.harness.delegate.beans.VersionOverride;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.selection.log.DelegateSelectionLog;
import io.harness.selection.log.DelegateSelectionLogTaskMetadata;

import software.wings.beans.DelegateConnection;
import software.wings.beans.DelegateInsightsSummary;
import software.wings.beans.DelegatePerpetualTaskUsageInsights;
import software.wings.beans.DelegateTaskUsageInsights;

import java.util.Set;

@OwnedBy(DEL)
public class DelegateServiceMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Delegate.class);
    set.add(DelegateConnection.class);
    set.add(DelegateSelectionLog.class);
    set.add(DelegateSelectionLogTaskMetadata.class);
    set.add(DelegateGroup.class);
    set.add(DelegateInsightsSummary.class);
    set.add(DelegateTaskUsageInsights.class);
    set.add(DelegatePerpetualTaskUsageInsights.class);
    set.add(PerpetualTaskRecord.class);
    set.add(DelegateRing.class);
    set.add(VersionOverride.class);
    set.add(DelegateMtlsEndpoint.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {}
}
