package io.seriput;

/**
 * A simple generic cache interface.
 *
 * @param <K> type of keys maintained by the cache
 * @param <V> type of values
 */
public interface Cache<K, V> {
  /**
   * Puts a key-value pair into the cache.
   *
   * @param key   the key
   * @param value the value
   */
  void put(K key, V value);

  /**
   * Returns a value from the cache by {@code key}.
   *
   * @param key the key
   * @return value associated with the key, or {@code null} if not found
   */
  V get(K key);

  /**
   * Removes key-value pair from the cache by {@code key}.
   *
   * @param key the key
   * @return {@code true} if the key was found and removed, {@code false} otherwise
   */
  boolean remove(K key);

  /**
   * Returns whether the cache contains a value for the given {@code key}.
   *
   * @param key the key
   * @return {@code true} if the cache contains a value for the key, {@code false} otherwise
   */
  boolean containsKey(K key);

  /**
   * Returns the number of key-value pairs in the cache.
   *
   * @return size of the cache
   */
  int size();
}
