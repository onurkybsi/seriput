package io.seriput.client.fixture;

import io.seriput.common.ObjectMapperProvider;
import io.seriput.common.serialization.response.ResponseStatus;
import io.seriput.common.serialization.response.ResponseValueType;
import java.nio.ByteBuffer;

public final class ResponseFixtures {

  private ResponseFixtures() {}

  public static byte[] okVoid() {
    return frame(ResponseStatus.OK, ResponseValueType.VOID, new byte[0]);
  }

  public static byte[] okJson(Object value) {
    try {
      byte[] jsonBytes = ObjectMapperProvider.getInstance().writeValueAsBytes(value);
      return frame(ResponseStatus.OK, ResponseValueType.JSON_UTF8, jsonBytes);
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize value to JSON", e);
    }
  }

  public static byte[] notFoundVoid() {
    return frame(ResponseStatus.NOT_FOUND, ResponseValueType.VOID, new byte[0]);
  }

  public static byte[] invalidRequestVoid() {
    return frame(ResponseStatus.INVALID_REQUEST, ResponseValueType.VOID, new byte[0]);
  }

  public static byte[] internalErrorVoid() {
    return frame(ResponseStatus.INTERNAL_ERROR, ResponseValueType.VOID, new byte[0]);
  }

  private static byte[] frame(
      ResponseStatus status, ResponseValueType valueType, byte[] valueBytes) {
    ByteBuffer buffer = ByteBuffer.allocate(1 + 1 + 4 + valueBytes.length);
    buffer.put(status.status());
    buffer.put(valueType.typeId());
    buffer.putInt(valueBytes.length);
    buffer.put(valueBytes);
    return buffer.array();
  }
}
