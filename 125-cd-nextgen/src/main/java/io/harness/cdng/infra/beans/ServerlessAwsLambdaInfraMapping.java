/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.infra.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@Builder
@TypeAlias("serverlessAwsLambdaInfraMapping")
@JsonTypeName("serverlessAwsLambdaInfraMapping")
@RecasterAlias("io.harness.cdng.infra.beans.ServerlessAwsLambdaInfraMapping")
public class ServerlessAwsLambdaInfraMapping implements InfraMapping {
  @Id private String uuid;
  private String accountId;
  private String awsConnector;
  private String region;
  private String stage;
}
