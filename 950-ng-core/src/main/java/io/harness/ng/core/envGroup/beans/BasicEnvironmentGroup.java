package io.harness.ng.core.envGroup.beans;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.ng.core.common.beans.NGTag;
import lombok.Builder;
import lombok.Data;

import java.util.List;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

@OwnedBy(PIPELINE)
@Data
@Builder
public class BasicEnvironmentGroup {
    @EntityName
    String name;
    @EntityIdentifier
    String identifier;

    String orgIdentifier;
     String projectIdentifier;

    String description;
    String color;
    List<NGTag> tags;

    private List<String> envIdentifiers;
}
