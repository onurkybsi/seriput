package io.seriput.server.fixture;

import io.seriput.common.serialization.KeyType;
import io.seriput.common.serialization.RequestSerializer;
import io.seriput.common.serialization.ValueType;

public final class RequestFixtures {
  private static final RequestSerializer<String, Object> requestSerializer = RequestSerializer.build(KeyType.UTF8, ValueType.JSON_UTF8);

  private RequestFixtures() {}

  public static byte[] serializeGet(String key) {
    var buffer = requestSerializer.serializeGet(key);
    var bufferArray = new byte[buffer.remaining()];
    buffer.get(bufferArray);
    return bufferArray;
  }

  public static byte[] serializePut(String key, Object value) {
    var buffer = requestSerializer.serializePut(key, value);
    var bufferArray = new byte[buffer.remaining()];
    buffer.get(bufferArray);
    return bufferArray;
  }

  public static byte[] serializeDelete(String key) {
    var buffer = requestSerializer.serializeDelete(key);
    var bufferArray = new byte[buffer.remaining()];
    buffer.get(bufferArray);
    return bufferArray;
  }
}
