package io.seriput.benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

final class SocketBenchmarkConnection implements BenchmarkConnection {
  private final InputStream input;
  private final OutputStream output;
  private final Socket socket;

  private SocketBenchmarkConnection(Socket socket) throws IOException {
    this.socket = socket;
    this.input = socket.getInputStream();
    this.output = socket.getOutputStream();
  }

  static SocketBenchmarkConnection from(BenchmarkWorkload workload) throws IOException {
    var socket = new Socket(); // NOSONAR
    int timeoutMillis = Math.toIntExact(workload.socketTimeout().toMillis());
    socket.setTcpNoDelay(true);
    socket.setSoTimeout(timeoutMillis);
    socket.connect(new InetSocketAddress(workload.host(), workload.port()), timeoutMillis);
    return new SocketBenchmarkConnection(socket);
  }

  @Override
  public void execute(byte[] request, ExpectedResponse expectedResponse) throws IOException {
    output.write(request);
    output.flush();
    ResponseValidator.readAndValidate(input, expectedResponse);
  }

  @Override
  public void close() throws IOException {
    socket.close();
  }
}
