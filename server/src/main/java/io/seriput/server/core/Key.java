package io.seriput.server.core;

/**
 * Represents a key stored in the key-value store.
 *
 * <p>A key can operate in two modes:
 *
 * <ul>
 *   <li><b>Owning mode</b> — created via {@link #Key(KeyType, byte[])}, the key owns the entire
 *       byte array.
 *   <li><b>View mode</b> — created via {@link #view(KeyType, byte[], int, int)}, the key references
 *       a slice of a shared byte array without copying. Suitable for transient lookups (GET,
 *       DELETE) where the key is not stored.
 * </ul>
 *
 * @param type type of the key
 * @param bytes raw bytes backing the key
 * @param offset start offset within {@code bytes}
 * @param length number of bytes representing the key
 */
public record Key(KeyType type, byte[] bytes, int offset, int length) {
  public Key(KeyType type, byte[] bytes) {
    this(type, bytes, 0, bytes.length);
  }

  /**
   * Creates a view key that references a slice of the given byte array without copying.
   *
   * <p>The caller must ensure the backing array is not mutated while this key is in use.
   */
  public static Key view(KeyType type, byte[] bytes, int offset, int length) {
    return new Key(type, bytes, offset, length);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Key key = (Key) o;
    if (type != key.type || length != key.length) return false;
    for (int i = 0; i < length; i++) {
      if (bytes[offset + i] != key.bytes[key.offset + i]) return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int h = 1;
    for (int i = offset; i < offset + length; i++) {
      h = 31 * h + bytes[i];
    }
    return 31 * type.hashCode() + h;
  }
}
