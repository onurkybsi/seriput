package io.seriput.server.exception;

/**
 * Thrown to indicate that there was a protocol violation.
 */
public final class ProtocolViolationException extends SeriputException {
  public ProtocolViolationException(String message) {
    super(message);
  }
}
