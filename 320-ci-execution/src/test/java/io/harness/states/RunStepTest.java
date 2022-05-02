/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.states;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.beans.outcomes.LiteEnginePodDetailsOutcome.POD_DETAILS_OUTCOME;
import static io.harness.beans.steps.stepinfo.InitializeStepInfo.CALLBACK_IDS;
import static io.harness.beans.steps.stepinfo.InitializeStepInfo.LOG_KEYS;
import static io.harness.beans.sweepingoutputs.CISweepingOutputNames.CODE_BASE_CONNECTOR_REF;
import static io.harness.beans.sweepingoutputs.ContainerPortDetails.PORT_DETAILS;
import static io.harness.beans.sweepingoutputs.StageInfraDetails.STAGE_INFRA_DETAILS;
import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.rule.OwnerRule.SHUBHAM;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.outcomes.LiteEnginePodDetailsOutcome;
import io.harness.beans.outcomes.VmDetailsOutcome;
import io.harness.beans.steps.outcome.CIStepOutcome;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.sweepingoutputs.CodeBaseConnectorRefSweepingOutput;
import io.harness.beans.sweepingoutputs.ContainerPortDetails;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8StageInfraDetails;
import io.harness.beans.sweepingoutputs.StageDetails;
import io.harness.beans.sweepingoutputs.StepLogKeyDetails;
import io.harness.beans.sweepingoutputs.StepTaskDetails;
import io.harness.beans.sweepingoutputs.VmStageInfraDetails;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml.K8sDirectInfraYamlSpec;
import io.harness.category.element.UnitTests;
import io.harness.ci.config.CIExecutionServiceConfig;
import io.harness.ci.serializer.RunStepProtobufSerializer;
import io.harness.ci.serializer.vm.VmStepSerializer;
import io.harness.delegate.beans.ci.vm.VmTaskExecutionResponse;
import io.harness.delegate.beans.ci.vm.steps.VmRunStep;
import io.harness.delegate.task.stepstatus.StepExecutionStatus;
import io.harness.delegate.task.stepstatus.StepMapOutput;
import io.harness.delegate.task.stepstatus.StepStatus;
import io.harness.delegate.task.stepstatus.StepStatusTaskResponseData;
import io.harness.executionplan.CIExecutionTestBase;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logserviceclient.CILogServiceUtils;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.plan.execution.SetupAbstractionKeys;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.rule.Owner;
import io.harness.stateutils.buildstate.ConnectorUtils;
import io.harness.tasks.ResponseData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(CI)
public class RunStepTest extends CIExecutionTestBase {
  public static final String STEP_ID = "runStepId";
  public static final String OUTPUT_KEY = "VAR1";
  public static final String OUTPUT_VALUE = "VALUE1";
  public static final String STEP_RESPONSE = "runStep";
  public static final String ERROR = "Error executing run step";
  @Mock private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Mock private OutcomeService outcomeService;
  @Mock private ConnectorUtils connectorUtils;
  @Mock private CIDelegateTaskExecutor ciDelegateTaskExecutor;
  @Mock private RunStepProtobufSerializer runStepProtobufSerializer;
  @Mock private CIExecutionServiceConfig ciExecutionServiceConfig;
  @Mock private VmStepSerializer vmStepSerializer;
  @Mock CILogServiceUtils logServiceUtils;
  @InjectMocks RunStep runStep;

  private Ambiance ambiance;
  private RunStepInfo stepInfo;
  private StepElementParameters stepElementParameters;
  private StepInputPackage stepInputPackage;
  private StepTaskDetails stepTaskDetails;
  private ContainerPortDetails containerPortDetails;
  private StepLogKeyDetails stepLogKeyDetails;
  private final String callbackId = UUID.randomUUID().toString();
  private Map<String, ResponseData> responseDataMap;
  private CodeBaseConnectorRefSweepingOutput codeBaseConnectorRefSweepingOutput;

  @Before
  public void setUp() {
    HashMap<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put(SetupAbstractionKeys.accountId, "accountId");
    setupAbstractions.put(SetupAbstractionKeys.projectIdentifier, "projectId");
    setupAbstractions.put(SetupAbstractionKeys.orgIdentifier, "orgId");
    ambiance =
        Ambiance.newBuilder()
            .setMetadata(ExecutionMetadata.newBuilder().setPipelineIdentifier("pipelineId").setRunSequence(1).build())
            .putAllSetupAbstractions(setupAbstractions)
            .addLevels(Level.newBuilder().setRuntimeId("runtimeId").setIdentifier("runStepId").setRetryIndex(1).build())
            .build();
    stepInfo = RunStepInfo.builder()
                   .identifier(STEP_ID)
                   .command(ParameterField.<String>builder().expressionValue("ls").build())
                   .image(ParameterField.<String>builder().expressionValue("alpine").build())
                   .build();
    stepElementParameters = StepElementParameters.builder().name("name").spec(stepInfo).build();
    stepInputPackage = StepInputPackage.builder().build();
    Map<String, String> callbackIds = new HashMap<>();
    callbackIds.put(STEP_ID, callbackId);
    stepTaskDetails = StepTaskDetails.builder().taskIds(callbackIds).build();
    Map<String, List<Integer>> portDetails = new HashMap<>();
    portDetails.put(STEP_ID, asList(10));

    containerPortDetails = ContainerPortDetails.builder().portDetails(portDetails).build();
    responseDataMap = new HashMap<>();
    codeBaseConnectorRefSweepingOutput =
        CodeBaseConnectorRefSweepingOutput.builder().codeBaseConnectorRef("codeBaseConnectorRef").build();
  }

  @After
  public void tearDown() throws Exception {
    responseDataMap.clear();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldExecuteAsync() {
    Map<String, List<String>> logKeys = new HashMap<>();
    String key =
        "accountId:accountId/orgId:orgId/projectId:projectId/pipelineId:pipelineId/runSequence:1/level0:runStepId_1";
    logKeys.put(STEP_ID, Collections.singletonList(key));
    StepLogKeyDetails stepLogKeyDetails = StepLogKeyDetails.builder().logKeys(logKeys).build();
    LiteEnginePodDetailsOutcome liteEnginePodDetailsOutcome =
        LiteEnginePodDetailsOutcome.builder().ipAddress("122.32.433.43").build();

    RefObject refObject1 = RefObjectUtils.getSweepingOutputRefObject(CALLBACK_IDS);
    RefObject refObject2 = RefObjectUtils.getSweepingOutputRefObject(LOG_KEYS);
    RefObject refObject3 = RefObjectUtils.getSweepingOutputRefObject(PORT_DETAILS);
    RefObject refObject4 = RefObjectUtils.getSweepingOutputRefObject(CODE_BASE_CONNECTOR_REF);

    when(executionSweepingOutputResolver.resolveOptional(
             ambiance, RefObjectUtils.getSweepingOutputRefObject(STAGE_INFRA_DETAILS)))
        .thenReturn(OptionalSweepingOutput.builder()
                        .found(true)
                        .output(K8StageInfraDetails.builder()
                                    .podName("podName")
                                    .infrastructure(K8sDirectInfraYaml.builder()
                                                        .spec(K8sDirectInfraYamlSpec.builder()
                                                                  .connectorRef(ParameterField.createValueField("fd"))
                                                                  .build())
                                                        .build())
                                    .containerNames(new ArrayList<>())
                                    .build())
                        .build());
    when(executionSweepingOutputResolver.resolve(eq(ambiance), eq(refObject1))).thenReturn(stepTaskDetails);
    when(executionSweepingOutputResolver.resolve(eq(ambiance), eq(refObject2))).thenReturn(stepLogKeyDetails);
    when(executionSweepingOutputResolver.resolve(eq(ambiance), eq(refObject3))).thenReturn(containerPortDetails);
    when(executionSweepingOutputResolver.resolveOptional(eq(ambiance), eq(refObject4)))
        .thenReturn(OptionalSweepingOutput.builder().found(true).output(codeBaseConnectorRefSweepingOutput).build());
    when(outcomeService.resolve(ambiance, RefObjectUtils.getOutcomeRefObject(POD_DETAILS_OUTCOME)))
        .thenReturn(liteEnginePodDetailsOutcome);
    when(ciExecutionServiceConfig.isLocal()).thenReturn(false);
    when(ciDelegateTaskExecutor.queueTask(any(), any(), any())).thenReturn(callbackId);
    when(runStepProtobufSerializer.serializeStepWithStepParameters(
             any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(UnitStep.newBuilder().build());

    AsyncExecutableResponse asyncExecutableResponse =
        runStep.executeAsync(ambiance, stepElementParameters, stepInputPackage, null);
    assertThat(asyncExecutableResponse)
        .isEqualTo(AsyncExecutableResponse.newBuilder()
                       .addCallbackIds(callbackId)
                       .addCallbackIds(callbackId)
                       .addLogKeys(key)
                       .build());
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void shouldHandleSuccessAsyncResponse() {
    responseDataMap.put(STEP_RESPONSE,
        StepStatusTaskResponseData.builder()
            .stepStatus(StepStatus.builder()
                            .stepExecutionStatus(StepExecutionStatus.SUCCESS)
                            .output(StepMapOutput.builder().output(OUTPUT_KEY, OUTPUT_VALUE).build())
                            .build())
            .build());
    StepResponse stepResponse = runStep.handleAsyncResponse(ambiance, stepElementParameters, responseDataMap);

    assertThat(stepResponse)
        .isEqualTo(
            StepResponse.builder()
                .status(Status.SUCCEEDED)
                .stepOutcome(
                    StepResponse.StepOutcome.builder()
                        .outcome(CIStepOutcome.builder()
                                     .outputVariables(
                                         StepMapOutput.builder().output(OUTPUT_KEY, OUTPUT_VALUE).build().getMap())
                                     .build())
                        .name("outputVariables")
                        .build())
                .build());
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldHandleFailureAsyncResponse() {
    responseDataMap.put(STEP_RESPONSE,
        StepStatusTaskResponseData.builder()
            .stepStatus(StepStatus.builder().stepExecutionStatus(StepExecutionStatus.FAILURE).error(ERROR).build())
            .build());
    when(executionSweepingOutputResolver.resolveOptional(
             ambiance, RefObjectUtils.getSweepingOutputRefObject(STAGE_INFRA_DETAILS)))
        .thenReturn(
            OptionalSweepingOutput.builder()
                .found(true)
                .output(K8StageInfraDetails.builder().podName("podName").containerNames(new ArrayList<>()).build())
                .build());
    StepResponse stepResponse = runStep.handleAsyncResponse(ambiance, stepElementParameters, responseDataMap);

    assertThat(stepResponse)
        .isEqualTo(StepResponse.builder()
                       .status(Status.FAILED)
                       .failureInfo(FailureInfo.newBuilder()
                                        .setErrorMessage(ERROR)
                                        .addAllFailureTypes(EnumSet.of(FailureType.APPLICATION_FAILURE))
                                        .build())
                       .build());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void shouldHandleSkippedAsyncResponse() {
    responseDataMap.put(STEP_RESPONSE,
        StepStatusTaskResponseData.builder()
            .stepStatus(StepStatus.builder().stepExecutionStatus(StepExecutionStatus.SKIPPED).build())
            .build());
    when(executionSweepingOutputResolver.resolveOptional(
             ambiance, RefObjectUtils.getSweepingOutputRefObject(STAGE_INFRA_DETAILS)))
        .thenReturn(
            OptionalSweepingOutput.builder()
                .found(true)
                .output(K8StageInfraDetails.builder().podName("podName").containerNames(new ArrayList<>()).build())
                .build());
    StepResponse stepResponse = runStep.handleAsyncResponse(ambiance, stepElementParameters, responseDataMap);

    assertThat(stepResponse).isEqualTo(StepResponse.builder().status(Status.SKIPPED).build());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void shouldExecuteAsyncVm() {
    Map<String, List<String>> logKeys = new HashMap<>();
    String key =
        "accountId:accountId/orgId:orgId/projectId:projectId/pipelineId:pipelineId/runSequence:1/level0:runStepId_1";
    logKeys.put(STEP_ID, Collections.singletonList(key));

    RefObject refObject = RefObjectUtils.getSweepingOutputRefObject(CODE_BASE_CONNECTOR_REF);

    when(executionSweepingOutputResolver.resolveOptional(
             ambiance, RefObjectUtils.getSweepingOutputRefObject(STAGE_INFRA_DETAILS)))
        .thenReturn(OptionalSweepingOutput.builder().found(true).output(VmStageInfraDetails.builder().build()).build());
    when(executionSweepingOutputResolver.resolveOptional(eq(ambiance), eq(refObject)))
        .thenReturn(OptionalSweepingOutput.builder().found(true).output(codeBaseConnectorRefSweepingOutput).build());
    when(executionSweepingOutputResolver.resolveOptional(
             ambiance, RefObjectUtils.getSweepingOutputRefObject(ContextElement.stageDetails)))
        .thenReturn(OptionalSweepingOutput.builder()
                        .found(true)
                        .output(StageDetails.builder().stageRuntimeID("test").build())
                        .build());
    when(outcomeService.resolveOptional(
             ambiance, RefObjectUtils.getOutcomeRefObject(VmDetailsOutcome.VM_DETAILS_OUTCOME)))
        .thenReturn(OptionalOutcome.builder()
                        .found(true)
                        .outcome(VmDetailsOutcome.builder().ipAddress("1.1.1.1").build())
                        .build());
    when(executionSweepingOutputResolver.resolveOptional(
             ambiance, RefObjectUtils.getSweepingOutputRefObject(STAGE_INFRA_DETAILS)))
        .thenReturn(OptionalSweepingOutput.builder().found(true).output(VmStageInfraDetails.builder().build()).build());

    when(vmStepSerializer.serialize(any(), any(), any(), any())).thenReturn(VmRunStep.builder().build());
    when(ciDelegateTaskExecutor.queueTask(any(), any(), any())).thenReturn(callbackId);

    AsyncExecutableResponse asyncExecutableResponse =
        runStep.executeAsync(ambiance, stepElementParameters, stepInputPackage, null);
    assertThat(asyncExecutableResponse)
        .isEqualTo(AsyncExecutableResponse.newBuilder().addCallbackIds(callbackId).addLogKeys(key).build());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void shouldHandleSuccessVmAsyncResponse() {
    responseDataMap.put(STEP_RESPONSE,
        VmTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build());
    when(executionSweepingOutputResolver.resolveOptional(
             ambiance, RefObjectUtils.getSweepingOutputRefObject(STAGE_INFRA_DETAILS)))
        .thenReturn(OptionalSweepingOutput.builder().found(true).output(VmStageInfraDetails.builder().build()).build());
    StepResponse stepResponse = runStep.handleAsyncResponse(ambiance, stepElementParameters, responseDataMap);

    assertThat(stepResponse).isEqualTo(StepResponse.builder().status(Status.SUCCEEDED).build());
  }
}
