package io.seriput.common.serialization;

/**
 * Represents the value types supported by the current protocol.
 */
public enum ValueType {
  JSON_UTF8((byte) 0x01);

  private final byte typeId;

  ValueType(byte typeId) {
    this.typeId = typeId;
  }

  public byte typeId() {
    return typeId;
  }
}
