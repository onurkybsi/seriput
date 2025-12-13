package io.seriput.server.core;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a value stored in the key-value store.
 *
 * @param type type of the value
 * @param bytes raw bytes of the value
 */
public record Value(ValueType type, byte[] bytes) {
  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Value value = (Value) o;
    return type == value.type && Objects.deepEquals(bytes, value.bytes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, Arrays.hashCode(bytes));
  }
}
