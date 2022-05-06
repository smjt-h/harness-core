/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.notification.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = MonitoredServiceChangeImpactConditionSpec.class, name = "ChangeImpact")
  , @JsonSubTypes.Type(value = MonitoredServiceHealthScoreConditionSpec.class, name = "HealthScore"),
      @JsonSubTypes.Type(value = MonitoredServiceChangeObservedConditionSpec.class, name = "ChangeObserved")
})
public abstract class MonitoredServiceNotificationRuleConditionSpec {
  @JsonIgnore public abstract MonitoredServiceNotificationRuleConditionType getConditionType();
}
