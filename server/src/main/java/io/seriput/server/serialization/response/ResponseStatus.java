package io.seriput.server.serialization.response;

/**
 * Represents the status of a response sent by Seriput server.
 */
public enum ResponseStatus {
  OK((byte) 0x00),
  INVALID_REQUEST((byte) 0x01),
  NOT_FOUND((byte) 0x02),
  INTERNAL_ERROR((byte) 0x03);

  private final byte status;

  ResponseStatus(byte status) {
    this.status = status;
  }

  byte status() {
    return status;
  }
}
