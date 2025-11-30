package io.seriput.client.serialization;

/**
 * Serializer for keys.
 */
sealed interface KeySerializer<T> permits Utf8StringKeySerializer {
  /**
   * Serializes the given key.
   *
   * @param key key to serialize
   * @return serialized key
   * @throws NullPointerException if given {@code key} is null
   */
  byte[] serialize(T key);

  /**
   * Returns the type ID of the serializer.
   *
   * @return type ID of the serializer
   */
  byte typeId();
}
