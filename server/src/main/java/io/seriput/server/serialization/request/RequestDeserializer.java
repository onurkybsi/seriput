package io.seriput.server.serialization.request;

import io.seriput.server.core.Key;
import io.seriput.server.core.KeyType;
import io.seriput.server.core.ValueType;
import io.seriput.server.core.Value;
import io.seriput.server.exception.ProtocolViolationException;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Request deserializer for the Seriput protocol v1.
 */
public final class RequestDeserializer {
  private static final int OP_OFFSET = 0;
  private static final int KEY_TYPE_OFFSET = 1;
  private static final int VALUE_TYPE_OFFSET = 2;
  private static final int KEY_LENGTH_OFFSET = 3;
  private static final int VALUE_LENGTH_OFFSET = 7;
  private static final int HEADER_SIZE = 1 + 1 + 1 + 4 + 4; // op + keyTypeId + valueTypeId + keyLength + valueLength

  public static int headerSize() {
    return HEADER_SIZE;
  }

  /**
   * Returns the size of the body by given {@code buffer}.
   * <p>
   * Note that this method assumes that the given {@code buffer} is in read mode and contains at least the full header!
   *
   * @param buffer request payload buffer
   * @return size of the request body
   */
  public static int bodySize(ByteBuffer buffer) {
    return
      buffer.getInt(buffer.position() + KEY_LENGTH_OFFSET) +
      buffer.getInt(buffer.position() + VALUE_LENGTH_OFFSET);
  }

  /**
   * Deserializes the given {@code buffer} into a {@link Request}.
   *
   * @param buffer request payload buffer
   * @return deserialized request
   */
  public static Request deserialize(byte[] buffer) {
    assertOrThrow(buffer.length >= HEADER_SIZE, "'buffer' too small: " + buffer.length);
    RequestOp requestOp = RequestOp.fromByte(buffer[OP_OFFSET]);
    assertOrThrow(requestOp != null, "Unknown request op: " + buffer[OP_OFFSET]);
    return switch (requestOp) {
      case GET -> deserializeGet(buffer);
      case PUT -> deserializePut(buffer);
      case DELETE -> deserializeDelete(buffer);
    };
  }

  private static GetRequest deserializeGet(byte[] buffer) {
    KeyType keyType = KeyType.fromByte(buffer[KEY_TYPE_OFFSET]);
    assertOrThrow(keyType != null, "Unknown key type: " + buffer[KEY_TYPE_OFFSET]);
    int keyLength = toIntBigEndian(buffer, KEY_LENGTH_OFFSET);
    byte[] keyBytes = Arrays.copyOfRange(buffer, HEADER_SIZE, HEADER_SIZE + keyLength);
    return new GetRequest(new Key(keyType, keyBytes));
  }

  private static PutRequest deserializePut(byte[] buffer) {
    KeyType keyType = KeyType.fromByte(buffer[KEY_TYPE_OFFSET]);
    assertOrThrow(keyType != null, "Unknown key type: " + buffer[KEY_TYPE_OFFSET]);
    ValueType valueType = ValueType.fromByte(buffer[VALUE_TYPE_OFFSET]);
    assertOrThrow(valueType != null, "Unknown value type: " + buffer[VALUE_TYPE_OFFSET]);
    int keyLength = toIntBigEndian(buffer, KEY_LENGTH_OFFSET);
    int valueLength = toIntBigEndian(buffer, VALUE_LENGTH_OFFSET);
    byte[] keyBytes = Arrays.copyOfRange(buffer, HEADER_SIZE, HEADER_SIZE + keyLength);
    byte[] valueBytes = Arrays.copyOfRange(buffer, HEADER_SIZE + keyLength, HEADER_SIZE + keyLength + valueLength);
    return new PutRequest(new Key(keyType, keyBytes), new Value(valueType, valueBytes));
  }

  private static DeleteRequest deserializeDelete(byte[] buffer) {
    KeyType keyType = KeyType.fromByte(buffer[KEY_TYPE_OFFSET]);
    assertOrThrow(keyType != null, "Unknown key type: " + buffer[KEY_TYPE_OFFSET]);
    int keyLength = toIntBigEndian(buffer, KEY_LENGTH_OFFSET);
    byte[] keyBytes = Arrays.copyOfRange(buffer, HEADER_SIZE, HEADER_SIZE + keyLength);
    return new DeleteRequest(new Key(keyType, keyBytes));
  }

  private static void assertOrThrow(boolean condition, String message) {
    if (!condition) {
      throw new ProtocolViolationException(message);
    }
  }

  private static int toIntBigEndian(byte[] bytes, int startingIx) {
    return ((bytes[startingIx] & 0xFF) << 24) |
      ((bytes[startingIx + 1] & 0xFF) << 16) |
      ((bytes[startingIx + 2] & 0xFF) << 8)  |
      ((bytes[startingIx + 3] & 0xFF));
  }
}
