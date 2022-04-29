/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.CommandExecutionStatus;

import java.util.EnumSet;
import java.util.Set;

/**
 * Describes possible execution statuses for a state.
 */
@OwnedBy(CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
public enum ExecutionStatus {
  ABORTED,
  DISCONTINUING,
  ERROR,
  FAILED,
  NEW,
  PAUSED,
  PAUSING,
  QUEUED,
  RESUMED,
  RUNNING,
  SCHEDULED,
  STARTING,
  SUCCESS,
  WAITING,
  SKIPPED,
  @Deprecated ABORTING,
  REJECTED,
  EXPIRED,
  PREPARING;

  private static Set<ExecutionStatus> finalStatuses =
      EnumSet.<ExecutionStatus>of(ABORTED, ERROR, FAILED, SUCCESS, REJECTED, EXPIRED, SKIPPED);
  private static Set<ExecutionStatus> brokeStatuses = EnumSet.<ExecutionStatus>of(ERROR, FAILED);
  private static Set<ExecutionStatus> negativeStatuses =
      EnumSet.<ExecutionStatus>of(ABORTED, DISCONTINUING, ERROR, FAILED, REJECTED, EXPIRED);
  private static Set<ExecutionStatus> runningStatuses =
      EnumSet.<ExecutionStatus>of(DISCONTINUING, NEW, RUNNING, STARTING, QUEUED, PREPARING);
  private static Set<ExecutionStatus> haltedStatuses = EnumSet.<ExecutionStatus>of(PAUSED, WAITING);
  private static Set<ExecutionStatus> activeStatuses =
      EnumSet.<ExecutionStatus>of(DISCONTINUING, NEW, PAUSED, RUNNING, STARTING, QUEUED, WAITING, PREPARING);
  private static Set<ExecutionStatus> positiveStatuses = EnumSet.<ExecutionStatus>of(SUCCESS, SKIPPED);
  private static Set<ExecutionStatus> discontinueStatuses = EnumSet.<ExecutionStatus>of(ABORTED, REJECTED, EXPIRED);
  private static Set<ExecutionStatus> flowingStatuses =
      EnumSet.<ExecutionStatus>of(DISCONTINUING, NEW, PAUSED, RUNNING, STARTING, WAITING, PREPARING);
  private static Set<ExecutionStatus> persistedStatuses =
      EnumSet.<ExecutionStatus>of(ABORTED, ERROR, FAILED, SUCCESS, REJECTED, EXPIRED, SKIPPED, RUNNING, PAUSED);
  private static Set<ExecutionStatus> persistedActiveStatuses = EnumSet.<ExecutionStatus>of(RUNNING, PAUSED);
  public static Set<ExecutionStatus> resumableStatuses =
      EnumSet.<ExecutionStatus>of(FAILED, ABORTED, REJECTED, EXPIRED, ERROR);
  ExecutionStatus() {}

  public static Set<ExecutionStatus> activeStatuses() {
    return activeStatuses;
  }

  public static boolean isActiveStatus(ExecutionStatus status) {
    return status != null && activeStatuses.contains(status);
  }
  public static Set<ExecutionStatus> finalStatuses() {
    return finalStatuses;
  }

  public static Set<ExecutionStatus> persistedStatuses() {
    return persistedStatuses;
  }

  public static Set<ExecutionStatus> persistedActiveStatuses() {
    return persistedActiveStatuses;
  }

  public static boolean isFinalStatus(ExecutionStatus status) {
    return status != null && finalStatuses.contains(status);
  }

  public static boolean isNotFinalStatus(ExecutionStatus status) {
    return !isFinalStatus(status);
  }

  public static Set<ExecutionStatus> brokeStatuses() {
    return brokeStatuses;
  }

  public static boolean isBrokeStatus(ExecutionStatus status) {
    return status != null && brokeStatuses.contains(status);
  }

  public static Set<ExecutionStatus> negativeStatuses() {
    return negativeStatuses;
  }

  public static Set<ExecutionStatus> flowingStatuses() {
    return flowingStatuses;
  }

  public static boolean isNegativeStatus(ExecutionStatus status) {
    return status != null && negativeStatuses.contains(status);
  }

  public static Set<ExecutionStatus> runningStatuses() {
    return runningStatuses;
  }

  public static boolean isRunningStatus(ExecutionStatus status) {
    return status != null && runningStatuses.contains(status);
  }

  public static boolean isHaltedStatus(ExecutionStatus status) {
    return status != null && haltedStatuses.contains(status);
  }

  public static boolean isDiscontinueStatus(ExecutionStatus status) {
    return status != null && discontinueStatuses.contains(status);
  }

  public static boolean isPositiveStatus(ExecutionStatus status) {
    return status != null && positiveStatuses.contains(status);
  }

  public static CommandExecutionStatus translateExecutionStatus(ExecutionStatus executionStatus) {
    switch (executionStatus) {
      case SUCCESS:
        return CommandExecutionStatus.SUCCESS;
      case FAILED:
        return CommandExecutionStatus.FAILURE;
      case RUNNING:
        return CommandExecutionStatus.RUNNING;
      case QUEUED:
        return CommandExecutionStatus.QUEUED;
      default:
        throw new IllegalArgumentException("invalid status: " + executionStatus);
    }
  }

  public static ExecutionStatus translateCommandExecutionStatus(CommandExecutionStatus commandExecutionStatus) {
    switch (commandExecutionStatus) {
      case SUCCESS:
        return ExecutionStatus.SUCCESS;
      case FAILURE:
        return ExecutionStatus.FAILED;
      case RUNNING:
        return ExecutionStatus.RUNNING;
      case QUEUED:
        return ExecutionStatus.QUEUED;
      default:
        throw new IllegalArgumentException("invalid status: " + commandExecutionStatus);
    }
  }

  public static ExecutionStatusCategory getStatusCategory(ExecutionStatus status) {
    if (ExecutionStatus.isPositiveStatus(status)) {
      return ExecutionStatusCategory.SUCCEEDED;
    } else if (ExecutionStatus.isNegativeStatus(status)) {
      return ExecutionStatusCategory.ERROR;
    } else {
      return ExecutionStatusCategory.ACTIVE;
    }
  }
}
