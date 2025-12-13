package io.seriput.server.core;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a key stored in the key-value store.
 *
 * @param type type of the key
 * @param bytes raw bytes of the key
 */
public record Key(KeyType type, byte[] bytes) {
  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Key key = (Key) o;
    return type == key.type && Objects.deepEquals(bytes, key.bytes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, Arrays.hashCode(bytes));
  }
}
