/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.yaml.clone;

import io.harness.rest.RestResponse;

public interface YamlCloneService {
  RestResponse cloneEntityUsingYaml(
      String accountId, String appId, boolean includeFiles, String entityType, String entiytId, String newName);
}
