package io.seriput.common;

import java.nio.ByteBuffer;

/**
 * Allocates heap-backed {@link ByteBuffer} instances via
 * {@link ByteBuffer#allocate(int)}.
 */
public final class HeapByteBufferAllocator implements ByteBufferAllocator {
  @Override
  public ByteBuffer allocate(int capacity) {
    return ByteBuffer.allocate(capacity);
  }
}
