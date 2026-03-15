package io.seriput.client.exception;

import java.util.Objects;

/**
 * Exception thrown when an operation could not be completed because the TCP
 * connection is closed.
 */
public final class ConnectionClosedException extends SeriputClientException {
  public ConnectionClosedException(String message) {
    super(message);
  }

  @Override
  public boolean isRetryable() {
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof ConnectionClosedException that))
      return false;
    return Objects.equals(getMessage(), that.getMessage())
        && Objects.equals(getCause(), that.getCause());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getMessage(), getCause());
  }

  @Override
  public String toString() {
    return "ConnectionClosedException(" + "message=" + getMessage() + ", cause=" + getCause() + ')';
  }
}
