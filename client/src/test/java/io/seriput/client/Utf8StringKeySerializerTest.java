package io.seriput.client;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

final class Utf8StringKeySerializerTest {
  private static final Utf8StringKeySerializer underTest = new Utf8StringKeySerializer();

  @Nested
  class Serialize {
    @Test
    void should_Serialize_Given_Key() {
      // given
      String key = "user:1234";

      // when
      var actual = underTest.serialize(key);

      // then
      assertThat(actual).isEqualTo(new byte[] { 0x75, 0x73, 0x65, 0x72, 0x3A, 0x31, 0x32, 0x33, 0x34 });
    }

    @Test
    void should_Throw_NullPointerException_When_Given_Key_Is_Null() {
      // given & when
      var actual = assertThrows(NullPointerException.class, () -> underTest.serialize(null));

      // then
      assertThat(actual.getMessage()).isEqualTo("key may not be null!");
    }
  }

  @Nested
  class TypeId {
    @Test
    void should_Return_Utf8StringKeySerializer_TypeId() {
      // given & when
      var actual = underTest.typeId();

      // then
      assertThat(actual).isEqualTo((byte) 0x01);
    }
  }
}
