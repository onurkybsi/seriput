package io.seriput.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class PooledByteBufferAllocatorTest {
  private final PooledByteBufferAllocator underTest = new PooledByteBufferAllocator();

  @Nested
  final class Allocate {
    @Test
    void should_Allocate_DirectBuffer() {
      // when
      ByteBuffer actual = underTest.allocate(64);

      // then
      assertThat(actual.isDirect()).isTrue();
      assertThat(actual.remaining()).isEqualTo(64);
    }

    @Test
    void should_Reuse_ReleasedBuffer_When_Capacity_Is_Sufficient() {
      // given
      ByteBuffer buffer = underTest.allocate(128);
      underTest.release(buffer);

      // when
      ByteBuffer actual = underTest.allocate(64);

      // then
      assertThat(actual).isSameAs(buffer);
      assertThat(actual.remaining()).isEqualTo(64);
    }

    @Test
    void should_Allocate_NewBuffer_When_PooledBuffer_Is_TooSmall() {
      // given
      ByteBuffer smallBuffer = underTest.allocate(32);
      underTest.release(smallBuffer);

      // when
      ByteBuffer actual = underTest.allocate(64);

      // then
      assertThat(actual).isNotSameAs(smallBuffer);
      assertThat(actual.remaining()).isEqualTo(64);
    }
  }

  @Nested
  class Release {
    @Test
    void should_Return_Buffer_To_Pool() {
      // given
      ByteBuffer buffer = underTest.allocate(64);

      // when
      underTest.release(buffer);
      ByteBuffer actual = underTest.allocate(64);

      // then
      assertThat(actual).isSameAs(buffer);
    }
  }
}
