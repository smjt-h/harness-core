/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.plan.execution;

import static io.harness.rule.OwnerRule.SAMARTH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import io.harness.CategoryTest;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.beans.RepresentationStrategy;
import io.harness.category.element.UnitTests;
import io.harness.dto.OrchestrationAdjacencyListDTO;
import io.harness.dto.OrchestrationGraphDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.interceptor.GitEntityFindInfoDTO;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.beans.dto.PipelineExecutionDetailDTO;
import io.harness.pms.plan.execution.beans.dto.PipelineExecutionSummaryDTO;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.rule.Owner;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

public class ExecutionDetailsResourceTest extends CategoryTest {
  @InjectMocks ExecutionDetailsResource executionDetailsResource;
  @Mock PMSExecutionService pmsExecutionService;
  @Mock PMSPipelineService pmsPipelineService;
  @Mock AccessControlClient accessControlClient;
  @Mock PmsGitSyncHelper pmsGitSyncHelper;

  private final String ACCOUNT_ID = "account_id";
  private final String ORG_IDENTIFIER = "orgId";
  private final String PROJ_IDENTIFIER = "projId";
  private final String PIPELINE_IDENTIFIER = "basichttpFail";
  private final String PLAN_EXECUTION_ID = "planId";
  private final String STAGE_NODE_ID = "stageNodeId";

  PipelineEntity entity;
  PipelineEntity entityWithVersion;
  PipelineExecutionSummaryEntity executionSummaryEntity;
  OrchestrationGraphDTO orchestrationGraph;
  EntityGitDetails entityGitDetails;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    entity = PipelineEntity.builder()
                 .accountId(ACCOUNT_ID)
                 .orgIdentifier(ORG_IDENTIFIER)
                 .projectIdentifier(PROJ_IDENTIFIER)
                 .identifier(PIPELINE_IDENTIFIER)
                 .name(PIPELINE_IDENTIFIER)
                 .build();

    entityGitDetails = EntityGitDetails.builder()
                           .branch("branch")
                           .repoIdentifier("repo")
                           .filePath("file.yaml")
                           .rootFolder("root/.harness/")
                           .build();

    entityWithVersion = PipelineEntity.builder()
                            .accountId(ACCOUNT_ID)
                            .orgIdentifier(ORG_IDENTIFIER)
                            .projectIdentifier(PROJ_IDENTIFIER)
                            .identifier(PIPELINE_IDENTIFIER)
                            .name(PIPELINE_IDENTIFIER)
                            .stageCount(1)
                            .stageName("qaStage")
                            .version(1L)
                            .build();

    executionSummaryEntity = PipelineExecutionSummaryEntity.builder()
                                 .accountId(ACCOUNT_ID)
                                 .orgIdentifier(ORG_IDENTIFIER)
                                 .projectIdentifier(PROJ_IDENTIFIER)
                                 .pipelineIdentifier(PIPELINE_IDENTIFIER)
                                 .planExecutionId(PLAN_EXECUTION_ID)
                                 .name(PLAN_EXECUTION_ID)
                                 .runSequence(0)
                                 .entityGitDetails(entityGitDetails)
                                 .build();

    orchestrationGraph = OrchestrationGraphDTO.builder()
                             .planExecutionId(PLAN_EXECUTION_ID)
                             .rootNodeIds(Collections.singletonList(STAGE_NODE_ID))
                             .adjacencyList(OrchestrationAdjacencyListDTO.builder()
                                                .graphVertexMap(Collections.emptyMap())
                                                .adjacencyMap(Collections.emptyMap())
                                                .build())
                             .build();
  }

  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void testGetListOfExecutions() {
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Direction.DESC, PipelineEntityKeys.createdAt));
    Page<PipelineExecutionSummaryEntity> pipelineExecutionSummaryEntities =
        new PageImpl<>(Collections.singletonList(executionSummaryEntity), pageable, 1);
    doReturn(pipelineExecutionSummaryEntities)
        .when(pmsExecutionService)
        .getPipelineExecutionSummaryEntity(any(), any());
    doReturn(Optional.of(PipelineEntity.builder().build()))
        .when(pmsPipelineService)
        .get(anyString(), anyString(), anyString(), anyString(), anyBoolean());

    doReturn(null).when(pmsGitSyncHelper).getGitSyncBranchContextBytesThreadLocal();

    Page<PipelineExecutionSummaryDTO> content =
        executionDetailsResource
            .getListOfExecutions(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, null, null, 0, 10, null, null, null, null,
                null, false, GitEntityFindInfoDTO.builder().build())
            .getData();
    assertThat(content).isNotEmpty();
    assertThat(content.getNumberOfElements()).isEqualTo(1);

    PipelineExecutionSummaryDTO responseDTO = content.toList().get(0);
    assertThat(responseDTO.getPipelineIdentifier()).isEqualTo(PIPELINE_IDENTIFIER);
    assertThat(responseDTO.getPlanExecutionId()).isEqualTo(PLAN_EXECUTION_ID);
    assertThat(responseDTO.getRunSequence()).isEqualTo(0);
  }

  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void testGetExecutionDetail() {
    doReturn(executionSummaryEntity)
        .when(pmsExecutionService)
        .getPipelineExecutionSummaryEntity(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, PLAN_EXECUTION_ID, false);
    doReturn(orchestrationGraph).when(pmsExecutionService).getOrchestrationGraph(STAGE_NODE_ID, PLAN_EXECUTION_ID);

    doNothing().when(accessControlClient).checkForAccessOrThrow(any(), any(), any());

    ResponseDTO<PipelineExecutionDetailDTO> executionDetails = executionDetailsResource.getExecutionDetail(
        ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, STAGE_NODE_ID, PLAN_EXECUTION_ID);

    assertThat(executionDetails.getData().getPipelineExecutionSummary().getPipelineIdentifier())
        .isEqualTo(PIPELINE_IDENTIFIER);
    assertThat(executionDetails.getData().getPipelineExecutionSummary().getPlanExecutionId())
        .isEqualTo(PLAN_EXECUTION_ID);
    assertThat(executionDetails.getData().getPipelineExecutionSummary().getRunSequence()).isEqualTo(0);
    assertThat(executionDetails.getData().getExecutionGraph().getRootNodeId()).isEqualTo(STAGE_NODE_ID);
    assertThat(executionDetails.getData().getExecutionGraph().getNodeMap().size()).isEqualTo(0);
    assertThat(executionDetails.getData().getExecutionGraph().getRepresentationStrategy())
        .isEqualTo(RepresentationStrategy.CAMELCASE);
  }

  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void testGetExecutionDetailWithInvalidExecutionId() {
    String invalidPlanExecutionId = "invalidId";
    doThrow(InvalidRequestException.class)
        .when(pmsExecutionService)
        .getPipelineExecutionSummaryEntity(ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, invalidPlanExecutionId, false);

    assertThatThrownBy(()
                           -> executionDetailsResource.getExecutionDetail(
                               ACCOUNT_ID, ORG_IDENTIFIER, PROJ_IDENTIFIER, STAGE_NODE_ID, invalidPlanExecutionId))
        .isInstanceOf(InvalidRequestException.class);
  }
}
