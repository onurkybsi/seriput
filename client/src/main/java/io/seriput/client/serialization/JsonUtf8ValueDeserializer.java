package io.seriput.client.serialization;

import io.seriput.common.ObjectMapperProvider;
import tools.jackson.databind.ObjectMapper;

final class JsonUtf8ValueDeserializer implements ValueDeserializer {
  private static final ObjectMapper objectMapper = ObjectMapperProvider.getInstance();

  @Override
  public <T> T deserialize(byte[] bytes, Class<T> valueType) {
    return objectMapper.readValue(bytes, valueType);
  }

  @Override
  public byte typeId() {
    return 0x01;
  }
}
