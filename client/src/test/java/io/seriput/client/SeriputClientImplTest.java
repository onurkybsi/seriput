package io.seriput.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.seriput.client.exception.InternalErrorException;
import io.seriput.client.exception.InvalidRequestException;
import io.seriput.client.exception.NotFoundException;
import io.seriput.client.fixture.ResponseFixtures;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.node.ObjectNode;

@ExtendWith(MockitoExtension.class)
final class SeriputClientImplTest {
  @Mock SeriputConnectionPool connectionPool;

  private SeriputClientImpl underTest;

  @BeforeEach
  void setUp() {
    underTest = new SeriputClientImpl(connectionPool);
  }

  @Nested
  class Get {
    @Test
    void should_Return_Deserialized_Value_When_Response_Is_Success() {
      // given
      when(connectionPool.enqueue(any(ByteBuffer.class), any(Runnable.class)))
          .thenReturn(
              CompletableFuture.completedFuture(ResponseFixtures.okJson(Map.of("name", "John"))));

      // when
      var actual = underTest.get("test-key", ObjectNode.class).join();

      // then
      assertThat(actual.get("name").asString()).isEqualTo("John");
      verify(connectionPool, times(1)).enqueue(any(ByteBuffer.class), any(Runnable.class));
    }

    @Test
    void should_Throw_NotFoundException_When_Response_Is_NOT_FOUND() {
      // given
      when(connectionPool.enqueue(any(ByteBuffer.class), any(Runnable.class)))
          .thenReturn(CompletableFuture.completedFuture(ResponseFixtures.notFoundVoid()));

      // when
      var thrown = assertThatThrownBy(() -> underTest.get("missing-key", String.class).join());

      // then
      thrown.isInstanceOf(CompletionException.class).hasCause(new NotFoundException(null, null));
      verify(connectionPool, times(1)).enqueue(any(ByteBuffer.class), any(Runnable.class));
    }

    @Test
    void should_Throw_InvalidRequestException_When_Response_Is_INVALID_REQUEST() {
      // given
      when(connectionPool.enqueue(any(ByteBuffer.class), any(Runnable.class)))
          .thenReturn(CompletableFuture.completedFuture(ResponseFixtures.invalidRequestVoid()));

      // when
      var thrown = assertThatThrownBy(() -> underTest.get("key", String.class).join());

      // then
      thrown
          .isInstanceOf(CompletionException.class)
          .hasCause(new InvalidRequestException(null, null));
      verify(connectionPool, times(1)).enqueue(any(ByteBuffer.class), any(Runnable.class));
    }

    @Test
    void should_Throw_InternalErrorException_When_Response_Is_INTERNAL_ERROR() {
      // given
      when(connectionPool.enqueue(any(ByteBuffer.class), any(Runnable.class)))
          .thenReturn(CompletableFuture.completedFuture(ResponseFixtures.internalErrorVoid()));

      // when
      var thrown = assertThatThrownBy(() -> underTest.get("key", String.class).join());

      // then
      thrown
          .isInstanceOf(CompletionException.class)
          .hasCause(new InternalErrorException(null, null));
      verify(connectionPool, times(1)).enqueue(any(ByteBuffer.class), any(Runnable.class));
    }

    @Test
    void should_Throw_IllegalStateException_When_Client_Is_Closed() throws Exception {
      // given
      underTest.close();

      // when
      var thrown = assertThatThrownBy(() -> underTest.get("key", String.class));

      // then
      thrown.isInstanceOf(IllegalStateException.class).hasMessage("SeriputClient is closed!");
      verify(connectionPool, never()).enqueue(any(ByteBuffer.class), any(Runnable.class));
    }
  }

  @Nested
  class Put {
    @Test
    void should_Return_Null_When_Response_Is_Success() {
      // given
      when(connectionPool.enqueue(any(ByteBuffer.class), any(Runnable.class)))
          .thenReturn(CompletableFuture.completedFuture(ResponseFixtures.okVoid()));

      // when
      var actual = underTest.put("key", "value").join();

      // then
      assertThat(actual).isNull();
      verify(connectionPool, times(1)).enqueue(any(ByteBuffer.class), any(Runnable.class));
    }

    @Test
    void should_Throw_InternalErrorException_When_Response_Is_INTERNAL_ERROR() {
      // given
      when(connectionPool.enqueue(any(ByteBuffer.class), any(Runnable.class)))
          .thenReturn(CompletableFuture.completedFuture(ResponseFixtures.internalErrorVoid()));

      // when
      var thrown = assertThatThrownBy(() -> underTest.put("key", "value").join());

      // then
      thrown
          .isInstanceOf(CompletionException.class)
          .hasCause(new InternalErrorException(null, null));
      verify(connectionPool, times(1)).enqueue(any(ByteBuffer.class), any(Runnable.class));
    }

    @Test
    void should_Throw_IllegalStateException_When_Client_Is_Closed() throws Exception {
      // given
      underTest.close();

      // when
      var thrown = assertThatThrownBy(() -> underTest.put("key", "value"));

      // then
      thrown.isInstanceOf(IllegalStateException.class).hasMessage("SeriputClient is closed!");
      verify(connectionPool, never()).enqueue(any(ByteBuffer.class), any(Runnable.class));
    }
  }

  @Nested
  class Delete {
    @Test
    void should_Return_Null_When_Response_Is_Success() {
      // given
      when(connectionPool.enqueue(any(ByteBuffer.class), any(Runnable.class)))
          .thenReturn(CompletableFuture.completedFuture(ResponseFixtures.okVoid()));

      // when
      var actual = underTest.delete("key").join();

      // then
      assertThat(actual).isNull();
      verify(connectionPool, times(1)).enqueue(any(ByteBuffer.class), any(Runnable.class));
    }

    @Test
    void should_Throw_InvalidRequestException_When_Response_Is_INVALID_REQUEST() {
      // given
      when(connectionPool.enqueue(any(ByteBuffer.class), any(Runnable.class)))
          .thenReturn(CompletableFuture.completedFuture(ResponseFixtures.invalidRequestVoid()));

      // when & then
      var thrown = assertThatThrownBy(() -> underTest.delete("key").join());

      // then
      thrown
          .isInstanceOf(CompletionException.class)
          .hasCause(new InvalidRequestException(null, null));
      verify(connectionPool, times(1)).enqueue(any(ByteBuffer.class), any(Runnable.class));
    }

    @Test
    void should_Throw_IllegalStateException_When_Client_Is_Closed() throws Exception {
      // given
      underTest.close();

      // when
      var thrown = assertThatThrownBy(() -> underTest.delete("key"));

      // then
      thrown.isInstanceOf(IllegalStateException.class).hasMessage("SeriputClient is closed!");
      verify(connectionPool, never()).enqueue(any(ByteBuffer.class), any(Runnable.class));
    }
  }

  @Nested
  class CloseTest {
    @Test
    void should_Delegate_To_ConnectionPool_When_Closed() throws Exception {
      // when
      underTest.close();

      // then
      verify(connectionPool, times(1)).close();
    }

    @Test
    void should_Not_Delegate_To_ConnectionPool_When_Already_Closed() throws Exception {
      // given
      underTest.close();

      // when
      underTest.close();

      // then
      verify(connectionPool, times(1)).close();
    }
  }
}
