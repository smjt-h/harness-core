/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.filestore;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.filestore.NGFileType;
import io.harness.ng.core.dto.filestore.filter.FilesFilterPropertiesDTO;
import io.harness.ng.core.entities.NGFile.NGFiles;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.ng.core.utils.URLDecoderUtility;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.CDP)
@UtilityClass
public class FileStoreRepositoryCriteriaCreator {
  public static Criteria createCriteriaByScopeAndParentIdentifier(Scope scope, final String parentIdentifier) {
    Criteria scopeCriteria = createScopeCriteria(scope);
    return scopeCriteria.and(NGFiles.parentIdentifier).is(parentIdentifier);
  }

  public static Criteria createScopeCriteria(Scope scope) {
    Criteria criteria = new Criteria();
    criteria.and(NGFiles.accountIdentifier).is(scope.getAccountIdentifier());
    criteria.and(NGFiles.orgIdentifier).is(scope.getOrgIdentifier());
    criteria.and(NGFiles.projectIdentifier).is(scope.getProjectIdentifier());
    return criteria;
  }

  public static Criteria createFilesFilterCriteria(
      Scope scope, FilesFilterPropertiesDTO filterProperties, String searchTerm) {
    Criteria criteria = createScopeCriteria(scope);
    criteria.and(NGFiles.type).is(NGFileType.FILE);

    searchTerm = URLDecoderUtility.getDecodedString(searchTerm);

    if (isNotEmpty(searchTerm)) {
      criteria.and(NGFiles.name).regex(searchTerm, NGResourceFilterConstants.CASE_INSENSITIVE_MONGO_OPTIONS);
    }

    if (filterProperties != null && filterProperties.getFileUsage() != null) {
      criteria.and(NGFiles.fileUsage).is(filterProperties.getFileUsage().name());
    }

    if (filterProperties != null && filterProperties.getCreatedBy() != null) {
      criteria.and(NGFiles.createdBy).is(filterProperties.getCreatedBy());
    }

    if (filterProperties != null && !isEmpty(filterProperties.getTags())) {
      criteria.and(NGFiles.tags).in(TagMapper.convertToList(filterProperties.getTags()));
    }

    return criteria;
  }

  public static Sort createSortByLastModifiedAtDesc() {
    return Sort.by(Sort.Direction.DESC, NGFiles.lastModifiedAt);
  }

  public static Sort createSortByName(Sort.Direction direction) {
    return Sort.by(direction, NGFiles.name);
  }
}
