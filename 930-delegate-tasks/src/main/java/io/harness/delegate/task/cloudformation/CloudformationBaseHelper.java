/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.cloudformation;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.logging.LogCallback;
import io.harness.security.encryption.EncryptedDataDetail;

import com.amazonaws.services.cloudformation.model.Stack;
import java.util.List;
import java.util.Optional;

@OwnedBy(CDP)
public interface CloudformationBaseHelper {
  Optional<Stack> getIfStackExists(String customStackName, String suffix, AwsInternalConfig awsConfig, String region);
  AwsInternalConfig getAwsInternalConfig(
      AwsConnectorDTO awsConnectorDTO, String region, List<EncryptedDataDetail> encryptedDataDetails);
  void deleteStack(String region, AwsInternalConfig awsConfig, String stackName, String roleARN, int timeout);
  void waitForStackToBeDeleted(
      String region, AwsInternalConfig awsInternalConfig, String stackId, LogCallback logCallback);
  void performCleanUpTasks(
      CloudformationTaskNGParameters taskNGParameters, String delegateId, String taskId, LogCallback logCallback);
}
