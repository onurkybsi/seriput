package io.seriput.server.core;

/**
 * Represents a key stored in the key-value store.
 *
 * <p>A key can operate in two modes:
 *
 * <ul>
 *   <li><b>Owning mode</b> — created via {@link #Key(KeyType, byte[])}, the key owns the entire
 *       byte array. The hash code is computed eagerly at construction.
 *   <li><b>View mode</b> — created via {@link #view(KeyType, byte[], int, int)}, the key references
 *       a slice of a shared byte array without copying. The hash code is computed lazily on first
 *       access. Suitable for transient lookups (GET, DELETE) where the key is not stored.
 * </ul>
 */
public final class Key {
  private final KeyType type;
  private final byte[] bytes;
  private final int offset;
  private final int length;
  private int hash;

  public Key(KeyType type, byte[] bytes, int offset, int length) {
    this.type = type;
    this.bytes = bytes;
    this.offset = offset;
    this.length = length;
  }

  public Key(KeyType type, byte[] bytes) {
    this(type, bytes, 0, bytes.length);
    this.hash = computeHashCode();
  }

  /**
   * Creates a view key that references a slice of the given byte array without copying.
   *
   * <p>The caller must ensure the backing array is not mutated while this key is in use.
   */
  public static Key view(KeyType type, byte[] bytes, int offset, int length) {
    return new Key(type, bytes, offset, length);
  }

  public KeyType type() {
    return type;
  }

  public byte[] bytes() {
    return bytes;
  }

  public int offset() {
    return offset;
  }

  public int length() {
    return length;
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
    int h = this.hash;
    if (h == 0) {
      h = computeHashCode();
      this.hash = h;
    }
    return h;
  }

  private int computeHashCode() {
    int h = 1;
    for (int i = offset; i < offset + length; i++) {
      h = 31 * h + bytes[i];
    }
    return 31 * type.hashCode() + h;
  }
}
