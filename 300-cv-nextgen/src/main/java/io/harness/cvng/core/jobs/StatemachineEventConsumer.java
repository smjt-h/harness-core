/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.jobs;

import static io.harness.eventsframework.EventsFrameworkConstants.SRM_STATEMACHINE_EVENT;

import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine;
import io.harness.cvng.statemachine.services.api.AnalysisStateMachineService;
import io.harness.cvng.statemachine.services.api.OrchestrationService;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.consumer.Message;
import io.harness.eventsframework.schemas.cv.StateMachineTrigger;
import io.harness.exception.InvalidRequestException;
import io.harness.queue.QueueController;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StatemachineEventConsumer extends AbstractStreamConsumer {
  private static final int MAX_WAIT_TIME_SEC = 10;
  AnalysisStateMachineService stateMachineService;

  @Inject private OrchestrationService orchestrationService;
  @Inject private StateMachineEventPublisherService stateMachineEventPublisherService;

  @Inject
  public StatemachineEventConsumer(@Named(SRM_STATEMACHINE_EVENT) Consumer consumer, QueueController queueController,
      AnalysisStateMachineService stateMachineService) {
    super(MAX_WAIT_TIME_SEC, consumer, queueController);
    this.stateMachineService = stateMachineService;
  }

  @Override
  protected void processMessage(Message message) {
    StateMachineTrigger trigger = null;
    try {
      trigger = StateMachineTrigger.parseFrom(message.getMessage().getData());
      stateMachineService.executeStateMachine(trigger.getVerificationTaskId());
      processAnalysisStateMachine(trigger.getVerificationTaskId());
    } catch (Exception ex) {
      if (Objects.nonNull(trigger)) {
        processFailureMessage(trigger);
      } else {
        throw new InvalidRequestException("Invalid message for srm_statemachine_event topic  " + message);
      }
    }
  }

  private void processAnalysisStateMachine(String verificationTaskId) {
    AnalysisStateMachine currentlyExecutingStateMachine =
        stateMachineService.getExecutingStateMachine(verificationTaskId);
    if (currentlyExecutingStateMachine == null) {
      return;
    }
    AnalysisStatus stateMachineStatus = null;

    switch (currentlyExecutingStateMachine.getStatus()) {
      case CREATED:
      case SUCCESS:
      case IGNORED:
        orchestrationService.orchestrateNewAnalysisStateMachine(verificationTaskId);
        break;
      case RUNNING:
        stateMachineStatus = stateMachineService.executeStateMachine(currentlyExecutingStateMachine);
        break;
      case FAILED:
      case TIMEOUT:
        stateMachineService.retryStateMachineAfterFailure(currentlyExecutingStateMachine);
        break;
      case COMPLETED:
        log.info("Analysis for the entire duration is done. Time to close down");
        orchestrationService.updateStatusOfOrchestrator(verificationTaskId, AnalysisStatus.COMPLETED);
        break;
      default:
        log.info("Unknown analysis status of the state machine under execution");
    }

    if (AnalysisStatus.SUCCESS == stateMachineStatus || AnalysisStatus.COMPLETED == stateMachineStatus) {
      // Queue the next job in topic
      AnalysisStateMachine analysisStateMachine = orchestrationService.getFrontOfStateMachineQueue(verificationTaskId);
      stateMachineEventPublisherService.registerTaskComplete(
          analysisStateMachine.getAccountId(), analysisStateMachine.getVerificationTaskId());
    }
  }

  private void processFailureMessage(StateMachineTrigger trigger) {
    // Add the same message again for retry
    stateMachineEventPublisherService.registerTaskComplete(trigger.getAccountId(), trigger.getVerificationTaskId());
  }
}
