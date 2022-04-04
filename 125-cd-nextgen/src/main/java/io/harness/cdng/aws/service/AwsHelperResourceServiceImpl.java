/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.aws.service;

import io.harness.exception.InvalidArgumentsException;

import com.amazonaws.services.cloudformation.model.Capability;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AwsHelperResourceServiceImpl implements AwsHelperResourceService {
  private Map<String, String> regionMap;
  private static final String regionNode = "awsRegionIdToName";
  private static final String yamlResourceFilePath = "aws/aws.yaml";
  @Override
  public List<String> getCapabilities() {
    return Stream.of(Capability.values()).map(Capability::toString).collect(Collectors.toList());
  }

  @Override
  public Set<String> getCFStates() {
    return EnumSet.allOf(StackStatus.class).stream().map(Enum::name).collect(Collectors.toSet());
  }

  @Override
  public Map<String, String> getRegions() {
    if (regionMap == null) {
      try {
        ClassLoader classLoader = this.getClass().getClassLoader();
        String parsedYamlFile = Resources.toString(
            Objects.requireNonNull(classLoader.getResource(yamlResourceFilePath)), StandardCharsets.UTF_8);
        getMapRegionFromYaml(parsedYamlFile);
      } catch (IOException e) {
        throw new InvalidArgumentsException("Failed to read the region yaml file:" + e);
      }
    }
    return regionMap;
  }

  protected void getMapRegionFromYaml(String parsedYamlfile) {
    try {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      JsonNode node = mapper.readTree(parsedYamlfile);
      JsonNode regions = node.path(regionNode);
      mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
      regionMap = mapper.readValue(String.valueOf(regions), new TypeReference<Map<String, String>>() {});
    } catch (IOException e) {
      throw new InvalidArgumentsException("Failed to Deserialize the region yaml file:" + e);
    }
  }
}
