package io.harness.entity;

import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.execution.ExecutionStatus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import org.bson.Document;
import org.hibernate.validator.constraints.NotEmpty;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class PipelineExecutionSummaryDashboardEntity {
  @NotEmpty String accountId;
  @NotEmpty String orgIdentifier;
  @Trimmed @NotEmpty String projectIdentifier;

  @NotEmpty String pipelineIdentifier;
  @NotEmpty @FdUniqueIndex String planExecutionId;
  @NotEmpty String name;
  ExecutionStatus status;
  Long startTs;
  Long endTs;
  @Builder.Default Map<String, Document> moduleInfo = new HashMap<>();
  ExecutionTriggerInfo executionTriggerInfo;
}
