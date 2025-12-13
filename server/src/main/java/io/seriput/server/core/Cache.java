package io.seriput.server.core;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple thread-safe cache implementation using {@code ConcurrentHashMap} under the hood.
 */
public final class Cache {
  private static final ConcurrentHashMap<Key, Value> cache = new ConcurrentHashMap<>();

  public Value get(Key key) {
    Objects.requireNonNull(key, "key may not be null!");
    return cache.get(key);
  }

  public void put(Key key, Value value) {
    Objects.requireNonNull(key, "key may not be null!");
    Objects.requireNonNull(value, "value may not be null!");
    cache.put(key, value);
  }

  public Value delete(Key key) {
    Objects.requireNonNull(key, "key may not be null!");
    return cache.remove(key);
  }
}
