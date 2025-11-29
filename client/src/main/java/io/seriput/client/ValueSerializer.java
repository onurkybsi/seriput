package io.seriput.client;

/**
 * Serializer for values.
 */
sealed interface ValueSerializer<T> permits JsonUtf8ValueSerializer {
  /**
   * Serializes the given value.
   *
   * @param value value to serialize
   * @return serialized value
   * @throws NullPointerException if given {@code value} is null
   */
  byte[] serialize(T value);

  /**
   * Returns the type ID of the serializer.
   *
   * @return type ID of the serializer
   */
  byte typeId();
}
