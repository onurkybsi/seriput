package io.seriput.server.serialization.response;

import io.seriput.common.serialization.response.ResponseValueType;
import io.seriput.server.core.Value;

import java.nio.ByteBuffer;

import static io.seriput.common.serialization.response.ResponseStatus.*;
import static io.seriput.common.serialization.response.ResponseValueType.VOID;

/**
 * Response deserializer for the Seriput protocol v1.
 */
public final class ResponseSerializer {
  private static final int VALUE_OFFSET = 1 + 1 + 4; // status + valueType + valueLength
  private static final byte[] voidOkResponse =
    new byte[] { OK.status(), VOID.typeId(), 0x00, 0x00, 0x00, 0x00 /* valueLength */ };
  private static final byte[] notExistResponse =
    new byte[] { NOT_FOUND.status(), VOID.typeId(), 0x00, 0x00, 0x00, 0x00 /* valueLength */ };
  private static final byte[] voidInternalErrorResponse =
    new byte[] { INTERNAL_ERROR.status(), VOID.typeId(), 0x00, 0x00, 0x00, 0x00 /* valueLength */ };

  /**
   * Serializes and {@code OK} response with given {@code value}.
   *
   * @param value value to serialize
   * @return serialized response
   */
  public static ByteBuffer ok(Value value) {
    ByteBuffer buffer = ByteBuffer.allocate(VALUE_OFFSET + value.bytes().length);
    buffer.put(OK.status());
    buffer.put(ResponseValueType.fromByte(value.type().typeId()).typeId());
    buffer.putInt(value.bytes().length);
    buffer.put(value.bytes());
    buffer.flip(); // Switch to read mode
    return buffer;
  }

  /**
   * Serializes a void {@code OK} response.
   *
   * @return serialized response
   */
  public static ByteBuffer ok() {
    return ByteBuffer.wrap(voidOkResponse);
  }

  /**
   * Serializes a {@code NOT_FOUND} response.
   *
   * @return serialized response
   */
  public static ByteBuffer notFound() {
    return ByteBuffer.wrap(notExistResponse);
  }

  /**
   * Serializes an {@code INTERNAL_ERROR} response.
   *
   * @return serialized response
   */
  public static ByteBuffer internalError() {
    return ByteBuffer.wrap(voidInternalErrorResponse);
  }
}
