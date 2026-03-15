package io.seriput.client.exception;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * Exception thrown when an internal server error occurs in the Seriput
 * key-value store.
 */
@Accessors(fluent = true)
public final class InternalErrorException extends SeriputClientException {
  @Getter
  private final Integer errorCode;

  InternalErrorException(String message, Integer errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  InternalErrorException(String message, Throwable cause, Integer errorCode) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  @Override
  public boolean isRetryable() {
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof InternalErrorException that))
      return false;
    return Objects.equals(errorCode, that.errorCode)
        && Objects.equals(getMessage(), that.getMessage())
        && Objects.equals(getCause(), that.getCause());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getMessage(), getCause(), errorCode);
  }

  @Override
  public String toString() {
    return "InternalErrorException(" + "message=" + getMessage() + ", cause=" + getCause() +
        ", errorCode=" + errorCode + ')';
  }
}
