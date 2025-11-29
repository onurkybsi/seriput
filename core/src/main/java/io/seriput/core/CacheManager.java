package io.seriput.core;

/**
 * Manages to access the cache instance.
 */
public final class CacheManager {
  private static final Cache<Object, Object> cache = new CacheImpl<>();

  private CacheManager() {
    throw new AssertionError();
  }

  /**
   * Returns the cache instance.
   *
   * @param <K> type of keys maintained by the cache
   * @param <V> type of values
   * @return the cache instance
   */
  @SuppressWarnings("unchecked")
  public static <K, V> Cache<K, V> getInstance() {
    return (Cache<K, V>) cache;
  }
}
