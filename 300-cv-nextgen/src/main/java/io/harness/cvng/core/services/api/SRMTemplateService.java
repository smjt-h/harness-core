/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.template.SRMTemplateDTO;
import io.harness.cvng.core.beans.template.TemplateType;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.List;

public interface SRMTemplateService {
  PageResponse<SRMTemplateDTO> list(
      ProjectParams projectParams, List<TemplateType> templateTypes, String searchString, PageRequest pageRequest);
  SRMTemplateDTO get(String accountId, String fullyQualifiedIdentifier);
  SRMTemplateDTO create(ProjectParams projectParams, SRMTemplateDTO srmTemplateDTO);
  SRMTemplateDTO update(ProjectParams projectParams, String fullyQualifiedIdentifier, SRMTemplateDTO srmTemplateDTO);
  boolean delete(String accountId, String fullyQualifiedIdentifier);
}
