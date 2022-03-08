package io.harness.pms.pipeline.handlers;

import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;

import javax.inject.Named;

@Named
public class QueryForCi {
  private String CI_TableName = "pipeline_execution_summary_ci";
  public void insert(String id, PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity) {}
  public void update(String id, PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity) {}
  public void delete(String id) {}
}
