package software.wings.security;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExecutableElementInfo {
  private String entityType;
  private String entityId;
}