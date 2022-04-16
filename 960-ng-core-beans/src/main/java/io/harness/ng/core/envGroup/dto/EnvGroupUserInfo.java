package io.harness.ng.core.envGroup.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EnvGroupUserInfo {
    String id;
    String email;
    String name;
}
