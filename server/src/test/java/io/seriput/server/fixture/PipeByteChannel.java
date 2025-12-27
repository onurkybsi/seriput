package io.seriput.server.fixture;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Pipe;

/**
 * A {@link ByteChannel} implementation for testing purposes.
 */
public final class PipeByteChannel implements ByteChannel, AutoCloseable {
  private final Pipe.SinkChannel sink;
  private final Pipe.SourceChannel source;

  public PipeByteChannel() throws IOException {
    Pipe pipe = Pipe.open();
    this.sink = pipe.sink();
    this.sink.configureBlocking(false);
    this.source = pipe.source();
    this.source.configureBlocking(false);
  }

  @Override
  public int write(ByteBuffer src) throws IOException {
    return this.sink.write(src);
  }

  @Override
  public int read(ByteBuffer dst) throws IOException {
    return this.source.read(dst);
  }

  @Override
  public boolean isOpen() {
    return this.sink.isOpen() && this.source.isOpen();
  }

  @Override
  public void close() throws IOException {
    this.sink.close();
    this.source.close();
  }
}
