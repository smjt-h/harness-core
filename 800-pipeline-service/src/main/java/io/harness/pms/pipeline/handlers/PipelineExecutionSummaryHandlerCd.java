package io.harness.pms.pipeline.handlers;

import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.timescaledb.Tables;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import java.util.HashMap;
import java.util.List;
import org.bson.Document;
import org.jooq.DSLContext;
import org.jooq.Record;

public class PipelineExecutionSummaryHandlerCd {
  @Inject private DSLContext dsl;
  Record record = dsl.newRecord(Tables.PIPELINE_EXECUTION_SUMMARY_CD);

  List<PipelineExecutionSummaryEntity> fetch(String id) {
    return dsl.select()
        .from(Tables.PIPELINE_EXECUTION_SUMMARY_CD)
        .where(Tables.PIPELINE_EXECUTION_SUMMARY_CD.ID.equals(id))
        .fetch()
        .into(PipelineExecutionSummaryEntity.class);
  }

  public void createRecord(PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity, String id) {
    record.set(Tables.PIPELINE_EXECUTION_SUMMARY_CD.ID, id);
    record.set(Tables.PIPELINE_EXECUTION_SUMMARY_CD.MODULEINFO_TYPE, "CD");
    if (pipelineExecutionSummaryEntity.getAccountId() != null) {
      record.set(Tables.PIPELINE_EXECUTION_SUMMARY_CD.ACCOUNTID, pipelineExecutionSummaryEntity.getAccountId());
    }
    if (pipelineExecutionSummaryEntity.getOrgIdentifier() != null) {
      record.set(Tables.PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER, pipelineExecutionSummaryEntity.getOrgIdentifier());
    }
    if (pipelineExecutionSummaryEntity.getPipelineIdentifier() != null) {
      record.set(Tables.PIPELINE_EXECUTION_SUMMARY_CD.PIPELINEIDENTIFIER,
          pipelineExecutionSummaryEntity.getPipelineIdentifier());
    }
    if (pipelineExecutionSummaryEntity.getProjectIdentifier() != null) {
      record.set(Tables.PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER,
          pipelineExecutionSummaryEntity.getProjectIdentifier());
    }
    if (pipelineExecutionSummaryEntity.getPlanExecutionId() != null) {
      record.set(
          Tables.PIPELINE_EXECUTION_SUMMARY_CD.PLANEXECUTIONID, pipelineExecutionSummaryEntity.getPlanExecutionId());
    }
    if (pipelineExecutionSummaryEntity.getName() != null) {
      record.set(Tables.PIPELINE_EXECUTION_SUMMARY_CD.NAME, pipelineExecutionSummaryEntity.getName());
    }
    if (pipelineExecutionSummaryEntity.getStatus() != null) {
      record.set(Tables.PIPELINE_EXECUTION_SUMMARY_CD.STATUS, pipelineExecutionSummaryEntity.getStatus().toString());
    }

    if (pipelineExecutionSummaryEntity.getStartTs() != null) {
      record.set(Tables.PIPELINE_EXECUTION_SUMMARY_CD.STARTTS, pipelineExecutionSummaryEntity.getStartTs());
    }
    if (pipelineExecutionSummaryEntity.getEndTs() != null) {
      record.set(Tables.PIPELINE_EXECUTION_SUMMARY_CD.ENDTS, pipelineExecutionSummaryEntity.getEndTs());
    }
    if (pipelineExecutionSummaryEntity.getExecutionTriggerInfo() != null) {
      if (pipelineExecutionSummaryEntity.getExecutionTriggerInfo().getTriggerType() != null) {
        record.set(Tables.PIPELINE_EXECUTION_SUMMARY_CD.TRIGGER_TYPE,
            pipelineExecutionSummaryEntity.getExecutionTriggerInfo().getTriggerType().toString());
      }
      if (pipelineExecutionSummaryEntity.getExecutionTriggerInfo().getTriggeredBy() != null
          && pipelineExecutionSummaryEntity.getExecutionTriggerInfo().getTriggeredBy().getIdentifier() != null) {
        record.set(Tables.PIPELINE_EXECUTION_SUMMARY_CD.MODULEINFO_AUTHOR_ID,
            pipelineExecutionSummaryEntity.getExecutionTriggerInfo().getTriggeredBy().getIdentifier());
      }
    }

    if (pipelineExecutionSummaryEntity.getModuleInfo().get("ci") != null) {
      Document ciDocument = pipelineExecutionSummaryEntity.getModuleInfo().get("ci");
      DBObject ciObject = (DBObject) new BasicDBObject(ciDocument);
      DBObject ciExecutionInfo = (DBObject) ciObject.get("ciExecutionInfoDTO");
      if (ciObject.get("repoName") != null) {
        record.set(Tables.PIPELINE_EXECUTION_SUMMARY_CD.MODULEINFO_REPOSITORY, ciObject.get("repoName").toString());
      }

      if (ciObject.get("branch") != null) {
        record.set(Tables.PIPELINE_EXECUTION_SUMMARY_CD.MODULEINFO_BRANCH_NAME, ciObject.get("branch").toString());
      }

      //            if (ciObject.get("isPrivateRepo") != null) {
      //              fields.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.MOd)
      //              columnValueMapping.put("moduleinfo_is_private", ciObject.get("isPrivateRepo").toString());
      //            }

      if (ciExecutionInfo != null) {
        DBObject branch = (DBObject) (ciExecutionInfo.get("branch"));

        HashMap firstCommit = null;
        String commits = "commits";
        if (branch != null && branch.get(commits) != null && ((List) branch.get(commits)).size() > 0) {
          firstCommit = (HashMap) ((List) branch.get(commits)).get(0);
          if (firstCommit != null) {
            if (firstCommit.get("id") != null) {
              record.set(
                  Tables.PIPELINE_EXECUTION_SUMMARY_CD.MODULEINFO_BRANCH_COMMIT_ID, firstCommit.get("id").toString());
            }
            if (firstCommit.get("message") != null) {
              record.set(Tables.PIPELINE_EXECUTION_SUMMARY_CD.MODULEINFO_BRANCH_COMMIT_MESSAGE,
                  firstCommit.get("message").toString());
            }
          }
        } else if (ciExecutionInfo.get("pullRequest") != null) {
          DBObject pullRequestObject = (DBObject) ciExecutionInfo.get("pullRequest");

          if (pullRequestObject.get("sourceBranch") != null) {
            record.set(
                Tables.PIPELINE_EXECUTION_SUMMARY_CD.SOURCE_BRANCH, pullRequestObject.get("sourceBranch").toString());
          }

          //          if (pullRequestObject.get("id") != null) {
          //            fields.add(Tables.PIPELINE_EXECUTION_SUMMARY_CD.PR)
          //            columnValueMapping.put("pr",
          //            String.valueOf(Long.parseLong(pullRequestObject.get("id").toString())));
          //          }

          if (pullRequestObject.get(commits) != null && ((List) pullRequestObject.get(commits)).size() > 0) {
            firstCommit = (HashMap) ((List) pullRequestObject.get(commits)).get(0);
            if (firstCommit != null) {
              if (firstCommit.get("id") != null) {
                record.set(
                    Tables.PIPELINE_EXECUTION_SUMMARY_CD.MODULEINFO_BRANCH_COMMIT_ID, firstCommit.get("id").toString());
              }
              if (firstCommit.get("message") != null) {
                record.set(Tables.PIPELINE_EXECUTION_SUMMARY_CD.MODULEINFO_BRANCH_COMMIT_MESSAGE,
                    firstCommit.get("message").toString());
              }
            }
          }
        }
        DBObject author = (DBObject) (ciExecutionInfo.get("author"));
        if (author != null) {
          record.set(Tables.PIPELINE_EXECUTION_SUMMARY_CD.MODULEINFO_AUTHOR_ID, author.get("id").toString());
          record.set(Tables.PIPELINE_EXECUTION_SUMMARY_CD.AUTHOR_NAME, author.get("name").toString());
          record.set(Tables.PIPELINE_EXECUTION_SUMMARY_CD.AUTHOR_AVATAR, author.get("avatar").toString());
        }
        if (ciExecutionInfo.get("event") != null) {
          record.set(Tables.PIPELINE_EXECUTION_SUMMARY_CD.MODULEINFO_EVENT, ciExecutionInfo.get("event").toString());
        }
      }
    }
  }

  public void insert(String id, PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity) {
    if (fetch(id).size() != 0) {
      return;
    }
    createRecord(pipelineExecutionSummaryEntity, id);
    dsl.insertInto(Tables.PIPELINE_EXECUTION_SUMMARY_CD).set(record).execute();
  }

  public void update(String id, PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity) {
    if (fetch(id).size() == 0) {
      return;
    }
    createRecord(pipelineExecutionSummaryEntity, id);
    dsl.update(Tables.PIPELINE_EXECUTION_SUMMARY_CD)
        .set(record)
        .where(Tables.PIPELINE_EXECUTION_SUMMARY_CD.ID.equals(id))
        .execute();
  }

  public void delete(String id) {
    if (fetch(id).size() == 0) {
      return;
    }
    dsl.delete(Tables.PIPELINE_EXECUTION_SUMMARY_CD)
        .where(Tables.PIPELINE_EXECUTION_SUMMARY_CD.ID.equals(id))
        .execute();
  }
}
