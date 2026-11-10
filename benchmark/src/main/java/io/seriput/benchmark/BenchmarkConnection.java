package io.seriput.benchmark;

import java.io.IOException;

interface BenchmarkConnection extends AutoCloseable {
  void execute(byte[] request, ExpectedResponse expectedResponse) throws IOException;

  @Override
  void close() throws IOException;
}
