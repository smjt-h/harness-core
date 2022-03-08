package io.harness.pms.pipeline.handlers;

import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.timescaledb.Tables;

import com.google.inject.Inject;
import javax.inject.Named;
import org.jooq.DSLContext;

@Named
public class QueryForCd {
  @Inject private DSLContext dsl;

  public void insert(String id, PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity) {
    dsl.insertInto(Tables.PIPELINE_EXECUTION_SUMMARY_CD, Tables.PIPELINE_EXECUTION_SUMMARY_CD.ID,
           Tables.PIPELINE_EXECUTION_SUMMARY_CD.ACCOUNTID, Tables.PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER,
           Tables.PIPELINE_EXECUTION_SUMMARY_CD.PIPELINEIDENTIFIER,
           Tables.PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER, Tables.PIPELINE_EXECUTION_SUMMARY_CD.PLANEXECUTIONID,
           Tables.PIPELINE_EXECUTION_SUMMARY_CD.NAME, Tables.PIPELINE_EXECUTION_SUMMARY_CD.STATUS,
           Tables.PIPELINE_EXECUTION_SUMMARY_CD.MODULEINFO_TYPE, Tables.PIPELINE_EXECUTION_SUMMARY_CD.STARTTS,
           Tables.PIPELINE_EXECUTION_SUMMARY_CD.ENDTS)
        .values(id, pipelineExecutionSummaryEntity.getAccountId(), pipelineExecutionSummaryEntity.getOrgIdentifier(),
            pipelineExecutionSummaryEntity.getPipelineIdentifier(),
            pipelineExecutionSummaryEntity.getProjectIdentifier(), pipelineExecutionSummaryEntity.getPlanExecutionId(),
            pipelineExecutionSummaryEntity.getName(), pipelineExecutionSummaryEntity.getStatus().toString(), "CD",
            pipelineExecutionSummaryEntity.getStartTs(), pipelineExecutionSummaryEntity.getEndTs())
        .execute();
  }
  public void update(String id, PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity) {}
  public void delete(String id) {}
}
