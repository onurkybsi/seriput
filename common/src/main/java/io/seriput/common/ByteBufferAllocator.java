package io.seriput.common;

import java.nio.ByteBuffer;

/** Abstraction for allocating {@link ByteBuffer} instances. */
public sealed interface ByteBufferAllocator
    permits HeapByteBufferAllocator, PooledByteBufferAllocator {
  /**
   * Allocates a {@link ByteBuffer} with the given capacity.
   *
   * @param capacity the capacity of the buffer
   * @return allocated {@link ByteBuffer}
   */
  ByteBuffer allocate(int capacity);
}
