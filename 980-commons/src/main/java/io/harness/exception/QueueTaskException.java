package io.harness.exception;

import static io.harness.eraro.ErrorCode.QUEUE_TASK_EXCEPTION;

import io.harness.eraro.Level;

public class QueueTaskException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public QueueTaskException(String message) {
    super(message, null, QUEUE_TASK_EXCEPTION, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
