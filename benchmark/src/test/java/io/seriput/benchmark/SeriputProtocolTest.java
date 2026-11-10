package io.seriput.benchmark;

import static io.seriput.common.serialization.response.ResponseStatus.OK;
import static io.seriput.common.serialization.response.ResponseValueType.JSON_UTF8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class SeriputProtocolTest {
  private static final String KEY = "seriput-benchmark-key";
  private static final byte[] PAYLOAD = "\"x\"".getBytes(StandardCharsets.UTF_8);

  @Nested
  final class Frames {
    @Test
    void should_SerializeGetFrame_When_KeyIsProvided() {
      // given
      byte[] key = KEY.getBytes(StandardCharsets.UTF_8);
      ByteBuffer expected = ByteBuffer.allocate(11 + key.length);
      expected.put((byte) 0x01);
      expected.put((byte) 0x01);
      expected.put((byte) 0x01);
      expected.putInt(key.length);
      expected.putInt(0);
      expected.put(key);

      // when
      byte[] actual = ProtocolFrames.get(KEY);

      // then
      assertThat(actual).isEqualTo(expected.array());
    }

    @Test
    void should_SerializePrepopulationFrame_When_KeyAndValueAreProvided() {
      // given
      byte[] key = KEY.getBytes(StandardCharsets.UTF_8);
      ByteBuffer expected = ByteBuffer.allocate(11 + key.length + PAYLOAD.length);
      expected.put((byte) 0x02);
      expected.put((byte) 0x01);
      expected.put((byte) 0x01);
      expected.putInt(key.length);
      expected.putInt(PAYLOAD.length);
      expected.put(key);
      expected.put(PAYLOAD);

      // when
      byte[] actual = ProtocolFrames.put(KEY, "x");

      // then
      assertThat(actual).isEqualTo(expected.array());
    }
  }

  @Nested
  final class ResponseValidation {
    private final ExpectedResponse expected =
        new ExpectedResponse(OK.status(), JSON_UTF8.typeId(), PAYLOAD);

    @Test
    void should_ReadCompleteResponse_When_HeaderAndBodyArrivePartially() throws Exception {
      // given
      var input = new ChunkedInputStream(response(OK.status(), JSON_UTF8.typeId(), PAYLOAD), 1);

      // when
      ResponseValidator.readAndValidate(input, expected);

      // then
      assertThat(input.available()).isZero();
    }

    @Test
    void should_RejectResponse_When_StatusIsUnexpected() {
      // given
      var input = new ByteArrayInputStream(response((byte) 0x02, JSON_UTF8.typeId(), PAYLOAD));

      // when & then
      assertThatThrownBy(() -> ResponseValidator.readAndValidate(input, expected))
          .isInstanceOf(ProtocolException.class)
          .hasMessage("Unexpected response status: expected 0 but was 2");
    }

    @Test
    void should_RejectResponse_When_ValueTypeIsUnexpected() {
      // given
      var input = new ByteArrayInputStream(response(OK.status(), (byte) 0x00, PAYLOAD));

      // when & then
      assertThatThrownBy(() -> ResponseValidator.readAndValidate(input, expected))
          .isInstanceOf(ProtocolException.class)
          .hasMessage("Unexpected response value type: expected 1 but was 0");
    }

    @Test
    void should_RejectResponse_When_DeclaredLengthIsUnexpected() {
      // given
      byte[] response = response(OK.status(), JSON_UTF8.typeId(), PAYLOAD);
      ByteBuffer.wrap(response, 2, Integer.BYTES).putInt(PAYLOAD.length + 1);
      var input = new ByteArrayInputStream(response);

      // when & then
      assertThatThrownBy(() -> ResponseValidator.readAndValidate(input, expected))
          .isInstanceOf(ProtocolException.class)
          .hasMessage("Unexpected response length: expected 3 but was 4");
    }

    @Test
    void should_RejectResponse_When_PayloadIsUnexpected() {
      // given
      byte[] unexpected = "\"y\"".getBytes(StandardCharsets.UTF_8);
      var input = new ByteArrayInputStream(response(OK.status(), JSON_UTF8.typeId(), unexpected));

      // when & then
      assertThatThrownBy(() -> ResponseValidator.readAndValidate(input, expected))
          .isInstanceOf(ProtocolException.class)
          .hasMessage("Unexpected response payload");
    }
  }

  private static byte[] response(byte status, byte valueType, byte[] payload) {
    return ByteBuffer.allocate(6 + payload.length)
        .put(status)
        .put(valueType)
        .putInt(payload.length)
        .put(payload)
        .array();
  }

  private static final class ChunkedInputStream extends ByteArrayInputStream {
    private final int chunkSize;

    private ChunkedInputStream(byte[] buffer, int chunkSize) {
      super(buffer);
      this.chunkSize = chunkSize;
    }

    @Override
    public synchronized int read(byte[] bytes, int offset, int length) {
      return super.read(bytes, offset, Math.min(length, chunkSize));
    }
  }
}
