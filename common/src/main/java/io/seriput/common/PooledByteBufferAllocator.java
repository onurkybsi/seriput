package io.seriput.common;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A pooling allocator that reuses direct {@link ByteBuffer} instances.
 * <p>
 * Buffers returned by {@link #allocate(int)} are direct buffers. When no longer
 * needed,
 * callers should return them via {@link #release(ByteBuffer)} so they can be
 * reused.
 * <p>
 * If a pooled buffer is too small for the requested capacity, a new direct
 * buffer is allocated instead.
 */
public final class PooledByteBufferAllocator implements ByteBufferAllocator {
  private final Queue<ByteBuffer> pool = new ConcurrentLinkedQueue<>();

  @Override
  public ByteBuffer allocate(int capacity) {
    ByteBuffer buffer = pool.poll();
    if (buffer != null && buffer.capacity() >= capacity) {
      buffer.clear();
      buffer.limit(capacity);
      return buffer;
    }
    return ByteBuffer.allocateDirect(capacity);
  }

  /**
   * Returns a buffer to the pool for reuse.
   *
   * @param buffer the buffer to release
   */
  public void release(ByteBuffer buffer) {
    pool.offer(buffer);
  }
}
