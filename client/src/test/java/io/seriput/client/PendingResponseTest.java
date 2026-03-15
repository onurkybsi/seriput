package io.seriput.client;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

final class PendingResponseTest {
  @Nested
  class IsReadyToBeCompleted {
    @Test
    void should_Return_False_When_Payload_Is_Null() {
      // given
      var pendingResponse = new PendingResponse(new CompletableFuture<>());

      // when
      var actual = pendingResponse.isReadyToBeCompleted();

      // then
      assertThat(actual).isFalse();
    }

    @Test
    void should_Return_True_When_Payload_Is_Set() {
      // given
      var pendingResponse = new PendingResponse(new CompletableFuture<>());
      pendingResponse.payload(new byte[] { 1, 2, 3 });

      // when
      var actual = pendingResponse.isReadyToBeCompleted();

      // then
      assertThat(actual).isTrue();
    }
  }
}
