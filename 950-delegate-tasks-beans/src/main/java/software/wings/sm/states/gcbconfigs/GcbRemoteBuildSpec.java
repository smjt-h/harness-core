/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states.gcbconfigs;

import lombok.Data;

@Data
public class GcbRemoteBuildSpec {
  public enum RemoteFileSource { BRANCH, COMMIT }

  private String gitConfigId;
  private String sourceId;
  private String filePath;
  private String repoName;
  private RemoteFileSource fileSource;
}
