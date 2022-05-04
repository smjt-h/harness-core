/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.REJECTED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.HINGER;
import static io.harness.rule.OwnerRule.PRABU;
import static io.harness.rule.OwnerRule.SRINIVAS;

import static software.wings.api.EnvStateExecutionData.Builder.anEnvStateExecutionData;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.alert.AlertType.DEPLOYMENT_FREEZE_EVENT;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.FREEZE_WINDOW_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.FeatureName;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.DeploymentFreezeException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.ff.FeatureFlagService;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.api.ArtifactCollectionExecutionData;
import software.wings.api.EnvStateExecutionData;
import software.wings.api.WorkflowElement;
import software.wings.api.artifact.ServiceArtifactElement;
import software.wings.api.artifact.ServiceArtifactElements;
import software.wings.api.artifact.ServiceArtifactVariableElement;
import software.wings.api.artifact.ServiceArtifactVariableElements;
import software.wings.beans.Application;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.beans.artifact.Artifact;
import software.wings.common.NotificationMessageResolver;
import software.wings.service.impl.WorkflowExecutionUpdate;
import software.wings.service.impl.deployment.checks.DeploymentFreezeUtils;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class EnvStateTest extends WingsBaseTest {
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private ExecutionContextImpl context;
  @Mock private WorkflowService workflowService;
  @Mock private Workflow workflow;
  @Mock private CanaryOrchestrationWorkflow canaryOrchestrationWorkflow;
  @Mock private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Mock private ArtifactService artifactService;
  @Mock private SweepingOutputService sweepingOutputService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private NotificationMessageResolver notificationMessageResolver;
  @Mock private DeploymentFreezeUtils deploymentFreezeUtils;
  @Mock private WorkflowExecutionUpdate workflowExecutionUpdate;
  @Mock private Injector injector;

  private static final WorkflowElement workflowElement =
      WorkflowElement.builder()
          .artifactVariables(Collections.singletonList(
              ArtifactVariable.builder().name(ExpressionEvaluator.DEFAULT_ARTIFACT_VARIABLE_NAME).build()))
          .build();
  private static final WorkflowStandardParams WORKFLOW_STANDARD_PARAMS = aWorkflowStandardParams()
                                                                             .withAppId(APP_ID)
                                                                             .withArtifactIds(asList(ARTIFACT_ID))
                                                                             .withWorkflowElement(workflowElement)
                                                                             .build();

  @InjectMocks private EnvState envState = new EnvState("ENV_STATE");

  @Before
  public void setUp() throws Exception {
    envState.setWorkflowId(WORKFLOW_ID);
    when(context.getAccountId()).thenReturn(ACCOUNT_ID);
    when(context.getAppId()).thenReturn(APP_ID);
    when(context.getApp()).thenReturn(anApplication().uuid(APP_ID).build());
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(WORKFLOW_STANDARD_PARAMS);
    when(context.getWorkflowExecutionId()).thenReturn(PIPELINE_WORKFLOW_EXECUTION_ID);
    when(workflowExecutionService.triggerOrchestrationExecution(
             eq(APP_ID), eq(null), eq(WORKFLOW_ID), eq(PIPELINE_WORKFLOW_EXECUTION_ID), any(), any()))
        .thenReturn(WorkflowExecution.builder().uuid(WORKFLOW_EXECUTION_ID).status(ExecutionStatus.NEW).build());
    when(workflowService.readWorkflowWithoutServices(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldExecute() {
    when(workflow.getOrchestrationWorkflow()).thenReturn(canaryOrchestrationWorkflow);
    ExecutionResponse executionResponse = envState.execute(context);
    verify(workflowExecutionService)
        .triggerOrchestrationExecution(
            eq(APP_ID), eq(null), eq(WORKFLOW_ID), eq(PIPELINE_WORKFLOW_EXECUTION_ID), any(), any());
    assertThat(executionResponse.getCorrelationIds()).hasSameElementsAs(asList(WORKFLOW_EXECUTION_ID));
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(executionResponse.isAsync()).isTrue();
    EnvStateExecutionData stateExecutionData = (EnvStateExecutionData) executionResponse.getStateExecutionData();
    assertThat(stateExecutionData.getWorkflowId()).isEqualTo(WORKFLOW_ID);
    assertThat(stateExecutionData.getWorkflowExecutionId()).isEqualTo(WORKFLOW_EXECUTION_ID);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldExecuteWhenNoWorkflow() {
    ExecutionResponse executionResponse = envState.execute(context);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(FAILED);
    assertThat(executionResponse.getErrorMessage()).isNotEmpty();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldExecuteOnError() {
    when(workflow.getOrchestrationWorkflow()).thenReturn(canaryOrchestrationWorkflow);
    when(workflowExecutionService.triggerOrchestrationExecution(eq(APP_ID), eq(null), eq(WORKFLOW_ID),
             eq(PIPELINE_WORKFLOW_EXECUTION_ID), any(ExecutionArgs.class), any()))
        .thenThrow(new InvalidRequestException("Workflow variable [test] is mandatory for execution"));
    ExecutionResponse executionResponse = envState.execute(context);

    verify(workflowExecutionService)
        .triggerOrchestrationExecution(
            eq(APP_ID), eq(null), eq(WORKFLOW_ID), eq(PIPELINE_WORKFLOW_EXECUTION_ID), any(ExecutionArgs.class), any());
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(FAILED);
    assertThat(executionResponse.getErrorMessage()).isNotEmpty();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldSaveArtifactsForBuildWorkflow() {
    Artifact artifact1 = anArtifact().withUuid("a1").withDisplayName("a1dn").withArtifactStreamId("as1").build();
    Artifact artifact2 = anArtifact().withUuid("a2").withDisplayName("a2dn").withArtifactStreamId("as2").build();
    when(context.getStateExecutionData())
        .thenReturn(anEnvStateExecutionData()
                        .withOrchestrationWorkflowType(OrchestrationWorkflowType.BUILD)
                        .withWorkflowExecutionId(WORKFLOW_EXECUTION_ID)
                        .build());
    when(context.getStateExecutionInstanceId()).thenReturn("seiid1");
    when(workflowExecutionService.getArtifactsCollected(APP_ID, WORKFLOW_EXECUTION_ID))
        .thenReturn(asList(artifact1, artifact2));
    when(artifactStreamServiceBindingService.listServiceIds("as1")).thenReturn(asList("s1", "s2"));
    when(artifactStreamServiceBindingService.listServiceIds("as2")).thenReturn(Collections.singletonList("s3"));

    when(context.prepareSweepingOutputBuilder(any(SweepingOutputInstance.Scope.class))).thenAnswer(invocation -> {
      SweepingOutputInstance.Scope scope = invocation.getArgumentAt(0, SweepingOutputInstance.Scope.class);
      assertThat(scope).isEqualTo(SweepingOutputInstance.Scope.PIPELINE);
      return SweepingOutputInstance.builder();
    });

    when(sweepingOutputService.save(any(SweepingOutputInstance.class))).thenAnswer(invocation -> {
      SweepingOutputInstance sweepingOutputInstance = invocation.getArgumentAt(0, SweepingOutputInstance.class);
      assertThat(sweepingOutputInstance.getName()).startsWith(ServiceArtifactElements.SWEEPING_OUTPUT_NAME);
      assertThat(sweepingOutputInstance.getName()).contains("seiid1");
      assertThat(sweepingOutputInstance.getValue()).isInstanceOf(ServiceArtifactElements.class);
      ServiceArtifactElements artifactElements = (ServiceArtifactElements) sweepingOutputInstance.getValue();
      assertThat(artifactElements.getArtifactElements()).isNotNull();
      assertThat(artifactElements.getArtifactElements().size()).isEqualTo(2);
      ServiceArtifactElement artifactElement1 = artifactElements.getArtifactElements().get(0);
      assertThat(artifactElement1.getUuid()).isEqualTo("a1");
      assertThat(artifactElement1.getServiceIds()).containsExactly("s1", "s2");
      ServiceArtifactElement artifactElement2 = artifactElements.getArtifactElements().get(1);
      assertThat(artifactElement2.getUuid()).isEqualTo("a2");
      assertThat(artifactElement2.getServiceIds()).containsExactly("s3");
      return sweepingOutputInstance;
    });

    when(featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, ACCOUNT_ID)).thenReturn(false);

    envState.handleAsyncResponse(
        context, ImmutableMap.of("", new EnvState.EnvExecutionResponseData(WORKFLOW_EXECUTION_ID, SUCCESS)));
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldNotSaveArtifactsForBuildWorkflowWithNoArtifacts() {
    when(context.getStateExecutionData())
        .thenReturn(anEnvStateExecutionData()
                        .withOrchestrationWorkflowType(OrchestrationWorkflowType.BUILD)
                        .withWorkflowExecutionId(WORKFLOW_EXECUTION_ID)
                        .build());
    when(workflowExecutionService.getArtifactsCollected(APP_ID, WORKFLOW_EXECUTION_ID))
        .thenReturn(Collections.emptyList());
    when(featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, ACCOUNT_ID)).thenReturn(false);
    envState.handleAsyncResponse(
        context, ImmutableMap.of("", new EnvState.EnvExecutionResponseData(WORKFLOW_EXECUTION_ID, SUCCESS)));
    verify(sweepingOutputService, never()).save(any());
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldSaveArtifactVariablesForBuildWorkflow() {
    Artifact artifact1 = anArtifact().withUuid("a1").withDisplayName("a1dn").build();
    Artifact artifact2 = anArtifact().withUuid("a2").withDisplayName("a2dn").build();
    StateExecutionInstance instance1 = mock(StateExecutionInstance.class);
    StateExecutionInstance instance2 = mock(StateExecutionInstance.class);
    when(instance1.fetchStateExecutionData())
        .thenReturn(ArtifactCollectionExecutionData.builder()
                        .artifactId("a1")
                        .serviceId("s1")
                        .artifactVariableName("v1")
                        .build());
    when(instance2.fetchStateExecutionData())
        .thenReturn(ArtifactCollectionExecutionData.builder()
                        .artifactId("a2")
                        .serviceId("s2")
                        .artifactVariableName("v2")
                        .build());
    when(context.getStateExecutionData())
        .thenReturn(anEnvStateExecutionData()
                        .withOrchestrationWorkflowType(OrchestrationWorkflowType.BUILD)
                        .withWorkflowExecutionId(WORKFLOW_EXECUTION_ID)
                        .build());
    when(context.getStateExecutionInstanceId()).thenReturn("seiid2");
    when(workflowExecutionService.getStateExecutionInstances(APP_ID, WORKFLOW_EXECUTION_ID))
        .thenReturn(asList(instance1, instance2));
    when(artifactService.get("a1")).thenReturn(artifact1);
    when(artifactService.get("a2")).thenReturn(artifact2);

    when(context.prepareSweepingOutputBuilder(any(SweepingOutputInstance.Scope.class))).thenAnswer(invocation -> {
      SweepingOutputInstance.Scope scope = invocation.getArgumentAt(0, SweepingOutputInstance.Scope.class);
      assertThat(scope).isEqualTo(SweepingOutputInstance.Scope.PIPELINE);
      return SweepingOutputInstance.builder();
    });

    when(sweepingOutputService.save(any(SweepingOutputInstance.class))).thenAnswer(invocation -> {
      SweepingOutputInstance sweepingOutputInstance = invocation.getArgumentAt(0, SweepingOutputInstance.class);
      assertThat(sweepingOutputInstance.getName()).startsWith(ServiceArtifactVariableElements.SWEEPING_OUTPUT_NAME);
      assertThat(sweepingOutputInstance.getName()).contains("seiid2");
      assertThat(sweepingOutputInstance.getValue()).isInstanceOf(ServiceArtifactVariableElements.class);
      ServiceArtifactVariableElements artifactVariableElements =
          (ServiceArtifactVariableElements) sweepingOutputInstance.getValue();
      assertThat(artifactVariableElements.getArtifactVariableElements()).isNotNull();
      assertThat(artifactVariableElements.getArtifactVariableElements().size()).isEqualTo(2);
      ServiceArtifactVariableElement artifactVariableElement1 =
          artifactVariableElements.getArtifactVariableElements().get(0);
      assertThat(artifactVariableElement1.getUuid()).isEqualTo("a1");
      assertThat(artifactVariableElement1.getServiceId()).isEqualTo("s1");
      assertThat(artifactVariableElement1.getArtifactVariableName()).isEqualTo("v1");
      ServiceArtifactVariableElement artifactVariableElement2 =
          artifactVariableElements.getArtifactVariableElements().get(1);
      assertThat(artifactVariableElement2.getUuid()).isEqualTo("a2");
      assertThat(artifactVariableElement2.getServiceId()).isEqualTo("s2");
      assertThat(artifactVariableElement2.getArtifactVariableName()).isEqualTo("v2");
      return sweepingOutputInstance;
    });

    when(featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, ACCOUNT_ID)).thenReturn(true);

    envState.handleAsyncResponse(
        context, ImmutableMap.of("", new EnvState.EnvExecutionResponseData(WORKFLOW_EXECUTION_ID, SUCCESS)));
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldNotSaveArtifactVariablesForBuildWorkflowWithNoArtifacts() {
    when(context.getStateExecutionData())
        .thenReturn(anEnvStateExecutionData()
                        .withOrchestrationWorkflowType(OrchestrationWorkflowType.BUILD)
                        .withWorkflowExecutionId(WORKFLOW_EXECUTION_ID)
                        .build());
    when(workflowExecutionService.getStateExecutionInstances(APP_ID, WORKFLOW_EXECUTION_ID))
        .thenReturn(Collections.emptyList());
    when(featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, ACCOUNT_ID)).thenReturn(true);
    envState.handleAsyncResponse(
        context, ImmutableMap.of("", new EnvState.EnvExecutionResponseData(WORKFLOW_EXECUTION_ID, SUCCESS)));
    verify(sweepingOutputService, never()).save(any());
  }

  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void shouldCallResolveTagsAfterPipelineStageFinishes() {
    when(context.getStateExecutionData())
        .thenReturn(anEnvStateExecutionData()
                        .withOrchestrationWorkflowType(OrchestrationWorkflowType.BUILD)
                        .withWorkflowExecutionId(WORKFLOW_EXECUTION_ID)
                        .build());

    when(context.getWorkflowType()).thenReturn(WorkflowType.PIPELINE);
    when(context.getApp()).thenReturn(Application.Builder.anApplication().accountId(ACCOUNT_ID).build());

    when(context.getAppId()).thenReturn(APP_ID);
    when(context.getWorkflowExecutionId()).thenReturn(WORKFLOW_EXECUTION_ID);

    when(workflowExecutionService.getStateExecutionInstances(APP_ID, WORKFLOW_EXECUTION_ID))
        .thenReturn(Collections.emptyList());
    when(featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, ACCOUNT_ID)).thenReturn(true);
    when(featureFlagService.isEnabled(FeatureName.RESOLVE_DEPLOYMENT_TAGS_BEFORE_EXECUTION, ACCOUNT_ID))
        .thenReturn(true);

    envState.handleAsyncResponse(
        context, ImmutableMap.of("", new EnvState.EnvExecutionResponseData(WORKFLOW_EXECUTION_ID, SUCCESS)));

    verify(workflowExecutionUpdate, atLeastOnce()).resolveDeploymentTags(any(), any());
    verify(workflowExecutionUpdate, atLeastOnce()).addTagsToWorkflowExecution(any());
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetTimeout() {
    Integer timeoutMillis = envState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo(EnvState.ENV_STATE_TIMEOUT_MILLIS);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetSetTimeout() {
    envState.setTimeoutMillis((int) TimeUnit.HOURS.toMillis(1));
    Integer timeoutMillis = envState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo((int) TimeUnit.HOURS.toMillis(1));
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldHandleAbort() {
    envState.setTimeoutMillis((int) (0.6 * TimeUnit.HOURS.toMillis(1)));
    when(context.getStateExecutionData())
        .thenReturn(anEnvStateExecutionData().withWorkflowId(WORKFLOW_ID).withEnvId(ENV_ID).build());
    envState.handleAbortEvent(context);
    assertThat(context.getStateExecutionData()).isNotNull();
    assertThat(context.getStateExecutionData().getErrorMsg()).contains("Workflow not completed within 36m");
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldReturnRejectedResponseAndSendNotificationOnDeploymentFreeze() {
    when(workflow.getOrchestrationWorkflow()).thenReturn(canaryOrchestrationWorkflow);
    List<String> deploymentFreezeIds = Collections.singletonList(FREEZE_WINDOW_ID);
    when(workflowExecutionService.triggerOrchestrationExecution(
             eq(APP_ID), eq(null), eq(WORKFLOW_ID), eq(PIPELINE_WORKFLOW_EXECUTION_ID), any(), any()))
        .thenThrow(new DeploymentFreezeException(ErrorCode.DEPLOYMENT_GOVERNANCE_ERROR, Level.INFO, WingsException.USER,
            ACCOUNT_ID, deploymentFreezeIds, "", false, false));
    when(workflowExecutionService.fetchWorkflowExecution(APP_ID, PIPELINE_WORKFLOW_EXECUTION_ID,
             WorkflowExecutionKeys.createdAt, WorkflowExecutionKeys.triggeredBy, WorkflowExecutionKeys.status))
        .thenReturn(WorkflowExecution.builder().build());
    Map<String, String> emptyMap = new HashMap<>();
    when(notificationMessageResolver.getPlaceholderValues(eq(context), eq(""), eq(0), anyLong(), eq(""), eq("rejected"),
             eq(""), eq(REJECTED), eq(DEPLOYMENT_FREEZE_EVENT)))
        .thenReturn(emptyMap);

    ExecutionResponse executionResponse = envState.execute(context);
    verify(workflowExecutionService)
        .triggerOrchestrationExecution(
            eq(APP_ID), eq(null), eq(WORKFLOW_ID), eq(PIPELINE_WORKFLOW_EXECUTION_ID), any(), any());
    verify(deploymentFreezeUtils, times(1))
        .sendPipelineRejectionNotification(ACCOUNT_ID, APP_ID, deploymentFreezeIds, emptyMap);
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(REJECTED);
    assertThat(executionResponse.getErrorMessage()).isNotBlank();
    EnvStateExecutionData stateExecutionData = (EnvStateExecutionData) executionResponse.getStateExecutionData();
    assertThat(stateExecutionData.getWorkflowId()).isEqualTo(WORKFLOW_ID);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldReturnRejectedResponseForMasterDeploymentFreeze() {
    when(workflow.getOrchestrationWorkflow()).thenReturn(canaryOrchestrationWorkflow);
    when(workflowExecutionService.triggerOrchestrationExecution(
             eq(APP_ID), eq(null), eq(WORKFLOW_ID), eq(PIPELINE_WORKFLOW_EXECUTION_ID), any(), any()))
        .thenThrow(new DeploymentFreezeException(ErrorCode.DEPLOYMENT_GOVERNANCE_ERROR, Level.INFO, WingsException.USER,
            ACCOUNT_ID, Collections.emptyList(), "", true, false));
    when(workflowExecutionService.fetchWorkflowExecution(APP_ID, PIPELINE_WORKFLOW_EXECUTION_ID,
             WorkflowExecutionKeys.createdAt, WorkflowExecutionKeys.triggeredBy, WorkflowExecutionKeys.status))
        .thenReturn(WorkflowExecution.builder().build());

    ExecutionResponse executionResponse = envState.execute(context);
    verify(workflowExecutionService)
        .triggerOrchestrationExecution(
            eq(APP_ID), eq(null), eq(WORKFLOW_ID), eq(PIPELINE_WORKFLOW_EXECUTION_ID), any(), any());
    verify(deploymentFreezeUtils, never())
        .sendPipelineRejectionNotification(eq(ACCOUNT_ID), eq(APP_ID), anyList(), anyMap());
    assertThat(executionResponse).isNotNull();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(REJECTED);
    assertThat(executionResponse.getErrorMessage())
        .isEqualTo("Master Deployment Freeze is active. No deployments are allowed.");
    EnvStateExecutionData stateExecutionData = (EnvStateExecutionData) executionResponse.getStateExecutionData();
    assertThat(stateExecutionData.getWorkflowId()).isEqualTo(WORKFLOW_ID);
  }
}
