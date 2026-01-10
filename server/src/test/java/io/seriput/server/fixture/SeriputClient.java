package io.seriput.server.fixture;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public final class SeriputClient implements Closeable {
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

  @Override
  public void close() throws IOException {
    this.socket.close();
  }
}
