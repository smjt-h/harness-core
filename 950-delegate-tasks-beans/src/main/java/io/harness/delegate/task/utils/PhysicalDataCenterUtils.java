package io.harness.delegate.task.utils;

import lombok.experimental.UtilityClass;

import java.util.Optional;

import static io.harness.delegate.task.utils.PhysicalDataCenterConstants.DEFAULT_SSH_PORT;
import static org.apache.commons.lang3.StringUtils.isBlank;

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

  public Optional<Integer> extractPortFromHost(String host) {
    String[] parts = host.split(":");
    if(parts.length < 2) {
      return Optional.empty();
    }
    String portS = parts[parts.length - 1];
    try {
      return Optional.ofNullable(Integer.parseInt(portS));
    } catch (NumberFormatException nfe) {
      return Optional.empty();
    }
  }

  public Optional<String> extractHostnameFromHost(String host) {
    if(isBlank(host)) {
      return Optional.empty();
    }
    return Optional.ofNullable(host.substring(0, host.lastIndexOf(":")));
  }
}
