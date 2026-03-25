package io.seriput.client;

import java.util.concurrent.CompletableFuture;

/**
 * A client interface for interacting with Seriput key-value store.
 *
 * @author o.kayabasi@outlook.com
 */
public interface SeriputClient extends AutoCloseable {
  /**
   * Retrieves the value associated with the given key from Seriput key-value store.
   *
   * @param key key whose associated value is to be returned
   * @param valueType class instance of the value type
   * @return a {@link CompletableFuture} that, when completed, will contain the value associated
   *     with the specified key
   * @param <T> type of the value
   */
  <T> CompletableFuture<T> get(String key, Class<T> valueType);

  /**
   * Associates the given value with given key in Seriput key-value store.
   *
   * @param key key with which the given value is to be associated
   * @param value value to be associated with the given key
   * @return a {@link CompletableFuture} that, when completed, indicates that the operation has
   *     finished
   * @param <T> type of the value
   */
  <T> CompletableFuture<Void> put(String key, T value);

  /**
   * Deletes the value associated with the given key from Seriput key-value store.
   *
   * @param key key whose associated value is to be deleted
   * @return a {@link CompletableFuture} that, when completed, indicates that the operation has
   *     finished
   */
  CompletableFuture<Void> delete(String key);

  static SeriputClientImpl.SeriputClientImplBuilder builder(String host, int port) {
    return SeriputClientImpl.builder().host(host).port(port);
  }
}
