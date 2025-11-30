package io.seriput.client.serialization;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

final class ResponseDeserializerTest {
  private static final String TEST_VALUE = "{\"firstName\":\"John\",\"lastName\":\"Doe\"}";

  private static final ResponseDeserializer underTest = new ResponseDeserializer();

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
    var actual = underTest.tryToDeserialize(buffer, valueType);

    // then
    var expected = new SuccessResponse<>(new TestType("John", "Doe"));
    assertThat(actual).isEqualTo(expected);
    assertThat(buffer.position()).isEqualTo(0);
    assertThat(buffer.limit()).isEqualTo(buffer.capacity());
  }

  @Test
  void should_Return_Null_When_Status_Byte_Available_Only() {
    // given
    ByteBuffer buffer = ByteBuffer.allocate(6 + TEST_VALUE.length());
    buffer.put((byte) 0);
    Class<?> valueType = TestType.class;
    int originalPosition = buffer.position();
    int originalLimit = buffer.limit();

    // when
    var actual = underTest.tryToDeserialize(buffer, valueType);

    // then
    assertThat(actual).isNull();
    assertThat(buffer.position()).isEqualTo(originalPosition);
    assertThat(buffer.limit()).isEqualTo(originalLimit);
    buffer.flip();
    assertThat(buffer.get()).isEqualTo((byte) 0);
  }

  @Test
  void should_Return_Null_When_Status_And_ValueTypeId_Bytes_Available_Only() {
    // given
    ByteBuffer buffer = ByteBuffer.allocate(6 + TEST_VALUE.length());
    buffer.put((byte) 0);
    buffer.put((byte) 1);
    Class<?> valueType = TestType.class;
    int originalPosition = buffer.position();
    int originalLimit = buffer.limit();

    // when
    var actual = underTest.tryToDeserialize(buffer, valueType);

    // then
    assertThat(actual).isNull();
    assertThat(buffer.position()).isEqualTo(originalPosition);
    assertThat(buffer.limit()).isEqualTo(originalLimit);
    buffer.flip();
    assertThat(buffer.get()).isEqualTo((byte) 0);
    assertThat(buffer.get()).isEqualTo((byte) 1);
  }

  @Test
  void should_Return_Null_When_Value_Missing_Only() {
    // given
    ByteBuffer buffer = ByteBuffer.allocate(6 + TEST_VALUE.length());
    buffer.put((byte) 0);
    buffer.put((byte) 1);
    buffer.putInt(TEST_VALUE.length());
    Class<?> valueType = TestType.class;
    int originalPosition = buffer.position();
    int originalLimit = buffer.limit();

    // when
    var actual = underTest.tryToDeserialize(buffer, valueType);

    // then
    assertThat(actual).isNull();
    assertThat(buffer.position()).isEqualTo(originalPosition);
    assertThat(buffer.limit()).isEqualTo(originalLimit);
    buffer.flip();
    assertThat(buffer.get()).isEqualTo((byte) 0);
    assertThat(buffer.get()).isEqualTo((byte) 1);
    assertThat(buffer.getInt()).isEqualTo(TEST_VALUE.length());
  }

  private record TestType(String firstName, String lastName) {}
}
