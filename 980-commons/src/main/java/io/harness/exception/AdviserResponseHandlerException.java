package io.harness.exception;

import static io.harness.eraro.ErrorCode.FAILED_ADVISER_RESPONSE;

import io.harness.eraro.Level;
import io.harness.exception.ngexception.ErrorMetadataDTO;

public class AdviserResponseHandlerException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public AdviserResponseHandlerException(String message, ErrorMetadataDTO metadata) {
    super(message, null, FAILED_ADVISER_RESPONSE, Level.ERROR, null, null, metadata);
    super.param(MESSAGE_ARG, message);
  }

  public AdviserResponseHandlerException(String message, Throwable cause) {
    super(message, cause, FAILED_ADVISER_RESPONSE, Level.ERROR, null, null);
    if (message != null) {
      super.param(MESSAGE_ARG, message);
    }
  }
}