/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.states;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTaskRequest;
import io.harness.callback.DelegateCallbackToken;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.HDelegateTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.grpc.DelegateServiceGrpcClient;

import com.google.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@Slf4j
@OwnedBy(HarnessTeam.CI)
public class CIDelegateTaskExecutor {
  private final DelegateServiceGrpcClient delegateServiceGrpcClient;
  private final Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;
  private final Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(2);
  private final int MAX_ATTEMPTS = 3;
  @Inject
  public CIDelegateTaskExecutor(DelegateServiceGrpcClient delegateServiceGrpcClient,
      Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier) {
    this.delegateServiceGrpcClient = delegateServiceGrpcClient;
    this.delegateCallbackTokenSupplier = delegateCallbackTokenSupplier;
  }

  public String queueTask(Map<String, String> setupAbstractions, HDelegateTask task, List<String> taskSelectorList) {
    String accountId = task.getAccountId();
    TaskData taskData = task.getData();
    final DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                        .parked(taskData.isParked())
                                                        .accountId(accountId)
                                                        .taskSelectors(taskSelectorList)
                                                        .taskType(taskData.getTaskType())
                                                        .taskParameters(extractTaskParameters(taskData))
                                                        .executionTimeout(Duration.ofHours(12))
                                                        .taskSetupAbstractions(setupAbstractions)
                                                        .expressionFunctorToken(taskData.getExpressionFunctorToken())
                                                        .build();
    RetryPolicy<Object> retryPolicy =
        getRetryPolicy(format("[Retrying failed call to submit delegate task attempt: {}"),
            format("Failed to submit delegate task  after retrying {} times"));
    // Make a call to the log service and get back the token

    return Failsafe.with(retryPolicy).get(() -> {
      return delegateServiceGrpcClient.submitAsyncTask(
          delegateTaskRequest, delegateCallbackTokenSupplier.get(), Duration.ZERO);
    });
  }

  public void expireTask(Map<String, String> setupAbstractions, String taskId) {
    // Needs to be implemented
  }

  public boolean abortTask(Map<String, String> setupAbstractions, String taskId) {
    // Needs to be implemented
    return false;
  }

  private TaskParameters extractTaskParameters(TaskData taskData) {
    if (taskData == null || EmptyPredicate.isEmpty(taskData.getParameters())) {
      return null;
    }
    if (taskData.getParameters()[0] instanceof TaskParameters) {
      return (TaskParameters) taskData.getParameters()[0];
    }
    throw new InvalidRequestException("Task Execution not supported for type");
  }

  private RetryPolicy<Object> getRetryPolicy(String failedAttemptMessage, String failureMessage) {
    return new RetryPolicy<>()
        .handle(Exception.class)
        .withDelay(RETRY_SLEEP_DURATION)
        .withMaxAttempts(MAX_ATTEMPTS)
        .onFailedAttempt(event -> log.info(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }
}
