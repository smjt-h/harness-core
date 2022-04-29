/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.remote.mapper.recommendation;

import io.harness.ccm.remote.beans.recommendation.CCMRecommendationFilterProperties;
import io.harness.ccm.remote.beans.recommendation.CCMRecommendationFilterPropertiesDTO;
import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.filter.entity.FilterProperties;
import io.harness.filter.mapper.FilterPropertiesMapper;
import io.harness.ng.core.mapper.TagMapper;

import com.google.inject.Singleton;
import org.modelmapper.ModelMapper;

@Singleton
public class CCMRecommendationFilterPropertiesMapper
    implements FilterPropertiesMapper<CCMRecommendationFilterPropertiesDTO, CCMRecommendationFilterProperties> {
  @Override
  public FilterPropertiesDTO writeDTO(FilterProperties filterProperties) {
    ModelMapper modelMapper = new ModelMapper();
    FilterPropertiesDTO filterPropertiesDTO =
        modelMapper.map(filterProperties, CCMRecommendationFilterPropertiesDTO.class);
    filterPropertiesDTO.setTags(TagMapper.convertToMap(filterProperties.getTags()));
    return filterPropertiesDTO;
  }

  @Override
  public FilterProperties toEntity(FilterPropertiesDTO filterPropertiesDTO) {
    ModelMapper modelMapper = new ModelMapper();
    CCMRecommendationFilterProperties filterProperties =
        modelMapper.map(filterPropertiesDTO, CCMRecommendationFilterProperties.class);
    filterProperties.setType(filterPropertiesDTO.getFilterType());
    filterProperties.setTags(TagMapper.convertToList(filterPropertiesDTO.getTags()));
    return filterProperties;
  }
}
