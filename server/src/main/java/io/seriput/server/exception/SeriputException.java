package io.seriput.server.exception;

/**
 * Base class for all Seriput server exceptions.
 */
public sealed abstract class SeriputException extends RuntimeException permits ProtocolViolationException {
  protected SeriputException(String message) {
    super(message);
  }

  protected SeriputException(Throwable cause) {
    super(cause);
  }

  protected SeriputException(String message, Throwable cause) {
    super(message, cause);
  }
}
