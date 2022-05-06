/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.service.steps;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@TypeAlias("serviceStepOutcome")
@JsonTypeName("serviceStepOutcome")
@RecasterAlias("io.harness.cdng.service.steps.ServiceStepOutcome")
public class ServiceStepOutcome implements Outcome, ExecutionSweepingOutput {
  String identifier;
  String name;
  String description;
  String type;
  Map<String, String> tags;

  public String getServiceDefinitionType() {
    return type;
  }

  public static ServiceStepOutcome fromServiceEntity(String type, ServiceEntity serviceEntity) {
    if (serviceEntity == null) {
      return null;
    }

    return ServiceStepOutcome.builder()
        .identifier(serviceEntity.getIdentifier())
        .name(serviceEntity.getName())
        .description(serviceEntity.getName())
        .type(type)
        .tags(serviceEntity.getTags() == null
                ? Collections.emptyMap()
                : serviceEntity.getTags().stream().collect(Collectors.toMap(NGTag::getKey, NGTag::getValue)))
        .build();
  }
}
