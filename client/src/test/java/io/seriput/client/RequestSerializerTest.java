package io.seriput.client;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

final class RequestSerializerTest {
  private static final RequestSerializer<Object> underTest = new RequestSerializer<>();

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
      byte[] expected = new byte[] {
        RequestOps.GET.op(),
        1,
        1,
        0, 0, 0, 9, // key length = 9
        0, 0, 0, 0,  // value length = 0
        'u', 's', 'e', 'r', ':', '1', '2', '3', '4'
      };
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
      byte[] expected = new byte[] {
        RequestOps.PUT.op(),
        1,
        1,
        0, 0, 0, 9, // key length = 9
        0, 0, 0, 37,  // value length = 37
        'u', 's', 'e', 'r', ':', '1', '2', '3', '4',
      };
      byte[] expectedValueBytes = "{\"firstName\":\"John\",\"lastName\":\"Doe\"}".getBytes(StandardCharsets.UTF_8);
      byte[] combined = new byte[expected.length + expectedValueBytes.length];
      System.arraycopy(expected, 0, combined, 0, expected.length);
      System.arraycopy(expectedValueBytes, 0, combined, expected.length, expectedValueBytes.length);
      assertThat(actualBytes).isEqualTo(combined);
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
      byte[] expected = new byte[] {
        RequestOps.DELETE.op(),
        1,
        1,
        0, 0, 0, 9, // key length = 9
        0, 0, 0, 0,  // value length = 0
        'u', 's', 'e', 'r', ':', '1', '2', '3', '4'
      };
      assertThat(actualBytes).isEqualTo(expected);
    }
  }
}
