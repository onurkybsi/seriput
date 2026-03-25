package io.seriput.client.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.seriput.common.serialization.response.ErrorResponse;
import io.seriput.common.serialization.response.ErrorResponsePayload;
import io.seriput.common.serialization.response.ResponseStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class SeriputClientExceptionTest {
  @Nested
  class From {
    @Test
    void should_Return_InvalidRequestException_When_Status_Is_INVALID_REQUEST() {
      // given
      var payload = new ErrorResponsePayload(100, "bad key format");
      var error = new ErrorResponse(ResponseStatus.INVALID_REQUEST, payload);

      // when
      var actual = SeriputClientException.from(error);

      // then
      var expected = new InvalidRequestException("bad key format", 100);
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    void should_Return_NotFoundException_When_Status_Is_NOT_FOUND() {
      // given
      var error = new ErrorResponse(ResponseStatus.NOT_FOUND, null);

      // when
      var actual = SeriputClientException.from(error);

      // then
      var expected = new NotFoundException(null, null);
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    void should_Return_InternalErrorException_When_Status_Is_INTERNAL_ERROR() {
      // given
      var error = new ErrorResponse(ResponseStatus.INTERNAL_ERROR, null);

      // when
      var actual = SeriputClientException.from(error);

      // then
      var expected = new InternalErrorException(null, null);
      assertThat(actual).isEqualTo(expected);
    }

    @Test
    void should_Throw_IllegalArgumentException_When_Status_Is_OK() {
      // given
      var error = new ErrorResponse(ResponseStatus.OK, null);

      // when & then
      assertThatThrownBy(() -> SeriputClientException.from(error))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class IsRetryable {
    @Test
    void should_Return_False_When_InvalidRequestException() {
      // given
      var actual = new InvalidRequestException(null, null);

      // when & then
      assertThat(actual.isRetryable()).isFalse();
    }

    @Test
    void should_Return_False_When_NotFoundException() {
      // given
      var actual = new NotFoundException(null, null);

      // when & then
      assertThat(actual.isRetryable()).isFalse();
    }

    @Test
    void should_Return_True_When_InternalErrorException() {
      // given
      var actual = new InternalErrorException(null, null);

      // when & then
      assertThat(actual.isRetryable()).isTrue();
    }

    @Test
    void should_Return_True_When_ConnectionClosedException() {
      // given
      var actual = new ConnectionClosedException("closed");

      // when & then
      assertThat(actual.isRetryable()).isTrue();
    }
  }
}
