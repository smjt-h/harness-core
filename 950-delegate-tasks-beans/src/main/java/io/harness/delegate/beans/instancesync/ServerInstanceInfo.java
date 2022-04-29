/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.instancesync;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.instancesync.info.K8sServerInstanceInfo;
import io.harness.delegate.beans.instancesync.info.NativeHelmServerInstanceInfo;
import io.harness.delegate.beans.instancesync.info.ServerlessAwsLambdaServerInstanceInfo;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Extend this class and create deployment specific server instance structs that will
 * contain details with respect to the logical instance entity on the server
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = K8sServerInstanceInfo.class, name = "K8sServerInstanceInfo")
  , @JsonSubTypes.Type(value = NativeHelmServerInstanceInfo.class, name = "NativeHelmServerInstanceInfo"),
      @JsonSubTypes.Type(
          value = ServerlessAwsLambdaServerInstanceInfo.class, name = "ServerlessAwsLambdaServerInstanceInfo")
})
@OwnedBy(HarnessTeam.DX)
public abstract class ServerInstanceInfo {}
