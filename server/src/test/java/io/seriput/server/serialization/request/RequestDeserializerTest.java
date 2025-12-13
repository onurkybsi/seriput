package io.seriput.server.serialization.request;

import io.seriput.server.core.Key;
import io.seriput.server.core.KeyType;
import io.seriput.server.core.Value;
import io.seriput.server.core.ValueType;
import io.seriput.server.exception.ProtocolViolationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class RequestDeserializerTest {
  @Nested
  class BodySize {
    @Test
    void should_Return_SumOf_Key_And_ValueLength() {
      // given
      ByteBuffer buffer = ByteBuffer.allocate(RequestDeserializer.headerSize());
      buffer.put(RequestOp.PUT.op());
      buffer.put(KeyType.UTF8.typeId());
      buffer.put(ValueType.JSON_UTF8.typeId());
      buffer.putInt(3); // key length
      buffer.putInt(5); // value length
      buffer.flip();

      // when
      int actual = RequestDeserializer.bodySize(buffer);

      // then
      assertThat(actual).isEqualTo(8);
    }

    @Test
    void should_Return_Zero_When_Key_And_Value_Are_Empty() {
      // given
      ByteBuffer buffer = ByteBuffer.allocate(RequestDeserializer.headerSize());
      buffer.put(RequestOp.PUT.op());
      buffer.put(KeyType.UTF8.typeId());
      buffer.put(ValueType.JSON_UTF8.typeId());
      buffer.putInt(0);
      buffer.putInt(0);
      buffer.flip();

      // when
      int actual = RequestDeserializer.bodySize(buffer);

      // then
      assertThat(actual).isEqualTo(0);
    }
  }

  @Nested
  class Deserialize {
    @Test
    void should_Deserialize_Get_Request() {
      // given
      byte[] key = "user:1".getBytes();
      byte[] buffer = concat(
        new byte[] { // header
          RequestOp.GET.op(),
          KeyType.UTF8.typeId(),
          (byte) 0, // no value type
        },
        toBytes(key.length),
        toBytes(0),
        key
      );

      // when
      Request actual = RequestDeserializer.deserialize(buffer);

      // then
      var expected = new GetRequest(new Key(KeyType.UTF8, key));
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    void should_Deserialize_Put_Request() {
      // given
      byte[] key = "user:1".getBytes();
      byte[] value = "{\"name\":\"Alice\",\"age\":30}".getBytes();
      byte[] buffer = concat(
        new byte[] { // header
          RequestOp.PUT.op(),
          KeyType.UTF8.typeId(),
          ValueType.JSON_UTF8.typeId(),
        },
        toBytes(key.length),
        toBytes(value.length),
        key,
        value
      );

      // when
      Request actual = RequestDeserializer.deserialize(buffer);

      // then
      var expected = new PutRequest(new Key(KeyType.UTF8, key), new Value(ValueType.JSON_UTF8, value));
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    void should_Deserialize_Delete_Request() {
      // given
      byte[] key = "user:1".getBytes();
      byte[] buffer = concat(
        new byte[] { // header
          RequestOp.DELETE.op(),
          KeyType.UTF8.typeId(),
          (byte) 0, // no value type
        },
        toBytes(key.length),
        toBytes(0),
        key
      );

      // when
      Request actual = RequestDeserializer.deserialize(buffer);

      // then
      var expected = new DeleteRequest(new Key(KeyType.UTF8, key));
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    void should_Throw_ProtocolViolationException_When_Given_Buffer_Is_Smaller_Than_Excepted() {
      // given
      byte[] buffer = new byte[RequestDeserializer.headerSize() - 1];

      // when
      ProtocolViolationException thrown = assertThrows(ProtocolViolationException.class, () -> RequestDeserializer.deserialize(buffer));

      // then
      assertThat(thrown.getMessage()).isEqualTo("'buffer' too small: 10");
      assertThat(thrown.getCause()).isNull();
    }

    @Test
    void should_Throw_ProtocolViolationException_When_Given_Buffer_DoesNot_Contain_Valid_Op() {
      // given
      byte[] buffer = concat(
        new byte[] {
          (byte) 0x4, // unknown op
          KeyType.UTF8.typeId(),
          ValueType.JSON_UTF8.typeId()
        },
        toBytes(0),
        toBytes(0)
      );

      // when
      ProtocolViolationException thrown = assertThrows(ProtocolViolationException.class, () -> RequestDeserializer.deserialize(buffer));

      // then
      assertThat(thrown.getMessage()).isEqualTo("Unknown request op: 4");
      assertThat(thrown.getCause()).isNull();
    }

    @Test
    void should_Throw_ProtocolViolationException_When_Given_Buffer_DoesNot_Contain_KeyTypeId() {
      // given
      byte[] buffer = concat(
        new byte[] {
          RequestOp.GET.op(),
          (byte) 0x2, // unknown key type
          ValueType.JSON_UTF8.typeId()
        },
        toBytes(0),
        toBytes(0)
      );

      // when
      ProtocolViolationException thrown = assertThrows(ProtocolViolationException.class, () -> RequestDeserializer.deserialize(buffer));

      // then
      assertThat(thrown.getMessage()).isEqualTo("Unknown key type: 2");
      assertThat(thrown.getCause()).isNull();
    }

    @Test
    void should_Throw_ProtocolViolationException_When_Given_Buffer_DoesNot_Contain_ValueTypeId() {
      // given
      byte[] buffer = concat(
        new byte[] {
          RequestOp.PUT.op(),
          KeyType.UTF8.typeId(),
          (byte) 0x2, // unknown value type
        },
        toBytes(0),
        toBytes(0)
      );

      // when
      ProtocolViolationException thrown = assertThrows(ProtocolViolationException.class, () -> RequestDeserializer.deserialize(buffer));

      // then
      assertThat(thrown.getMessage()).isEqualTo("Unknown value type: 2");
      assertThat(thrown.getCause()).isNull();
    }
  }

  private static byte[] concat(byte[]... parts) {
    int size = 0;
    for (byte[] p : parts) {
      size += p.length;
    }

    byte[] concatenated = new byte[size];
    int i = 0;
    for (byte[] p : parts) {
      System.arraycopy(p, 0, concatenated, i, p.length);
      i += p.length;
    }
    return concatenated;
  }

  private static byte[] toBytes(int value) {
    return ByteBuffer.allocate(4).putInt(value).array();
  }
}
