/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class AwsEcsRequest extends AwsRequest implements TaskParameters {
  public enum AwsEcsRequestType { LIST_CLUSTERS, LIST_CLUSTER_SERVICES }

  @NotNull AwsEcsRequestType requestType;
  @NotNull String region;

  public AwsEcsRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, AwsEcsRequestType requestType, String region) {
    super(awsConfig, encryptionDetails);
    this.requestType = requestType;
    this.region = region;
  }
}
