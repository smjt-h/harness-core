/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service.ngManifestFactory;

import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.ngmigration.beans.ManifestProvidedEntitySpec;
import io.harness.ngmigration.beans.NgEntityDetail;

import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.ngmigration.CgEntityId;

import java.util.Map;

public interface NgManifestService {
  ManifestConfigWrapper getManifestConfigWrapper(ApplicationManifest applicationManifest,
      Map<CgEntityId, NgEntityDetail> migratedEntities, ManifestProvidedEntitySpec entitySpec);
}
