package io.seriput.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

final class CacheImplTest {
  private final CacheImpl<String, String> underTest = new CacheImpl<>();

  @BeforeEach
  void setUp() {
    underTest.clear();
  }

  @Nested
  class Put {
    @Test
    void should_Put_Given_Key_Value_Pair() {
      // given
      String key = "key1";
      String value = "value1";

      // when
      underTest.put(key, value);

      // then
      assertThat(underTest.get("key1")).isEqualTo("value1");
    }

    @Test
    void should_Overwrite_Value_For_Given_Existing_Key() {
      // given
      String key = "key";
      String value1 = "value1";
      String value2 = "value2";

      // when
      underTest.put(key, value1);
      underTest.put(key, value2);

      // then
      assertThat(underTest.get(key)).isEqualTo(value2);
    }

    @Test
    void should_Throw_NullPointerException_When_Given_Key_Is_Null() {
      // when / then
      try {
        underTest.put(null, "value");
      } catch (NullPointerException e) {
        assertThat(e.getMessage()).isEqualTo("key may not be null!");
      }
    }

    @Test
    void should_Throw_NullPointerException_When_Given_Value_Is_Null() {
      // when / then
      try {
        underTest.put("key", null);
      } catch (NullPointerException e) {
        assertThat(e.getMessage()).isEqualTo("value may not be null!");
      }
    }
  }
}
