package io.seriput.client.exception;

import java.util.Objects;
import lombok.Getter;
import lombok.experimental.Accessors;

/** Exception thrown when a requested key is not found in the Seriput key-value store. */
@Accessors(fluent = true)
public final class NotFoundException extends SeriputClientException {
  @Getter private final Integer errorCode;

  public NotFoundException(String message, Integer errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  NotFoundException(String message, Throwable cause, Integer errorCode) {
    super(message, cause);
    this.errorCode = errorCode;
  }

  @Override
  public boolean isRetryable() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof NotFoundException that)) return false;
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
    return "NotFoundException("
        + "message="
        + getMessage()
        + ", cause="
        + getCause()
        + ", errorCode="
        + errorCode
        + ')';
  }
}
