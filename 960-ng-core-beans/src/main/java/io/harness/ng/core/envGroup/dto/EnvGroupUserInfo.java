package io.harness.ng.core.envGroup.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnvGroupUserInfo {
  @Schema(description = "Id of the User who last updated the Entity") String id;
  @Schema(description = "Email of the User who last updated the Entity") String email;
  @Schema(description = "Name of the User who last updated the Entity") String name;
}
