package io.seriput.client.serialization;

import java.nio.ByteBuffer;

// TODO: Add JavaDoc
public final class ResponseDeserializer {
  private static final int HEADER_SIZE = 1 + 1 + 4; // status + valueTypeId + valueLength

  private final ValueDeserializer valueDeserializer = new JsonUtf8ValueDeserializer();

  public Response deserialize(ByteBuffer buffer, Class<?> valueType) {
    buffer.flip(); // Switch to reading mode
    ResponseStatus status = ResponseStatus.fromByte(buffer.get());
    ResponseValueType responseValueType = ResponseValueType.fromByte(buffer.get());
    int valueLength = buffer.getInt();
    byte[] valueBytes = new byte[valueLength];
    buffer.get(valueBytes);
    buffer.compact(); // Switch back to writing mode, leave buffer as is
    return deserialize(valueType, status, responseValueType, valueBytes);
  }

  public static int headerSize() {
    return HEADER_SIZE;
  }

  // Assumes buffer is in writing mode (i.e., position indicates how much data has been written)
  public static int bodySize(ByteBuffer buffer) {
    if (buffer.position() < HEADER_SIZE) {
      throw new IllegalArgumentException("Buffer does not contain full header yet!");
    }
    return buffer.getInt(2);
  }

  private <T> Response deserialize(Class<T> valueType, ResponseStatus status,
                                   ResponseValueType responseValueType, byte[] valueBytes) {
    if (valueBytes.length == 0 && status == ResponseStatus.OK) {
      return new SuccessResponse<>(null);
    }
    if (valueBytes.length == 0) {
      return new ErrorResponse(status, null);
    }
    return switch (responseValueType) {
      case JSON_UTF8 -> {
        if (status == ResponseStatus.OK) {
          yield new SuccessResponse<>(valueDeserializer.deserialize(valueBytes, valueType));
        } else {
          yield new ErrorResponse(status, valueDeserializer.deserialize(valueBytes, ErrorResponsePayload.class));
        }
      }
    };
  }
}
