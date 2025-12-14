package io.seriput.common.serialization;

import com.google.common.primitives.Bytes;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

final class RequestSerializerTest {
  private static final RequestSerializer<String, Object> underTest = RequestSerializer.build(KeyType.UTF8, ValueType.JSON_UTF8);

  @Nested
  class SerializeGet {
    @Test
    void should_Serialize_Get_Request() {
      // given
      String key = "user:1234";

      // when
      var actual = underTest.serializeGet(key);

      // then
      byte[] actualBytes = new byte[actual.remaining()];
      actual.get(actualBytes);
      byte[] expected = Bytes.concat(
        new byte[] { // headers
          RequestOp.GET.op(),
          KeyType.UTF8.typeId(),
          ValueType.JSON_UTF8.typeId(),
          0, 0, 0, 9, // keyLength = 9
          0, 0, 0, 0,  // valueLength = 0
        },
        key.getBytes()
      );
      assertThat(actualBytes).isEqualTo(expected);
    }
  }

  @Nested
  class SerializePut {
    @Test
    void should_Serialize_Put_Request() {
      // given
      String key = "user:1234";
      Object value = new Object() {
        public final String firstName = "John";
        public final String lastName = "Doe";
      };

      // when
      var actual = underTest.serializePut(key, value);

      // then
      byte[] actualBytes = new byte[actual.remaining()];
      actual.get(actualBytes);
      byte[] expected = Bytes.concat(
        new byte[] { // headers
          RequestOp.PUT.op(),
          KeyType.UTF8.typeId(),
          ValueType.JSON_UTF8.typeId(),
          0, 0, 0, 9, // keyLength = 9
          0, 0, 0, 37,  // valueLength = 37
        },
        key.getBytes(),
        "{\"firstName\":\"John\",\"lastName\":\"Doe\"}".getBytes(StandardCharsets.UTF_8)
      );
      assertThat(actualBytes).isEqualTo(expected);
    }
  }

  @Nested
  class SerializeDelete {
    @Test
    void should_Serialize_Get_Request() {
      // given
      String key = "user:1234";

      // when
      var actual = underTest.serializeDelete(key);

      // then
      byte[] actualBytes = new byte[actual.remaining()];
      actual.get(actualBytes);
      byte[] expected = Bytes.concat(
        new byte[] { // headers
          RequestOp.DELETE.op(),
          KeyType.UTF8.typeId(),
          ValueType.JSON_UTF8.typeId(),
          0, 0, 0, 9, // keyLength = 9
          0, 0, 0, 0,  // valueLength = 0
        },
        key.getBytes()
      );
      assertThat(actualBytes).isEqualTo(expected);
    }
  }
}
