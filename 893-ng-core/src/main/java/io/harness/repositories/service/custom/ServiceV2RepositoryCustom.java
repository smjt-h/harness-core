/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.service.custom;

import io.harness.ng.core.service.entity.ServiceEntity;

import com.mongodb.client.result.UpdateResult;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface ServiceV2RepositoryCustom {
  ServiceEntity save(ServiceEntity serviceEntity);
  Optional<ServiceEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier, boolean notDeleted);
  ServiceEntity update(ServiceEntity serviceEntity, Criteria criteria);
  ServiceEntity upsert(Criteria criteria, ServiceEntity serviceEntity);
  Page<ServiceEntity> findAll(Criteria criteria, Pageable pageable);
  List<ServiceEntity> findAllRunTimePermission(Criteria criteria);
  ServiceEntity find(String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceIdentifier,
      boolean deleted);
  Long findActiveServiceCountAtGivenTimestamp(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, long timestampInMs);
  UpdateResult delete(Criteria criteria);
}
