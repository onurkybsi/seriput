package io.seriput.benchmark;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

final class ResponseValidator {
  private static final int HEADER_SIZE = 6;

  private ResponseValidator() {}

  static void readAndValidate(InputStream input, ExpectedResponse expected) throws IOException {
    byte[] header = readFully(input, HEADER_SIZE);
    byte status = header[0];
    byte valueType = header[1];
    int length = ByteBuffer.wrap(header, 2, Integer.BYTES).getInt();

    if (status != expected.status()) {
      throw new ProtocolException(
          "Unexpected response status: expected %d but was %d"
              .formatted(expected.status(), status));
    }
    if (valueType != expected.valueType()) {
      throw new ProtocolException(
          "Unexpected response value type: expected %d but was %d"
              .formatted(expected.valueType(), valueType));
    }
    byte[] expectedPayload = expected.payload();
    if (length != expectedPayload.length) {
      throw new ProtocolException(
          "Unexpected response length: expected %d but was %d"
              .formatted(expectedPayload.length, length));
    }
    byte[] actualPayload = readFully(input, length);
    if (!Arrays.equals(actualPayload, expectedPayload)) {
      throw new ProtocolException("Unexpected response payload");
    }
  }

  private static byte[] readFully(InputStream input, int length) throws IOException {
    byte[] bytes = new byte[length];
    int offset = 0;
    while (offset < length) {
      int read = input.read(bytes, offset, length - offset);
      if (read == -1) {
        throw new EOFException(
            "Connection closed after %d of %d response bytes".formatted(offset, length));
      }
      offset += read;
    }
    return bytes;
  }
}
