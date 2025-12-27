package io.seriput.server.fixture;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Pipe;

/**
 * A {@link ByteChannel} implementation for testing one particular case which the OS send buffer is full.
 */
public final class PartialWritePipeByteChannel implements ByteChannel, AutoCloseable {
  private final int maxBytesPerWrite;
  private final Pipe.SinkChannel sink;
  private final Pipe.SourceChannel source;

  public PartialWritePipeByteChannel(int maxBytesPerWrite) throws IOException {
    this.maxBytesPerWrite = maxBytesPerWrite;
    Pipe pipe = Pipe.open();
    this.sink = pipe.sink();
    this.sink.configureBlocking(false);
    this.source = pipe.source();
    this.source.configureBlocking(false);
  }

  @Override
  public int write(ByteBuffer src) throws IOException {
    if (!src.hasRemaining()) {
      return 0;
    }

    int bytesToWrite = Math.min(src.remaining(), this.maxBytesPerWrite);
    int originalLimit = src.limit();
    src.limit(src.position() + bytesToWrite);
    ByteBuffer temp = ByteBuffer.allocate(bytesToWrite);
    temp.put(src);
    temp.flip();
    src.limit(originalLimit);

    int written = this.sink.write(temp);
    src.position(src.position() - (bytesToWrite - written));
    return written;
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
