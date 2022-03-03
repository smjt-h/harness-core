/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.cloudformation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(HarnessTeam.CDP)
public enum CloudformationTypes {
  @JsonProperty("CreateStack") CREATE_STACK("CreateStack"),
  @JsonProperty("DestroyStack") DESTROY_STACK("DestroyStack");

  private final String displayName;

  CloudformationTypes(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }

  @JsonCreator
  public static CloudformationTypes fromString(String value) {
    for (CloudformationTypes type : CloudformationTypes.values()) {
      if (type.displayName.equals(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Invalid CloudformationType: " + value);
  }
}
