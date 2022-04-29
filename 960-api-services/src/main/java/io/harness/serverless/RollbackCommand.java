/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serverless;

import org.apache.commons.lang3.StringUtils;

public class RollbackCommand extends AbstractExecutable {
  private ServerlessClient client;
  private String timeStamp;
  private String config;
  private String stage;
  private String region;
  public RollbackCommand(ServerlessClient client) {
    this.client = client;
  }
  public RollbackCommand timeStamp(String timeStamp) {
    this.timeStamp = timeStamp;
    return this;
  }
  public RollbackCommand config(String config) {
    this.config = config;
    return this;
  }
  public RollbackCommand stage(String stage) {
    this.stage = stage;
    return this;
  }
  public RollbackCommand region(String region) {
    this.region = region;
    return this;
  }
  @Override
  public String command() {
    StringBuilder command = new StringBuilder(2048);
    command.append(client.command()).append("rollback ");
    if (StringUtils.isNotBlank(this.timeStamp)) {
      command.append(ServerlessClient.option(Option.timestamp, this.timeStamp));
    }
    if (StringUtils.isNotBlank(this.region)) {
      command.append(ServerlessClient.option(Option.region, this.region));
    }
    if (StringUtils.isNotBlank(this.stage)) {
      command.append(ServerlessClient.option(Option.stage, this.stage));
    }
    if (StringUtils.isNotBlank(this.config)) {
      command.append(ServerlessClient.option(Option.config, this.config));
    }
    return command.toString().trim();
  }
}
