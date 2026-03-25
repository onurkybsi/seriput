package io.seriput.server.serialization.response;

import static io.seriput.common.serialization.response.ResponseStatus.*;
import static io.seriput.common.serialization.response.ResponseValueType.VOID;

import io.seriput.common.ByteBufferAllocator;
import io.seriput.common.serialization.response.ResponseValueType;
import io.seriput.server.core.Value;
import java.nio.ByteBuffer;

/** Response serializer for the Seriput protocol v1. */
public final class ResponseSerializer {
  private static final int VALUE_OFFSET = 1 + 1 + 4; // status + valueType + valueLength
  private static final byte[] voidOkResponse =
      new byte[] {OK.status(), VOID.typeId(), 0x00, 0x00, 0x00, 0x00 /* valueLength */};
  private static final byte[] notExistResponse =
      new byte[] {NOT_FOUND.status(), VOID.typeId(), 0x00, 0x00, 0x00, 0x00 /* valueLength */};
  private static final byte[] voidInternalErrorResponse =
      new byte[] {INTERNAL_ERROR.status(), VOID.typeId(), 0x00, 0x00, 0x00, 0x00 /* valueLength */};
  public static int HEADER_SIZE = VALUE_OFFSET;
  public static int VALUE_LENGTH_OFFSET = 2;

  private final ByteBufferAllocator allocator;

  public ResponseSerializer(ByteBufferAllocator allocator) {
    this.allocator = allocator;
  }

  /**
   * Serializes an {@code OK} response with given {@code value}.
   *
   * @param value value to serialize
   * @return serialized response
   */
  public ByteBuffer ok(Value value) {
    ByteBuffer buffer = allocator.allocate(VALUE_OFFSET + value.bytes().length);
    buffer.put(OK.status());
    buffer.put(ResponseValueType.fromByte(value.type().typeId()).typeId());
    buffer.putInt(value.bytes().length);
    buffer.put(value.bytes());
    buffer.flip();
    return buffer;
  }

  /**
   * Serializes a void {@code OK} response.
   *
   * @return serialized response
   */
  public ByteBuffer ok() {
    return wrapStatic(voidOkResponse);
  }

  /**
   * Serializes a {@code NOT_FOUND} response.
   *
   * @return serialized response
   */
  public ByteBuffer notFound() {
    return wrapStatic(notExistResponse);
  }

  /**
   * Serializes an {@code INTERNAL_ERROR} response.
   *
   * @return serialized response
   */
  public ByteBuffer internalError() {
    return wrapStatic(voidInternalErrorResponse);
  }

  private ByteBuffer wrapStatic(byte[] source) {
    ByteBuffer buffer = allocator.allocate(source.length);
    buffer.put(source);
    buffer.flip();
    return buffer;
  }
}
