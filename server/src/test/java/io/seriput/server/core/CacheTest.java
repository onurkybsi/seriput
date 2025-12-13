package io.seriput.server.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

final class CacheTest {
  private static final Cache underTest = new Cache();

  @BeforeEach
  void setUp() {
    underTest.clear();
  }

  @Nested
  class Get {
    @Test
    void should_Return_Value_Stored_By_Given_Key() {
      // given
      Key key = new Key(KeyType.UTF8, "user:1".getBytes());
      Value value = new Value(ValueType.JSON_UTF8, "{\"name\":\"Alice\",\"age\":30}".getBytes());
      underTest.put(key, value);

      // when
      var actual = underTest.get(key);

      // then
      assertThat(actual).isEqualTo(value);
    }

    @Test
    void should_Return_Null_When_NoValue_Stored_By_Given_Key() {
      // given
      Key key = new Key(KeyType.UTF8, "user:1".getBytes());

      // when
      var actual = underTest.get(key);

      // then
      assertThat(actual).isNull();
    }

    @Test
    void should_Throw_NPE_When_Given_Key_Is_Null() {
      // given
      Key key = null;

      // when & then
      assertThatThrownBy(() -> underTest.get(key)).hasMessage("key may not be null!");
    }
  }

  @Nested
  class Put {
    @Test
    void should_Store_Given_Key_And_Value() {
      // given
      Key key = new Key(KeyType.UTF8, "user:1".getBytes());
      Value value = new Value(ValueType.JSON_UTF8, "{\"name\":\"Alice\",\"age\":30}".getBytes());

      // when
      underTest.put(key, value);

      // then
      var stored = underTest.get(key);
      assertThat(stored).isEqualTo(value);
    }

    @Test
    void should_Throw_NPE_When_Given_Key_Is_Null() {
      // given
      Key key = null;
      Value value = new Value(ValueType.JSON_UTF8, "{\"name\":\"Alice\",\"age\":30}".getBytes());

      // when & then
      assertThatThrownBy(() -> underTest.put(key, value)).hasMessage("key may not be null!");
    }

    @Test
    void should_Throw_NPE_When_Given_Value_Is_Null() {
      // given
      Key key = new Key(KeyType.UTF8, "user:1".getBytes());
      Value value = null;

      // when & then
      assertThatThrownBy(() -> underTest.put(key, value)).hasMessage("value may not be null!");
    }
  }

  @Nested
  class Delete {
    @Test
    void should_Delete_Stored_Value() {
      // given
      Key key = new Key(KeyType.UTF8, "user:1".getBytes());
      Value value = new Value(ValueType.JSON_UTF8, "{\"name\":\"Alice\",\"age\":30}".getBytes());
      underTest.put(key, value);

      // when
      var actual = underTest.delete(key);

      // then
      assertThat(actual).isEqualTo(value);
      var stored = underTest.get(key);
      assertThat(stored).isNull();
    }

    @Test
    void should_Return_Null_When_NoValue_Stored_By_Given_Key() {
      // given
      Key key = new Key(KeyType.UTF8, "user:1".getBytes());

      // when
      var actual = underTest.delete(key);

      // then
      assertThat(actual).isNull();
    }

    @Test
    void should_Throw_NPE_When_Given_Key_Is_Null() {
      // given
      Key key = null;

      // when & then
      assertThatThrownBy(() -> underTest.delete(key)).hasMessage("key may not be null!");
    }
  }
}
