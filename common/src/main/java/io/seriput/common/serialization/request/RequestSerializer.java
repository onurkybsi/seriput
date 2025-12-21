package io.seriput.common.serialization.request;

import java.nio.ByteBuffer;

/**
 * Request serializer for Seriput protocol v1.
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class RequestSerializer<K, V> {
  private static final int HEADER_SIZE = 1 + 1 + 1 + 4 + 4; // op + keyTypeId + valueTypeId + keyLength + valueLength

  private final KeySerializer<K> keySerializer;
  private final ValueSerializer<V> valueSerializer;

  private RequestSerializer(KeySerializer<K> keySerializer, ValueSerializer<V> valueSerializer) {
    this.keySerializer = keySerializer;
    this.valueSerializer = valueSerializer;
  }

  /**
   * Builds a {@code RequestSerializer} by given {@code keyType} and {@code valueType}.
   *
   * @param keyType key type to serialize
   * @param valueType value type to serialize
   * @return built {@code RequestSerializer} instance
   */
  @SuppressWarnings("unchecked")
  public static <K, V> RequestSerializer<K, V> build(KeyType keyType, ValueType valueType) {
    KeySerializer<K> keySerializer = (KeySerializer<K>) switch (keyType) {
      case UTF8 -> new Utf8StringKeySerializer();
    };
    ValueSerializer<V> valueSerializer = switch (valueType) {
      case JSON_UTF8 -> new JsonUtf8ValueSerializer<>();
    };
    return new RequestSerializer<>(keySerializer, valueSerializer);
  }

  /**
   * Serializes a {@code GET} request by the given key.
   *
   * @param key key to serialize
   * @return serialized {@code GET} request in read-only {@code ByteBuffer}
   */
  public ByteBuffer serializeGet(K key) {
    byte[] serializedKey = keySerializer.serialize(key);
    ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + serializedKey.length);
    buffer.put(RequestOp.GET.op());
    buffer.put(keySerializer.typeId());
    buffer.put(valueSerializer.typeId());
    buffer.putInt(serializedKey.length);
    buffer.putInt(0); // No value for GET
    buffer.put(serializedKey);
    buffer.flip(); // Switch to read mode
    return buffer.asReadOnlyBuffer();
  }

  /**
   * Serializes a {@code PUT} request by the given key and value.
   *
   * @param key key to serialize
   * @param value value to serialize
   * @return serialized {@code PUT} request in read-only {@code ByteBuffer}
   */
  public ByteBuffer serializePut(K key, V value) {
    byte[] serializedKey = keySerializer.serialize(key);
    byte[] serializedValue = valueSerializer.serialize(value);
    ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + serializedKey.length + serializedValue.length);
    buffer.put(RequestOp.PUT.op());
    buffer.put(keySerializer.typeId());
    buffer.put(valueSerializer.typeId());
    buffer.putInt(serializedKey.length);
    buffer.putInt(serializedValue.length);
    buffer.put(serializedKey);
    buffer.put(serializedValue);
    buffer.flip(); // Switch to read mode
    return buffer.asReadOnlyBuffer();
  }

  /**
   * Serializes a {@code DELETE} request by the given key.
   *
   * @param key key to serialize
   * @return serialized {@code DELETE} request in read-only {@code ByteBuffer}
   */
  public ByteBuffer serializeDelete(K key) {
    byte[] serializedKey = keySerializer.serialize(key);

    ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + serializedKey.length);
    buffer.put(RequestOp.DELETE.op());
    buffer.put(keySerializer.typeId());
    buffer.put(valueSerializer.typeId());
    buffer.putInt(serializedKey.length);
    buffer.putInt(0); // No value for DELETE
    buffer.put(serializedKey);
    buffer.flip(); // Switch to read mode
    return buffer.asReadOnlyBuffer();
  }
}
