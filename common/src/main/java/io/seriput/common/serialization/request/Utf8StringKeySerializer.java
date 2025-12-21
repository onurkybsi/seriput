package io.seriput.common.serialization.request;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

final class Utf8StringKeySerializer implements KeySerializer<String> {
  @Override
  public byte[] serialize(String key) {
    Objects.requireNonNull(key, "key may not be null!");
    return key.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public byte typeId() {
    return 0x01;
  }
}
