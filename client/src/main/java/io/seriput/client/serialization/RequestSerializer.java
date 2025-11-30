package io.seriput.client.serialization;

import java.nio.ByteBuffer;

// TODO: Add JavaDoc
public final class RequestSerializer<V> {
  private static final int HEADER_SIZE = 1 + 1 + 1 + 4 + 4; // op + keyTypeId + valueTypeId + keyLength + valueLength

  private final KeySerializer<String> keySerializer = new Utf8StringKeySerializer();
  private final ValueSerializer<V> valueSerializer = new JsonUtf8ValueSerializer<>();

  public ByteBuffer serializeGet(String key) {
    byte[] serializedKey = keySerializer.serialize(key);

    ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + serializedKey.length);
    buffer.put(RequestOp.GET.op());
    buffer.put(keySerializer.typeId());
    buffer.put(valueSerializer.typeId());
    buffer.putInt(serializedKey.length);
    buffer.putInt(0); // No value for GET
    buffer.put(serializedKey);
    buffer.flip();
    return buffer;
  }

  public ByteBuffer serializePut(String key, V value) {
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
    buffer.flip();
    return buffer;
  }

  public ByteBuffer serializeDelete(String key) {
    byte[] serializedKey = keySerializer.serialize(key);

    ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + serializedKey.length);
    buffer.put(RequestOp.DELETE.op());
    buffer.put(keySerializer.typeId());
    buffer.put(valueSerializer.typeId());
    buffer.putInt(serializedKey.length);
    buffer.putInt(0); // No value for DELETE
    buffer.put(serializedKey);
    buffer.flip();
    return buffer;
  }
}
