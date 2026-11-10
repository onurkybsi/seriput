package io.seriput.benchmark;

import io.seriput.common.HeapByteBufferAllocator;
import io.seriput.common.serialization.request.KeyType;
import io.seriput.common.serialization.request.RequestSerializer;
import io.seriput.common.serialization.request.ValueType;
import java.nio.ByteBuffer;

final class ProtocolFrames {
  private static final RequestSerializer<String, String> SERIALIZER =
      RequestSerializer.build(KeyType.UTF8, ValueType.JSON_UTF8, new HeapByteBufferAllocator());

  private ProtocolFrames() {}

  static byte[] get(String key) {
    return bytes(SERIALIZER.serializeGet(key));
  }

  static byte[] put(String key, String value) {
    return bytes(SERIALIZER.serializePut(key, value));
  }

  private static byte[] bytes(ByteBuffer buffer) {
    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    return bytes;
  }
}
