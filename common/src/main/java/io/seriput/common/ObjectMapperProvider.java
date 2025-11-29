package io.seriput.common;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Provider for the common {@link ObjectMapper} instance.
 */
public final class ObjectMapperProvider {
  private static final ObjectMapper instance = JsonMapper
    .builder()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
    .build();

  private ObjectMapperProvider() {}

  public static ObjectMapper getInstance() {
    return instance;
  }
}
