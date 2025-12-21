package io.seriput.common.serialization.response;

/**
 * Response status codes in Seriput protocol v1.
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

  static ResponseStatus fromByte(byte statusByte) {
    for (ResponseStatus status : ResponseStatus.values()) {
      if (status.status == statusByte) {
        return status;
      }
    }
    return null;
  }

  static boolean isErrorStatus(ResponseStatus status) {
    return !OK.equals(status);
  }

  byte status() {
    return this.status;
  }
}
