package io.seriput.core;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple thread-safe generic cache implementation using {@code ConcurrentHashMap} under the hood.
 *
 * @param <K> type of keys maintained by the cache
 * @param <V> type of values
 */
final class CacheImpl <K, V> implements Cache<K, V> {
  private static final ConcurrentHashMap<Object, Object> cache = new ConcurrentHashMap<>();

  @Override
  public void put(K key, V value) {
    Objects.requireNonNull(key, "key may not be null!");
    Objects.requireNonNull(value, "value may not be null!");
    cache.put(key, value);
  }

  @Override
  @SuppressWarnings("unchecked")
  public V get(K key) {
    Objects.requireNonNull(key, "key may not be null!");
    return (V) cache.get(key);
  }

  @Override
  public boolean remove(K key) {
    Objects.requireNonNull(key, "key may not be null!");
    return cache.remove(key) != null;
  }

  @Override
  public boolean containsKey(K key) {
    if(key == null) {
      return false;
    }
    return cache.containsKey(key);
  }

  @Override
  public int size() {
    return cache.size();
  }

  @Override
  public void clear() {
    cache.clear();
  }
}
