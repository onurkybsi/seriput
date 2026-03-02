package io.seriput.benchmark;

import io.seriput.common.serialization.request.KeyType;
import io.seriput.common.serialization.request.RequestSerializer;
import io.seriput.common.serialization.request.ValueType;
import io.seriput.common.serialization.response.Response;
import io.seriput.common.serialization.response.ResponseDeserializer;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public final class SeriputClient implements Closeable {
  public static int HEADER_SIZE = 6;
  public static int VALUE_LENGTH_OFFSET = 2;

  private static final RequestSerializer<String, Object> serializer = RequestSerializer.build(KeyType.UTF8, ValueType.JSON_UTF8);
  private static final ResponseDeserializer deserializer = ResponseDeserializer.build();

  private final String host;
  private final int port;
  private final Socket socket = new Socket();

  private SeriputClient(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public static SeriputClient of(String host, int port) {
    return new SeriputClient(host, port);
  }

  @Override
  public void close() throws IOException {
    this.socket.close();
  }

  public int localPort() {
    return this.socket.getLocalPort();
  }

  public boolean tryToConnect() {
    if (this.socket.isConnected()) {
      return true;
    }
    try {
      this.socket.connect(new InetSocketAddress(this.host, this.port));
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  public boolean isConnected() {
    return this.socket.isConnected();
  }

  public void write(byte[] bytes) throws IOException {
    if (!this.socket.isConnected()) {
      throw new IllegalStateException("Client is not connected yet!");
    }
    this.socket.getOutputStream().write(bytes);
  }

  public int available() throws IOException {
    return this.socket.getInputStream().available();
  }

  public ByteBuffer read(int bytes) throws IOException {
    return ByteBuffer.wrap(this.socket.getInputStream().readNBytes(bytes));
  }

  public <T> Response<?> get(String key, Class<T> valueType) throws IOException {
    ByteBuffer request = serializer.serializeGet(key);
    byte[] requestBytes = new byte[request.remaining()];
    request.get(requestBytes);
    write(requestBytes);
    return readResponse(valueType);
  }

  public Response<?> put(String key, Object value) throws IOException {
    ByteBuffer request = serializer.serializePut(key, value);
    byte[] requestBytes = new byte[request.remaining()];
    request.get(requestBytes);
    write(requestBytes);
    return readResponse(null);
  }

  public Response<?> delete(String key) throws IOException {
    ByteBuffer request = serializer.serializeDelete(key);
    byte[] requestBytes = new byte[request.remaining()];
    request.get(requestBytes);
    write(requestBytes);
    return readResponse(null);
  }

  private Response<?> readResponse(Class<?> valueType) throws IOException {
    ByteBuffer responseHeader = read(HEADER_SIZE);
    int length = responseHeader.getInt(VALUE_LENGTH_OFFSET);
    ByteBuffer responseBody = read(length);
    ByteBuffer response = ByteBuffer.allocate(responseHeader.remaining() + responseBody.remaining());
    response.put(responseHeader);
    response.put(responseBody);
    response.flip();
    return deserializer.deserialize(response, valueType);
  }
}
