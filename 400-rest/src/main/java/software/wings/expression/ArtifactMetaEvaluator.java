///*
// * Copyright 2020 Harness Inc. All rights reserved.
// * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// * that can be found in the licenses directory at the root of this repository, also available at
// * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
// */
//
package software.wings.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.annotations.dev.OwnedBy;
import io.harness.docker.ArtifactMetaInfo;
import io.harness.logging.AutoLogContext;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.delegatetasks.buildsource.ArtifactStreamLogContext;
import software.wings.service.intfc.BuildSourceService;

import java.util.Collections;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Data
@Builder
@Slf4j
public class ArtifactMetaEvaluator {
  private transient String buildNo;
  private transient ArtifactStream artifactStream;
  private transient BuildSourceService buildSourceService;
  private transient ArtifactMetaInfo artifactMetaInfo;

  public synchronized String output() {
    try (AutoLogContext ignore2 = new ArtifactStreamLogContext(
             artifactStream.getUuid(), artifactStream.getArtifactStreamType(), OVERRIDE_ERROR)) {
      if (artifactMetaInfo == null) {
        artifactMetaInfo = buildSourceService.getMetaInfo(artifactStream, Collections.singletonList(buildNo));
      }

      if (artifactMetaInfo == null) {
        log.error("no info available");
      }

      return artifactMetaInfo.getSHA();
    }
  }

  public synchronized String getSHA() {
    return output();
  }
}
