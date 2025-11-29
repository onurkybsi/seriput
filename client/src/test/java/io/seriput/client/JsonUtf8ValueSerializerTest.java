package io.seriput.client;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class JsonUtf8ValueSerializerTest {
  private static final JsonUtf8ValueSerializer<Object> underTest = new JsonUtf8ValueSerializer<>();

  @Nested
  class Serialize {
    @Test
    void should_Serialize_Given_Value() {
      // given
      var value = new Object() {
        public final String name = "John Doe";
        public final int age = 30;
      };

      // when
      var actual = underTest.serialize(value);

      // then
      assertThat(actual).isEqualTo("{\"age\":30,\"name\":\"John Doe\"}".getBytes());
    }

    @Test
    void should_Throw_NullPointerException_Given_Null_Value() {
      // given & when
      var actual = assertThrows(NullPointerException.class, () -> underTest.serialize(null));

      // then
      assertThat(actual.getMessage()).isEqualTo("value may not be null!");
    }
  }

  @Nested
  class TypeId {
    @Test
    void should_Return_JsonUtf8ValueSerializer_TypeId() {
      // given & when
      var actual = underTest.typeId();

      // then
      assertThat(actual).isEqualTo((byte) 0x01);
    }
  }
}
