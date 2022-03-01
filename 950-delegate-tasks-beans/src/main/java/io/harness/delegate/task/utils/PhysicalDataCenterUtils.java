package io.harness.delegate.task.utils;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PhysicalDataCenterUtils {
  private static final String DEFAULT_SSH_PORT = "22";

  public List<String> getHostsAsList(final String hostNames) {
    if (isBlank(hostNames)) {
      return Collections.emptyList();
    }

    String trimHostNames = hostNames.trim();
    return Arrays.asList(trimHostNames.split(","));
  }

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
