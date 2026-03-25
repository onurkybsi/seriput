package io.seriput.server.fixture;

import io.seriput.common.HeapByteBufferAllocator;
import io.seriput.common.serialization.request.KeyType;
import io.seriput.common.serialization.request.RequestSerializer;
import io.seriput.common.serialization.request.ValueType;
import java.util.UUID;

public final class RequestFixtures {
  private static final RequestSerializer<String, Object> requestSerializer =
      RequestSerializer.build(KeyType.UTF8, ValueType.JSON_UTF8, new HeapByteBufferAllocator());
  public static final String TEST_KEY = "user:";
  public static final Object testValue =
      new Object() {
        public final String name = "John Doe";
        public final int age = 30;
      };
  public static final byte[] testPutRequestPayload =
      RequestFixtures.serializePut(TEST_KEY, testValue);
  public static final byte[] testGetRequestPayload = RequestFixtures.serializeGet(TEST_KEY);
  public static final byte[] testDeleteRequestPayload = RequestFixtures.serializeDelete(TEST_KEY);

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

  public static String testKey() {
    return "user:" + UUID.randomUUID();
  }

  public static byte[] testPutRequestPayload(String key) {
    return RequestFixtures.serializePut(key, testValue);
  }
}
