/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.interrupts;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.PersistentLockException;
import io.harness.interrupts.Interrupt;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.logging.AutoLogContext;

import com.google.inject.Inject;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@OwnedBy(PIPELINE)
@Slf4j
public class InterruptManager {
  private static final String LOCK_NAME_PREFIX = "PLAN_EXECUTION_INFO_";
  @Inject private InterruptHandlerFactory interruptHandlerFactory;
  @Inject PersistentLocker persistentLocker;
  private static final int MAX_ATTEMPTS = 3;
  private static final long INITIAL_DELAY_MS = 100;
  private static final long MAX_DELAY_MS = 5000;
  private static final long DELAY_FACTOR = 5;
  private static final RetryPolicy<Object> RETRY_POLICY = createRetryPolicy();

  public Interrupt register(InterruptPackage interruptPackage) {
    Interrupt interrupt = Interrupt.builder()
                              .uuid(generateUuid())
                              .planExecutionId(interruptPackage.getPlanExecutionId())
                              .type(interruptPackage.getInterruptType())
                              .metadata(interruptPackage.getMetadata())
                              .nodeExecutionId(interruptPackage.getNodeExecutionId())
                              .interruptConfig(interruptPackage.getInterruptConfig())
                              .build();

    // On high load there is a high chance of getting persistent lock exception
    return Failsafe.with(RETRY_POLICY).get(() -> {
      String lockKey = LOCK_NAME_PREFIX + interruptPackage.getPlanExecutionId();
      try (AcquiredLock<?> lock =
               persistentLocker.waitToAcquireLock(lockKey, Duration.ofSeconds(15), Duration.ofMinutes(1));
           AutoLogContext ignore = interrupt.autoLogContext()) {
        if (lock == null) {
          throw new InvalidRequestException("Cannot register the interrupt. Please retry.");
        }
        InterruptHandler interruptHandler = interruptHandlerFactory.obtainHandler(interruptPackage.getInterruptType());
        Interrupt registeredInterrupt = interruptHandler.registerInterrupt(interrupt);
        log.info(
            "Interrupt Registered uuid: {}, type: {}", registeredInterrupt.getUuid(), registeredInterrupt.getType());
        return registeredInterrupt;
      }
    });
  }

  private static RetryPolicy<Object> createRetryPolicy() {
    return new RetryPolicy<>()
        .withBackoff(INITIAL_DELAY_MS, MAX_DELAY_MS, ChronoUnit.MILLIS, DELAY_FACTOR)
        .withMaxAttempts(MAX_ATTEMPTS)
        .onFailedAttempt(event
            -> log.warn(String.format("Got persistentLockException, retrying: %d", event.getAttemptCount()),
                event.getLastFailure()))
        .onFailure(event
            -> log.error(String.format("Got persistentLockException after attempts: %d", event.getAttemptCount()),
                event.getFailure()))
        .handleIf(throwable -> throwable instanceof PersistentLockException);
  }
}
