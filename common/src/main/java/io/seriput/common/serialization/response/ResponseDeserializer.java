package io.seriput.common.serialization.response;

import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkArgument;
import static io.seriput.common.serialization.response.ResponseStatus.isErrorStatus;

/**
 * Response serializer for Seriput protocol v1.
 */
public final class ResponseDeserializer {
  private static final int HEADER_SIZE = 1 + 1 + 4; // status + valueTypeId + valueLength
  private static final int VALUE_LENGTH_OFFSET = 2;

  private final ValueDeserializer valueDeserializer = new JsonUtf8ValueDeserializer();

  private ResponseDeserializer() {}

  /**
   * Builds a {@code ResponseDeserializer}.
   *
   * @return built {@code RequestSerializer} instance
   */
  public static ResponseDeserializer build() {
    return new ResponseDeserializer();
  }

  /**
   * Returns the size of the response header in bytes.
   *
   * @return size of the response header in bytes
   */
  public static int headerSize() {
    return HEADER_SIZE;
  }

  /**
   * Returns the size of the response body in bytes.
   * <p>
   * <b>Note that</b> this method assumes that
   * the given buffer's position is at the beginning of the response to be extracted.
   *
   * @param buffer buffer to read the body size from
   * @return size of the response body in bytes
   * @throws IllegalArgumentException if {@code buffer} does not contain the full header yet
   */
  public static int bodySize(ByteBuffer buffer) {
    checkArgument(buffer.remaining() >= HEADER_SIZE, "Buffer does not contain the full header yet!");
    return buffer.getInt(buffer.position() + VALUE_LENGTH_OFFSET);
  }

  /**
   * Deserializes the server response from given {@code buffer}.
   *
   * @param buffer buffer to deserialize from
   * @param valueType type of the response value, or {@code null} if the value type is {@code VOID}
   * @return deserialized response
   * @throws IllegalArgumentException if given buffer doesn't contain full response bytes
   * @param <T> type of the response value
   */
  public <T> Response<?> deserialize(ByteBuffer buffer, /* @Nullable */ Class<T> valueType) {
    checkArgument(buffer.remaining() >= HEADER_SIZE, "Buffer does not contain the full header yet!");
    ResponseStatus status = ResponseStatus.fromByte(buffer.get());
    checkArgument(status != null, "'status' is not valid: " + buffer.get(0));
    ResponseValueType responseValueType = ResponseValueType.fromByte(buffer.get());
    checkArgument(responseValueType != null, "'valueTypeId' is not valid: " + buffer.get(1));
    int valueLength = buffer.getInt();
    checkArgument(buffer.remaining() >= valueLength, "Buffer does not contain the full response yet!");
    byte[] valueBytes = new byte[valueLength];
    buffer.get(valueBytes);
    return deserialize(valueType, status, responseValueType, valueBytes);
  }

  private <T> Response<?> deserialize(Class<T> valueType, ResponseStatus status,
                                      ResponseValueType responseValueType, byte[] valueBytes) {
    if (ResponseStatus.OK.equals(status)) {
      return deserializeOk(valueType, responseValueType, valueBytes);
    } else if (isErrorStatus(status)) {
      return deserializeErrorResponse(status, responseValueType, valueBytes);
    } else {
      throw new IllegalStateException("Unexpected response status: " + status);
    }
  }

  private <T> Response<T> deserializeOk(Class<T> valueType, ResponseValueType responseValueType, byte[] valueBytes) {
    if (ResponseValueType.VOID.equals(responseValueType)) {
      return new SuccessResponse<>(null);
    } else if (ResponseValueType.JSON_UTF8.equals(responseValueType)) {
      return new SuccessResponse<>(valueDeserializer.deserialize(valueBytes, valueType));
    } else {
      throw new IllegalStateException("Unexpected response value type: " + responseValueType);
    }
  }

  private Response<ErrorResponsePayload> deserializeErrorResponse(ResponseStatus status,
                                                                  ResponseValueType responseValueType, byte[] valueBytes) {
    if (ResponseValueType.VOID.equals(responseValueType)) {
      return new ErrorResponse(status, null);
    } else if (ResponseValueType.JSON_UTF8.equals(responseValueType)) {
      return new ErrorResponse(status, valueDeserializer.deserialize(valueBytes, ErrorResponsePayload.class));
    } else {
      throw new IllegalStateException("Unexpected response value type: " + responseValueType);
    }
  }
}
