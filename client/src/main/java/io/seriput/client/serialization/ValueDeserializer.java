package io.seriput.client.serialization;

/**
 * Deserializer for values.
 */
interface ValueDeserializer {
  /**
   * Deserializes the given value.
   *
   * @param bytes value to deserialize
   * @param valueType type of the value to deserialize
   * @return deserialized value
   * @throws NullPointerException if given {@code value} is null
   */
  <T> T deserialize(byte[] bytes, Class<T> valueType);

  /**
   * Returns the type ID of the deserializer.
   *
   * @return type ID of the deserializer
   */
  byte typeId();
}
