package io.harness.delegate.task.utils;

import static io.harness.delegate.task.utils.PhysicalDataCenterConstants.DEFAULT_SSH_PORT;

import static org.apache.commons.lang3.StringUtils.isBlank;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PhysicalDataCenterUtils {
  public String getPortOrSSHDefault(final String host) {
    if (isBlank(host)) {
      return DEFAULT_SSH_PORT;
    }

    String[] hostParts = host.split(":");
    if (hostParts.length == 2) {
      return hostParts[1];
    } else {
      return DEFAULT_SSH_PORT;
    }
  }
}
