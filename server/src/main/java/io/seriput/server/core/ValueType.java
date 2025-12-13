package io.seriput.server.core;

/**
 * Represents the value types supported by the current protocol.
 */
public enum ValueType {
  JSON_UTF8((byte) 0x01);

  private final byte typeId;

  ValueType(byte typeId) {
    this.typeId = typeId;
  }

  public static ValueType fromByte(byte typeId) {
    for (ValueType valueType : values()) {
      if (valueType.typeId == typeId) {
        return valueType;
      }
    }
    return null;
  }

  public byte typeId() {
    return typeId;
  }
}
