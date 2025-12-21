package io.seriput.server.serialization.response;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import io.seriput.common.serialization.response.ResponseStatus;
import io.seriput.common.serialization.response.ResponseValueType;
import io.seriput.server.core.Value;
import io.seriput.server.core.ValueType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

final class ResponseSerializerTest {
  @Nested
  class OkResponse {
    @Test
    void should_Serialize_Ok_Response_With_Value() {
      // given
      Value value = new Value(ValueType.JSON_UTF8, "{\"key\":\"value\"}".getBytes());

      // when
      var actual = ResponseSerializer.ok(value);

      // then
      var expected = Bytes.concat(
        new byte[] { ResponseStatus.OK.status() },
        new byte[] { ResponseValueType.fromByte(value.type().typeId()).typeId() },
        Ints.toByteArray(value.bytes().length),
        value.bytes()
      );
      assertThat(actual.array()).isEqualTo(expected);
      assertThat(actual.position()).isEqualTo(0);
      assertThat(actual.limit()).isEqualTo(expected.length);
    }

    @Test
    void should_Serialize_Void_Ok_Response() {
      // given

      // when
      var actual = ResponseSerializer.ok();

      // then
      var expected = Bytes.concat(
        new byte[] { ResponseStatus.OK.status() },
        new byte[] { ResponseValueType.VOID.typeId() },
        Ints.toByteArray(0 /* valueLength */)
      );
      assertThat(actual.array()).isEqualTo(expected);
      assertThat(actual.position()).isEqualTo(0);
      assertThat(actual.limit()).isEqualTo(expected.length);
    }
  }

  @Nested
  class NotFound {
    @Test
    void should_Serialize_Not_Found_Response() {
      // given

      // when
      var actual = ResponseSerializer.notFound();

      // then
      var expected = Bytes.concat(
        new byte[] { ResponseStatus.NOT_FOUND.status() },
        new byte[] { ResponseValueType.VOID.typeId() },
        Ints.toByteArray(0 /* valueLength */)
      );
      assertThat(actual.array()).isEqualTo(expected);
      assertThat(actual.position()).isEqualTo(0);
      assertThat(actual.limit()).isEqualTo(expected.length);
    }
  }

  @Nested
  class InternalError {
    @Test
    void should_Serialize_Internal_Error_Response() {
      // given

      // when
      var actual = ResponseSerializer.internalError();

      // then
      var expected = Bytes.concat(
        new byte[] { ResponseStatus.INTERNAL_ERROR.status() },
        new byte[] { ResponseValueType.VOID.typeId() },
        Ints.toByteArray(0 /* valueLength */)
      );
      assertThat(actual.array()).isEqualTo(expected);
      assertThat(actual.position()).isEqualTo(0);
      assertThat(actual.limit()).isEqualTo(expected.length);
    }
  }
}
