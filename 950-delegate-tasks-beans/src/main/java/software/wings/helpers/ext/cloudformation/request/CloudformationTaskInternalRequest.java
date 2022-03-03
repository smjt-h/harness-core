/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.helpers.ext.cloudformation.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.security.encryption.EncryptedDataDetail;

import com.amazonaws.services.cloudformation.model.StackStatus;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
@OwnedBy(CDP)
public class CloudformationTaskInternalRequest {
  private String accountId;
  private String appId;
  private String activityId;
  private String commandName;
  private AwsInternalConfig awsConfig;
  private int timeoutInMs;
  private String region;
  private String cloudFormationRoleArn;
  private boolean skipWaitForResources;
  private String createType;
  private String data;
  private String stackNameSuffix;
  private String customStackName;
  private Map<String, String> variables;
  private Map<String, EncryptedDataDetail> encryptedVariables;
  private List<String> capabilities;
  private String tags;
  private List<StackStatus> stackStatusesToMarkAsSuccess;
}
