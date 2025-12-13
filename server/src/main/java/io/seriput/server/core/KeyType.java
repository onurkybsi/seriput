package io.seriput.server.core;

/**
 * Represents the key types supported by the current protocol.
 */
public enum KeyType {
  UTF8((byte) 0x01);

  private final byte typeId;

  KeyType(byte typeId) {
    this.typeId = typeId;
  }

  public static KeyType fromByte(byte typeId) {
    for (KeyType keyType : values()) {
      if (keyType.typeId == typeId) {
        return keyType;
      }
    }
    return null;
  }

  public byte typeId() {
    return typeId;
  }
}
