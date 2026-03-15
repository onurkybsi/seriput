package io.seriput.client.exception;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * Exception thrown when a request to the Seriput key-value store is invalid.
 */
@Accessors(fluent = true)
public final class InvalidRequestException extends SeriputClientException {
  @Getter
  private final Integer errorCode;

  InvalidRequestException(String message, Integer errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  InvalidRequestException(String message, Throwable cause, Integer errorCode) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  @Override
  public boolean isRetryable() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof InvalidRequestException that))
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
    return "InvalidRequestException(" + "message=" + getMessage() + ", cause=" + getCause() +
        ", errorCode=" + errorCode + ')';
  }
}
