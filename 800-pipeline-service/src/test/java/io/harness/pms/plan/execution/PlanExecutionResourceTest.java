/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.plan.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;
import static io.harness.rule.OwnerRule.PRASHANTSHARMA;
import static io.harness.rule.OwnerRule.UTKARSH_CHOUBEY;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.retry.RetryExecutionMetadata;
import io.harness.execution.PlanExecution;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.template.TemplateMergeResponseDTO;
import io.harness.pms.helpers.PmsFeatureFlagHelper;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.pipeline.service.PMSPipelineTemplateHelper;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.beans.dto.RunStageRequestDTO;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.pms.stages.StageExecutionResponse;
import io.harness.rule.Owner;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PIPELINE)
public class PlanExecutionResourceTest extends CategoryTest {
  @InjectMocks PlanExecutionResource planExecutionResource;
  @Mock PMSPipelineService pmsPipelineService;
  @Mock PipelineExecutor pipelineExecutor;
  @Mock PMSExecutionService pmsExecutionService;
  @Mock RetryExecutionHelper retryExecutionHelper;
  @Mock PmsFeatureFlagHelper featureFlagService;
  @Mock PMSPipelineTemplateHelper pipelineTemplateHelper;

  private final String ACCOUNT_ID = "account_id";
  private final String ORG_IDENTIFIER = "orgId";
  private final String PROJ_IDENTIFIER = "projId";
  private final String PIPELINE_IDENTIFIER = "p1";

  String yaml = "pipeline:\n"
      + "  identifier: p1\n"
      + "  name: p1\n"
      + "  allowStageExecutions: true\n"
      + "  stages:\n"
      + "  - stage:\n"
      + "      identifier: qaStage\n"
      + "      type: Approval\n"
      + "      name: qa stage\n"
      + "  - stage:\n"
      + "      identifier: qaStage2\n"
      + "      type: Deployment\n"
      + "      name: qa stage 2";

  PipelineEntity entity;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    entity = PipelineEntity.builder()
                 .accountId(ACCOUNT_ID)
                 .orgIdentifier(ORG_IDENTIFIER)
                 .projectIdentifier(PROJ_IDENTIFIER)
                 .identifier(PIPELINE_IDENTIFIER)
                 .name(PIPELINE_IDENTIFIER)
                 .yaml(yaml)
                 .allowStageExecutions(true)
                 .build();
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetStagesExecutionList() {
    when(featureFlagService.isEnabled(anyString(), eq(FeatureName.NG_PIPELINE_TEMPLATE))).thenReturn(false);
    doReturn(Optional.of(entity))
        .when(pmsPipelineService)
        .get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, false);
    ResponseDTO<List<StageExecutionResponse>> stagesExecutionList = planExecutionResource.getStagesExecutionList(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, null);
    assertThat(stagesExecutionList.getData()).hasSize(2);
    StageExecutionResponse stage0Data = stagesExecutionList.getData().get(0);
    assertThat(stage0Data.getStageIdentifier()).isEqualTo("qaStage");
    assertThat(stage0Data.getStageName()).isEqualTo("qa stage");
    assertThat(stage0Data.getMessage()).isEqualTo("Running an approval stage individually can be redundant");
    assertThat(stage0Data.getStagesRequired()).hasSize(0);
    StageExecutionResponse stage1Data = stagesExecutionList.getData().get(1);
    assertThat(stage1Data.getStageIdentifier()).isEqualTo("qaStage2");
    assertThat(stage1Data.getStageName()).isEqualTo("qa stage 2");
    assertThat(stage1Data.getMessage()).isNull();
    assertThat(stage1Data.getStagesRequired()).hasSize(0);
  }

  @Test
  @Owner(developers = UTKARSH_CHOUBEY)
  @Category(UnitTests.class)
  public void testGetStagesExecutionListWhenFfIsOn() {
    when(featureFlagService.isEnabled(anyString(), eq(FeatureName.NG_PIPELINE_TEMPLATE))).thenReturn(true);
    doReturn(Optional.of(entity))
        .when(pmsPipelineService)
        .get(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, false);
    TemplateMergeResponseDTO templateMergeResponseDTO =
        TemplateMergeResponseDTO.builder().mergedPipelineYaml(yaml).build();
    doReturn(templateMergeResponseDTO)
        .when(pipelineTemplateHelper)
        .resolveTemplateRefsInPipeline(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, yaml);
    ResponseDTO<List<StageExecutionResponse>> stagesExecutionList = planExecutionResource.getStagesExecutionList(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, null);
    assertThat(stagesExecutionList.getData()).hasSize(2);
    StageExecutionResponse stage0Data = stagesExecutionList.getData().get(0);
    assertThat(stage0Data.getStageIdentifier()).isEqualTo("qaStage");
    assertThat(stage0Data.getStageName()).isEqualTo("qa stage");
    assertThat(stage0Data.getMessage()).isEqualTo("Running an approval stage individually can be redundant");
    assertThat(stage0Data.getStagesRequired()).hasSize(0);
    StageExecutionResponse stage1Data = stagesExecutionList.getData().get(1);
    assertThat(stage1Data.getStageIdentifier()).isEqualTo("qaStage2");
    assertThat(stage1Data.getStageName()).isEqualTo("qa stage 2");
    assertThat(stage1Data.getMessage()).isNull();
    assertThat(stage1Data.getStagesRequired()).hasSize(0);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testRunStagesWithRuntimeInputYaml() {
    PlanExecutionResponseDto planExecutionResponseDto =
        PlanExecutionResponseDto.builder().planExecution(PlanExecution.builder().planId("someId").build()).build();
    doReturn(planExecutionResponseDto)
        .when(pipelineExecutor)
        .runStagesWithRuntimeInputYaml(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, "cd",
            RunStageRequestDTO.builder().build(), false);
    ResponseDTO<PlanExecutionResponseDto> dto = planExecutionResource.runStagesWithRuntimeInputYaml(ACCOUNT_ID,
        ORG_IDENTIFIER, PROJ_IDENTIFIER, "cd", PIPELINE_IDENTIFIER, null, false, RunStageRequestDTO.builder().build());
    assertThat(dto.getData()).isEqualTo(planExecutionResponseDto);
    verify(pipelineExecutor, times(1))
        .runStagesWithRuntimeInputYaml(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, "cd",
            RunStageRequestDTO.builder().build(), false);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetRetryHistory() {
    when(pmsExecutionService.getPipelineExecutionSummaryEntity(
             ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, "planExecutionId", false))
        .thenReturn(
            PipelineExecutionSummaryEntity.builder()
                .uuid("uuid")
                .planExecutionId("planExecutionId")
                .retryExecutionMetadata(RetryExecutionMetadata.builder().rootExecutionId("rootExecutionId").build())
                .build());
    planExecutionResource.getRetryHistory(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, "planExecutionId");
    verify(retryExecutionHelper, times(1)).getRetryHistory("rootExecutionId");
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetLatestExecutionId() {
    when(pmsExecutionService.getPipelineExecutionSummaryEntity(
             ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, "planExecutionId", false))
        .thenReturn(
            PipelineExecutionSummaryEntity.builder()
                .uuid("uuid")
                .planExecutionId("planExecutionId")
                .retryExecutionMetadata(RetryExecutionMetadata.builder().rootExecutionId("rootExecutionId").build())
                .build());
    planExecutionResource.getRetryLatestExecutionId(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PIPELINE_IDENTIFIER, "planExecutionId");
    verify(retryExecutionHelper, times(1)).getRetryLatestExecutionId("rootExecutionId");
  }
}
