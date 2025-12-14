package io.seriput.server.serialization.response;

import io.seriput.server.core.ValueType;

/**
 * Represents the type of response value sent by Seriput server.
 */
public enum ResponseValueType {
  VOID((byte) 0x00),
  JSON_UTF8((byte) 0x01);

  private final byte typeId;

  ResponseValueType(byte typeId) {
    this.typeId = typeId;
  }

  public static ResponseValueType from(ValueType type) {
    return switch (type) {
      case JSON_UTF8 -> JSON_UTF8;
    };
  }

  public byte typeId() {
    return typeId;
  }
}
