/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.artifacts.comparator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifact.ComparatorUtils;

import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.Comparator;

@OwnedBy(HarnessTeam.CDC)
public class BuildDetailsComparatorDescending implements Comparator<BuildDetails> {
  @Override
  public int compare(BuildDetails o1, BuildDetails o2) {
    return ComparatorUtils.compareDescending(o1.getNumber(), o2.getNumber());
  }
}
