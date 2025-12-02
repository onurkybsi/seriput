package io.seriput.client.serialization;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

final class ResponseDeserializerTest {
  private static final String TEST_VALUE = "{\"firstName\":\"John\",\"lastName\":\"Doe\"}";

  private static final ResponseDeserializer underTest = new ResponseDeserializer();

  @Nested
  class Deserialize {
    @Test
    void should_Return_Deserialized_Response() {
      // given
      ByteBuffer buffer = ByteBuffer.allocate(6 + TEST_VALUE.length());
      buffer.put((byte) 0);
      buffer.put((byte) 1);
      buffer.putInt(TEST_VALUE.length());
      buffer.put(TEST_VALUE.getBytes(StandardCharsets.UTF_8));
      Class<?> valueType = TestType.class;

      // when
      var actual = underTest.deserialize(buffer, valueType);

      // then
      var expected = new SuccessResponse<>(new TestType("John", "Doe"));
      assertThat(actual).isEqualTo(expected);
      assertThat(buffer.position()).isEqualTo(0);
      assertThat(buffer.limit()).isEqualTo(buffer.capacity());
    }
  }

  @Nested
  class HeaderSize {
    @Test
    void should_Return_Header_Size() {
      // given & when
      int actual = ResponseDeserializer.headerSize();

      // then
      assertThat(actual).isEqualTo(6);
    }
  }

  @Nested
  class BodySize {
    @Test
    void should_Return_Body_Size_By_Given_Buffer() {
      // given
      ByteBuffer buffer = ByteBuffer.allocate(6 + TEST_VALUE.length());
      buffer.put((byte) 0);
      buffer.put((byte) 1);
      buffer.putInt(TEST_VALUE.length());
      buffer.put(TEST_VALUE.getBytes(StandardCharsets.UTF_8));

      // when
      int actual = ResponseDeserializer.bodySize(buffer);

      // then
      assertThat(actual).isEqualTo(TEST_VALUE.length());
    }
  }

  private record TestType(String firstName, String lastName) {}
}
