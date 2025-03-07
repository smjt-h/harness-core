/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.servicelevelobjective.beans;

import java.util.List;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SLODashboardApiFilter {
  @QueryParam("userJourneyIdentifiers") List<String> userJourneyIdentifiers;
  @QueryParam("monitoredServiceIdentifier") String monitoredServiceIdentifier;
  @QueryParam("sliTypes") List<ServiceLevelIndicatorType> sliTypes;
  @QueryParam("targetTypes") List<SLOTargetType> targetTypes;
  @QueryParam("errorBudgetRisks") List<ErrorBudgetRisk> errorBudgetRisks;
}
