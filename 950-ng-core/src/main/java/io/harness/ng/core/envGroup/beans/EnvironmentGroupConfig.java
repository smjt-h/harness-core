package io.harness.ng.core.envGroup.beans;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.ng.core.common.beans.NGTag;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PIPELINE)
@Data
@Builder
@TypeAlias("environmentGroupConfig")
public class EnvironmentGroupConfig {
  @EntityName String name;
  @EntityIdentifier String identifier;

  String orgIdentifier;
  String projectIdentifier;

  String description;
  String color;
  List<NGTag> tags;

  private List<String> envIdentifiers;
}
