/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.template.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.ChangeType;
import io.harness.template.entity.TemplateEntity;
import io.harness.template.helpers.TemplateInputsRefreshHelper;
import io.harness.template.mappers.NGTemplateDtoMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
@OwnedBy(CDC)
public class TemplateRefreshServiceImpl implements TemplateRefreshService {
  private TemplateInputsRefreshHelper templateInputsRefreshHelper;
  private NGTemplateService templateService;

  @Override
  public boolean refreshAndUpdateTemplate(
      String accountId, String orgId, String projectId, String templateIdentifier, String versionLabel) {
    Optional<TemplateEntity> optionalTemplateEntity =
        templateService.get(accountId, orgId, projectId, templateIdentifier, versionLabel, false);

    if (!optionalTemplateEntity.isPresent()) {
      throw new InvalidRequestException(
          String.format("Template with the Identifier %s and versionLabel %s does not exist or has been deleted",
              templateIdentifier, versionLabel));
    }

    String refreshedYaml =
        refreshLinkedTemplateInputs(accountId, orgId, projectId, optionalTemplateEntity.get().getYaml());
    TemplateEntity templateEntity = NGTemplateDtoMapper.toTemplateEntity(
        accountId, orgId, projectId, templateIdentifier, versionLabel, refreshedYaml);
    templateService.updateTemplateEntity(templateEntity, ChangeType.MODIFY, false, "Refreshed template inputs");
    return true;
  }

  @Override
  public String refreshLinkedTemplateInputs(String accountId, String orgId, String projectId, String yaml) {
    return templateInputsRefreshHelper.refreshTemplates(accountId, orgId, projectId, yaml);
  }
}
