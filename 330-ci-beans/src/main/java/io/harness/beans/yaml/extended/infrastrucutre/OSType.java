/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans.yaml.extended.infrastrucutre;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OSType {
  @JsonProperty("Linux") LINUX("Linux"),
  @JsonProperty("Osx") OSX("Osx"),
  @JsonProperty("Windows") WINDOWS("Windows");
  private final String yamlName;

  @JsonCreator
  public static OSType getOSType(@JsonProperty("osType") String yamlName) {
    for (OSType osType : OSType.values()) {
      if (osType.yamlName.equalsIgnoreCase(yamlName)) {
        return osType;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + yamlName);
  }

  OSType(String yamlName) {
    this.yamlName = yamlName;
  }

  @JsonValue
  public String getYamlName() {
    return yamlName;
  }

  @Override
  public String toString() {
    return yamlName;
  }

  public static OSType fromString(final String s) {
    return OSType.getOSType(s);
  }
}
