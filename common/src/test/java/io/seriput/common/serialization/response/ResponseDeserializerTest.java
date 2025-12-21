package io.seriput.common.serialization.response;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

final class ResponseDeserializerTest {
  private static final String TEST_VALUE = "{\"firstName\":\"John\",\"lastName\":\"Doe\"}";

  private static final ResponseDeserializer underTest = new ResponseDeserializer();

  @Nested
  class Deserialize {
    @Test
    void should_Return_Deserialized_Response_By_Seriput_Protocol_V1() {
      // given
      ByteBuffer buffer = ByteBuffer.allocate(6 + TEST_VALUE.length());
      buffer.put(ResponseStatus.OK.status());
      buffer.put(ResponseValueType.JSON_UTF8.valueTypeId());
      buffer.putInt(TEST_VALUE.length());
      buffer.put(TEST_VALUE.getBytes(StandardCharsets.UTF_8));
      buffer.flip();
      Class<?> valueType = TestType.class;

      // when
      var actual = underTest.deserialize(buffer, valueType);

      // then
      var expected = new SuccessResponse<>(new TestType("John", "Doe"));
      assertThat(actual).isEqualTo(expected);
      assertThat(buffer.position()).isEqualTo(43);
      assertThat(buffer.limit()).isEqualTo(buffer.capacity());
    }

    @Test
    void should_Throw_IllegalStateException_When_Given_Buffer_Does_Not_Contain_FullHeader() {
      // given
      ByteBuffer buffer = ByteBuffer.allocate(6 + TEST_VALUE.length());
      buffer.put(ResponseStatus.OK.status());
      buffer.put(ResponseValueType.JSON_UTF8.valueTypeId());
      buffer.flip();

      // when & then
      assertThatThrownBy(() -> ResponseDeserializer.bodySize(buffer))
        .hasMessage("Buffer does not contain the full header yet!");
    }

    @Test
    void should_Throw_IllegalStateException_When_Given_Buffer_Does_Not_Contain_A_ValidStatus() {
      // given
      ByteBuffer buffer = ByteBuffer.allocate(6 + TEST_VALUE.length());
      buffer.put((byte) 4);
      buffer.put(ResponseValueType.JSON_UTF8.valueTypeId());
      buffer.putInt(TEST_VALUE.length());
      buffer.put(TEST_VALUE.getBytes(StandardCharsets.UTF_8));
      buffer.flip();
      Class<?> valueType = TestType.class;

      // when & then
      assertThatThrownBy(() -> underTest.deserialize(buffer, valueType))
        .hasMessage("'status' is not valid: 4");
    }

    @Test
    void should_Throw_IllegalStateException_When_Given_Buffer_Does_Not_Contain_A_ValidValueTypeId() {
      // given
      ByteBuffer buffer = ByteBuffer.allocate(6 + TEST_VALUE.length());
      buffer.put(ResponseStatus.OK.status());
      buffer.put((byte) 2);
      buffer.putInt(TEST_VALUE.length());
      buffer.put(TEST_VALUE.getBytes(StandardCharsets.UTF_8));
      buffer.flip();
      Class<?> valueType = TestType.class;

      // when & then
      assertThatThrownBy(() -> underTest.deserialize(buffer, valueType))
        .hasMessage("'valueTypeId' is not valid: 2");
    }

    @Test
    void should_Throw_IllegalStateException_When_Given_Buffer_Does_Not_Contain_FullResponse() {
      // given
      ByteBuffer buffer = ByteBuffer.allocate(6 + TEST_VALUE.length());
      buffer.put(ResponseStatus.OK.status());
      buffer.put(ResponseValueType.JSON_UTF8.valueTypeId());
      buffer.putInt(TEST_VALUE.length());
      buffer.flip();
      Class<?> valueType = TestType.class;

      // when & then
      assertThatThrownBy(() -> underTest.deserialize(buffer, valueType))
        .hasMessage("Buffer does not contain the full response yet!");
    }
  }

  @Nested
  class HeaderSize {
    @Test
    void should_Return_Header_Size_By_Seriput_Protocol_V1() {
      // given & when
      int actual = ResponseDeserializer.headerSize();

      // then
      assertThat(actual).isEqualTo(6);
    }
  }

  @Nested
  class BodySize {
    @Test
    void should_Return_Body_Size_By_Seriput_Protocol_V1_And_Given_Buffer() {
      // given
      ByteBuffer buffer = ByteBuffer.allocate(6 + TEST_VALUE.length());
      buffer.put(ResponseStatus.OK.status());
      buffer.put(ResponseValueType.JSON_UTF8.valueTypeId());
      buffer.putInt(TEST_VALUE.length());
      buffer.put(TEST_VALUE.getBytes(StandardCharsets.UTF_8));
      buffer.flip();

      // when
      int actual = ResponseDeserializer.bodySize(buffer);

      // then
      assertThat(actual).isEqualTo(TEST_VALUE.length());
    }

    @Test
    void should_Throw_IllegalStateException_When_Given_Buffer_Does_Not_Contain_FullHeader() {
      // given
      ByteBuffer buffer = ByteBuffer.allocate(6 + TEST_VALUE.length());
      buffer.put(ResponseStatus.OK.status());
      buffer.put(ResponseValueType.JSON_UTF8.valueTypeId());
      buffer.flip();

      // when & then
      assertThatThrownBy(() -> ResponseDeserializer.bodySize(buffer))
        .hasMessage("Buffer does not contain the full header yet!");
    }
  }

  private record TestType(String firstName, String lastName) {}
}
