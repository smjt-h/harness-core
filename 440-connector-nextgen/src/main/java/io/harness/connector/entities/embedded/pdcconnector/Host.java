package io.harness.connector.entities.embedded.pdcconnector;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.connector.entities.embedded.pdcconnector.Host")
public class Host {
  String hostName;
  List<HostAttribute> hostAttributes;
}
