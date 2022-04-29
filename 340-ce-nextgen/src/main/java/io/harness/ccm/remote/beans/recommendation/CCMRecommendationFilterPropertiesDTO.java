/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.remote.beans.recommendation;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.filter.FilterConstants.CCM_RECOMMENDATION_FILTER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterPropertiesDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(CCM_RECOMMENDATION_FILTER)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("CCMRecommendationFilterProperties")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CE)
@Schema(name = "CCMRecommendationFilterProperties",
    description = "Properties of the CCMRecommendation Filter defined in Harness")
public class CCMRecommendationFilterPropertiesDTO extends FilterPropertiesDTO {
  K8sRecommendationFilterPropertiesDTO k8sRecommendationFilterPropertiesDTO;

  Double minSaving;
  Double minCost;

  Long offset;
  Long limit;

  @Override
  public FilterType getFilterType() {
    return FilterType.CCMRECOMMENDATION;
  }
}
