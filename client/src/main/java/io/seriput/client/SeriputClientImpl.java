package io.seriput.client;

import io.seriput.client.exception.SeriputClientException;
import io.seriput.common.PooledByteBufferAllocator;
import io.seriput.common.serialization.request.KeyType;
import io.seriput.common.serialization.request.RequestSerializer;
import io.seriput.common.serialization.request.ValueType;
import io.seriput.common.serialization.response.ErrorResponse;
import io.seriput.common.serialization.response.ResponseDeserializer;
import io.seriput.common.serialization.response.SuccessResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Builder;

final class SeriputClientImpl implements SeriputClient {
  private static final int DEFAULT_POOL_SIZE = 10;
  private static final int DEFAULT_READ_BUFFER_SIZE = 8192;
  private static final int DEFAULT_MAX_OUTBOUND_QUEUE_SIZE = 1024;

  private final SeriputConnectionPool connectionPool;
  private final AtomicBoolean isClosed = new AtomicBoolean(false);
  private final PooledByteBufferAllocator allocator = new PooledByteBufferAllocator();
  private final RequestSerializer<String, Object> requestSerializer =
      RequestSerializer.build(KeyType.UTF8, ValueType.JSON_UTF8, allocator);
  private final ResponseDeserializer responseSerializer = ResponseDeserializer.build();

  // Visible for testing
  SeriputClientImpl(SeriputConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }

  @Builder
  SeriputClientImpl(
      String host,
      int port,
      int poolSize,
      ExecutorService callbackExecutor,
      int readBufferSize,
      int maxOutboundQueueSize)
      throws IOException {
    this.connectionPool =
        new SeriputConnectionPool(
            host,
            port,
            poolSize > 0 ? poolSize : DEFAULT_POOL_SIZE,
            callbackExecutor != null
                ? callbackExecutor
                : Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()),
            readBufferSize > 0 ? readBufferSize : DEFAULT_READ_BUFFER_SIZE,
            maxOutboundQueueSize > 0 ? maxOutboundQueueSize : DEFAULT_MAX_OUTBOUND_QUEUE_SIZE);
    this.connectionPool.start();
  }

  public static SeriputClientImpl build(String host, int port, int poolSize) throws IOException {
    return SeriputClient.builder(host, port).poolSize(poolSize).build();
  }

  @Override
  public void close() throws Exception {
    if (isClosed.compareAndSet(false, true)) {
      this.connectionPool.close();
    }
  }

  @Override
  public <T> CompletableFuture<T> get(String key, Class<T> valueType) {
    if (isClosed.get()) {
      throw new IllegalStateException("SeriputClient is closed!");
    }
    ByteBuffer payload = requestSerializer.serializeGet(key);
    return connectionPool
        .enqueue(payload, () -> allocator.release(payload))
        .thenApply(
            buffer -> {
              var response = responseSerializer.deserialize(ByteBuffer.wrap(buffer), valueType);
              if (response instanceof SuccessResponse<?> success) {
                return valueType.cast(success.value());
              }
              if (response instanceof ErrorResponse error) {
                throw SeriputClientException.from(error);
              }
              throw new IllegalStateException("Unknown response type: " + response);
            });
  }

  @Override
  public <T> CompletableFuture<Void> put(String key, T value) {
    if (isClosed.get()) {
      throw new IllegalStateException("SeriputClient is closed!");
    }
    ByteBuffer payload = requestSerializer.serializePut(key, value);
    return connectionPool
        .enqueue(payload, () -> allocator.release(payload))
        .thenApply(
            buffer -> {
              var response = responseSerializer.deserialize(ByteBuffer.wrap(buffer), null);
              if (response instanceof SuccessResponse<?>) {
                return null;
              }
              if (response instanceof ErrorResponse error) {
                throw SeriputClientException.from(error);
              }
              throw new IllegalStateException("Unknown response type: " + response);
            });
  }

  @Override
  public CompletableFuture<Void> delete(String key) {
    if (isClosed.get()) {
      throw new IllegalStateException("SeriputClient is closed!");
    }
    ByteBuffer payload = requestSerializer.serializeDelete(key);
    return connectionPool
        .enqueue(payload, () -> allocator.release(payload))
        .thenApply(
            buffer -> {
              var response = responseSerializer.deserialize(ByteBuffer.wrap(buffer), null);
              if (response instanceof SuccessResponse<?>) {
                return null;
              }
              if (response instanceof ErrorResponse error) {
                throw SeriputClientException.from(error);
              }
              throw new IllegalStateException("Unknown response type: " + response);
            });
  }
}
