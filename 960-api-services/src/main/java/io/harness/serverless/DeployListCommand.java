/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serverless;

import org.apache.commons.lang3.StringUtils;

public class DeployListCommand extends AbstractExecutable {
  private ServerlessClient client;
  private String stage;
  private String region;
  public DeployListCommand(ServerlessClient client) {
    this.client = client;
  }
  public DeployListCommand stage(String stage) {
    this.stage = stage;
    return this;
  }
  public DeployListCommand region(String region) {
    this.region = region;
    return this;
  }
  @Override
  public String command() {
    StringBuilder command = new StringBuilder();
    command.append(client.command()).append("deploy list ");
    if (StringUtils.isNotBlank(this.stage)) {
      command.append(ServerlessClient.option(Option.stage, this.stage));
    }
    if (StringUtils.isNotBlank(this.region)) {
      command.append(ServerlessClient.option(Option.region, this.region));
    }
    return command.toString().trim();
  }
}
