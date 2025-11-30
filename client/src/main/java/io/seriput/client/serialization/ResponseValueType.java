package io.seriput.client.serialization;

enum ResponseValueType {
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
    throw new IllegalArgumentException("Unknown type ID: " + typeId);
  }

  public byte typeId() {
    return typeId;
  }
}
