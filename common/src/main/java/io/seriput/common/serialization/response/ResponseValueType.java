package io.seriput.common.serialization.response;

/**
 * Represents the value types in Seriput protocol v1.
 */
public enum ResponseValueType {
  VOID((byte) 0x00),
  JSON_UTF8((byte) 0x01);

  private final byte typeId;

  ResponseValueType(byte typeId) {
    this.typeId = typeId;
  }

  public static ResponseValueType fromByte(byte typeId) {
    for (ResponseValueType valueType : values()) {
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
