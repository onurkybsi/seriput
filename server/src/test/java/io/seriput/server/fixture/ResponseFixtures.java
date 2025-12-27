package io.seriput.server.fixture;

import com.google.common.primitives.Bytes;
import io.seriput.common.serialization.response.Response;
import io.seriput.common.serialization.response.ResponseDeserializer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public final class ResponseFixtures {
  private static final ResponseDeserializer responseDeserializer = ResponseDeserializer.build();

  public static Response<?>[] deserialize(InputStream inputStream, int expectedLength, Class<?> valueType) throws IOException {
    Response<?>[] responses = new Response[expectedLength];
    int i = 0;
    while (i < expectedLength) {
      byte[] header = inputStream.readNBytes(ResponseDeserializer.headerSize());
      int bodySize = ResponseDeserializer.bodySize(ByteBuffer.wrap(header));
      byte[] body = inputStream.readNBytes(bodySize);
      responses[i] = responseDeserializer.deserialize(ByteBuffer.wrap(Bytes.concat(header, body)), valueType);
      i++;
    }
    return responses;
  }
}
