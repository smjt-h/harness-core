/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.aws.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(CDP)
public interface AwsHelperResourceService {
  /**
   * Get the list of all the Capabilities in cloudformation
   *
   * @return the list of capabilities
   */
  List<String> getCapabilities();

  /**
   * Get the list of all the cloudformation states
   *
   * @return the list of all the cloudformation states
   */
  Set<String> getCFStates();

  /**
   * Get the list of available regions from the aws.yaml resource file
   *
   * @return the list of available regions
   */
  Map<String, String> getRegions();
}
