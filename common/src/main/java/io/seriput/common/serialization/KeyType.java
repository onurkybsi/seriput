package io.seriput.common.serialization;

/**
 * Represents the key types supported by the current protocol.
 */
public enum KeyType {
  UTF8((byte) 0x01);

  private final byte typeId;

  KeyType(byte typeId) {
    this.typeId = typeId;
  }

  public byte typeId() {
    return typeId;
  }
}
