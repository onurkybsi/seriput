package io.seriput.common.serialization.request;

import io.seriput.common.ObjectMapperProvider;
import java.util.Objects;
import tools.jackson.databind.ObjectMapper;

final class JsonUtf8ValueSerializer<T> implements ValueSerializer<T> {
  private static final ObjectMapper objectMapper = ObjectMapperProvider.getInstance();

  @Override
  public byte[] serialize(T value) {
    Objects.requireNonNull(value, "value may not be null!");
    return objectMapper.writeValueAsBytes(value);
  }

  @Override
  public byte typeId() {
    return 0x01;
  }
}
