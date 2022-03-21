/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.service.mappers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ng.core.mapper.TagMapper.convertToList;
import static io.harness.ng.core.mapper.TagMapper.convertToMap;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.service.dto.ServiceRequestDTO;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.yaml.NGServiceConfig;
import io.harness.ng.core.service.yaml.NGServiceV2InfoConfig;
import io.harness.template.beans.yaml.NGTemplateConfig;
import io.harness.template.entity.TemplateEntity;
import io.harness.utils.YamlPipelineUtils;

import java.io.IOException;
import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class NGServiceEntityMapper {
  public String toYaml(NGServiceConfig ngServiceConfig) {
    try {
      return YamlPipelineUtils.getYamlString(ngServiceConfig);
    } catch (IOException e) {
      throw new InvalidRequestException("Cannot create service entity due to " + e.getMessage());
    }
  }

  public NGServiceConfig toNGServiceConfig(ServiceEntity serviceEntity) {
    return NGServiceConfig.builder()
        .ngServiceV2InfoConfig(NGServiceV2InfoConfig.builder()
                                   .name(serviceEntity.getName())
                                   .identifier(serviceEntity.getIdentifier())
                                   .orgIdentifier(serviceEntity.getOrgIdentifier())
                                   .projectIdentifier(serviceEntity.getProjectIdentifier())
                                   .description(serviceEntity.getDescription())
                                   .tags(convertToMap(serviceEntity.getTags()))
                                   .build())
        .build();
  }

  public NGServiceConfig toDTO(String yaml) {
    try {
      return YamlPipelineUtils.read(yaml, NGServiceConfig.class);
    } catch (IOException ex) {
      throw new InvalidRequestException("Cannot create service yaml: " + ex.getMessage(), ex);
    }
  }

  public NGServiceConfig toDTO(ServiceEntity serviceEntity) {
    try {
      return YamlPipelineUtils.read(serviceEntity.getYaml(), NGServiceConfig.class);
    } catch (IOException ex) {
      throw new InvalidRequestException("Cannot create service yaml: " + ex.getMessage(), ex);
    }
  }

  public ServiceEntity toServiceEntity(String accountId, String serviceYaml) {
    try {
      NGServiceConfig ngServiceConfig = YamlPipelineUtils.read(serviceYaml, NGServiceConfig.class);
      ServiceEntity serviceEntity =
          ServiceEntity.builder()
              .identifier(ngServiceConfig.getNgServiceV2InfoConfig().getIdentifier())
              .accountId(accountId)
              .orgIdentifier(ngServiceConfig.getNgServiceV2InfoConfig().getOrgIdentifier())
              .projectIdentifier(ngServiceConfig.getNgServiceV2InfoConfig().getProjectIdentifier())
              .name(ngServiceConfig.getNgServiceV2InfoConfig().getName())
              .description(ngServiceConfig.getNgServiceV2InfoConfig().getDescription())
              .tags(convertToList(ngServiceConfig.getNgServiceV2InfoConfig().getTags()))
              .yaml(serviceYaml)
              .build();
      return serviceEntity;
    } catch (IOException e) {
      throw new InvalidRequestException("Cannot create template entity due to " + e.getMessage());
    }
  }

  public ServiceEntity toServiceEntity(String accountId, NGServiceConfig ngServiceConfig) {
    try {
      ServiceEntity serviceEntity =
          ServiceEntity.builder()
              .identifier(ngServiceConfig.getNgServiceV2InfoConfig().getIdentifier())
              .accountId(accountId)
              .orgIdentifier(ngServiceConfig.getNgServiceV2InfoConfig().getOrgIdentifier())
              .projectIdentifier(ngServiceConfig.getNgServiceV2InfoConfig().getProjectIdentifier())
              .name(ngServiceConfig.getNgServiceV2InfoConfig().getName())
              .description(ngServiceConfig.getNgServiceV2InfoConfig().getDescription())
              .tags(convertToList(ngServiceConfig.getNgServiceV2InfoConfig().getTags()))
              .yaml(NGServiceEntityMapper.toYaml(ngServiceConfig))
              .build();
      return serviceEntity;
    } catch (Exception e) {
      throw new InvalidRequestException("Cannot create template entity due to " + e.getMessage());
    }
  }
}
