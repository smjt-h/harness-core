/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.repositories.orchestrationEventLog;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationEventLog;
import io.harness.beans.OrchestrationEventLog.OrchestrationEventLogKeys;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.PIPELINE)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class OrchestrationEventLogRepositoryCustomImpl implements OrchestrationEventLogRepositoryCustom {
  private static final int LOG_LIMIT = 1000;
  private final MongoTemplate mongoTemplate;
  private final String logLimit = System.getProperty("UNPROCESSED_EVENT_LOG_LIMIT");

  @Override
  public List<OrchestrationEventLog> findUnprocessedEvents(String planExecutionId, long lastUpdatedAt) {
    Criteria criteria = Criteria.where(OrchestrationEventLogKeys.planExecutionId).is(planExecutionId);
    criteria.andOperator(Criteria.where(OrchestrationEventLogKeys.createdAt).gte(lastUpdatedAt));
    Query query = new Query(criteria).with(Sort.by(Sort.Order.asc("createdAt"))).limit(LOG_LIMIT);
    return mongoTemplate.find(query, OrchestrationEventLog.class);
  }

  @Override
  public void deleteLogsForGivenPlanExecutionId(String planExecutionId) {
    Criteria criteria = Criteria.where(OrchestrationEventLogKeys.planExecutionId).is(planExecutionId);
    mongoTemplate.remove(new Query(criteria), OrchestrationEventLog.class);
  }
}
