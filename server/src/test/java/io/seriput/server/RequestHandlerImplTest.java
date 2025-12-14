package io.seriput.server;

import io.seriput.common.ObjectMapperProvider;
import io.seriput.server.core.Value;
import io.seriput.server.core.ValueType;
import io.seriput.server.fixture.RequestFixtures;
import io.seriput.server.serialization.response.ResponseSerializer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

final class RequestHandlerImplTest {
  @Nested
  class Instance {
    @Test
    void should_Return_Singleton_Instance() {
      // given & when
      RequestHandler first = RequestHandlerImpl.instance();
      RequestHandler second = RequestHandlerImpl.instance();

      // then
      assertThat(second).isSameAs(first);
    }
  }

  @Nested
  class Handle {
    private static final RequestHandler underTest = RequestHandlerImpl.instance();

    @Nested
    class Get {
      @Test
      void should_Return_Ok_Response_When_There_Is_A_Value_By_GivenKey() {
        // given
        String key = "user:1";
        Object value = new Object() {
          public final String name = "John Doe";
          public final int age = 30;
        };
        underTest.handle(RequestFixtures.serializePut(key, value));
        var requestPayload = RequestFixtures.serializeGet(key);

        // when
        var actual = underTest.handle(requestPayload);

        // then
        var expectedValue = new Value(ValueType.JSON_UTF8, ObjectMapperProvider.getInstance().writeValueAsBytes(value));
        var expected = ResponseSerializer.ok(expectedValue);
        assertThat(actual.array()).isEqualTo(expected.array());
      }

      @Test
      void should_Return_NotFound_Response_When_There_Is_No_Value_By_GivenKey() {
        // given
        String key = "user:1";
        var requestPayload = RequestFixtures.serializeGet(key);

        // when
        var actual = underTest.handle(requestPayload);

        // then
        var expected = ResponseSerializer.notFound();
        assertThat(actual.array()).isEqualTo(expected.array());
      }
    }

    @Nested
    class Put {
      @Test
      void should_Return_Ok_Response_When_Given_KeyValue_Stored_Successfully() {
        // given
        String key = "user:1";
        Object value = new Object() {
          public final String name = "John Doe";
          public final int age = 30;
        };
        var requestPayload = RequestFixtures.serializePut(key, value);

        // when
        var actual = underTest.handle(requestPayload);

        // then
        var expected = ResponseSerializer.ok();
        assertThat(actual.array()).isEqualTo(expected.array());
        var stored = underTest.handle(RequestFixtures.serializeGet(key));
        var expectedStoredValue = new Value(ValueType.JSON_UTF8, ObjectMapperProvider.getInstance().writeValueAsBytes(value));
        assertThat(stored.array()).isEqualTo(ResponseSerializer.ok(expectedStoredValue).array());
      }
    }

    @Nested
    class Delete {
      @Test
      void should_Return_Ok_Response_When_There_Is_A_Value_Delete_By_GivenKey() {
        // given
        String key = "user:1";
        Object value = new Object() {
          public final String name = "John Doe";
          public final int age = 30;
        };
        underTest.handle(RequestFixtures.serializePut(key, value));
        var requestPayload = RequestFixtures.serializeDelete(key);

        // when
        var actual = underTest.handle(requestPayload);

        // then
        var expected = ResponseSerializer.ok();
        assertThat(actual.array()).isEqualTo(expected.array());
        var deleted = underTest.handle(RequestFixtures.serializeGet(key));
        var expectedDeleted = ResponseSerializer.notFound();
        assertThat(deleted.array()).isEqualTo(expectedDeleted.array());
      }

      @Test
      void should_Return_NotFound_Response_When_There_Is_No_Value_To_Delete_By_GivenKey() {
        // given
        String key = "user:1";
        var requestPayload = RequestFixtures.serializeDelete(key);

        // when
        var actual = underTest.handle(requestPayload);

        // then
        var expected = ResponseSerializer.notFound();
        assertThat(actual.array()).isEqualTo(expected.array());
      }
    }
  }
}
