/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.yaml;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@RecasterAlias("io.harness.pms.yaml.ParameterFieldValueWrapper")
@OwnedBy(HarnessTeam.PIPELINE)
public class ParameterFieldValueWrapper<T> {
  public static final String VALUE_FIELD = "value";

  private final T value;
}
