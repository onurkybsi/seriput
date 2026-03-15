package io.seriput.client.exception;

import io.seriput.common.serialization.response.ErrorResponse;
import io.seriput.common.serialization.response.ErrorResponsePayload;

import java.util.Optional;

/**
 * Base exception class for failures occurring in {@code SeriputClient} operations.
 */
public abstract sealed class SeriputClientException extends RuntimeException permits InternalErrorException, InvalidRequestException, NotFoundException, ConnectionClosedException {
  protected SeriputClientException(String message) {
    super(message);
  }

  protected SeriputClientException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Indicates whether the operation that caused this exception can be retried.
   *
   * @return {@code true} if the operation can be retried; {@code false} otherwise
   */
  public abstract boolean isRetryable();

  public static SeriputClientException from(ErrorResponse errorResponse) {
    String message = Optional.ofNullable(errorResponse.errorPayload()).map(ErrorResponsePayload::errorMessage).orElse(null);
    Integer errorCode = Optional.ofNullable(errorResponse.errorPayload()).map(ErrorResponsePayload::errorCode).orElse(null);
    return switch (errorResponse.responseStatus()) {
      case OK -> throw new IllegalArgumentException("responseStatus may not be OK!");
      case INVALID_REQUEST -> new InvalidRequestException(message, errorCode);
      case NOT_FOUND -> new NotFoundException(message, errorCode);
      case INTERNAL_ERROR -> new InternalErrorException(message, errorCode);
    };
  }
}
