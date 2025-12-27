package io.seriput.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.seriput.server.serialization.request.RequestDeserializer.bodySize;
import static io.seriput.server.serialization.request.RequestDeserializer.headerSize;

/**
 * Manages a single Seriput connection lifecycle.
 * <p>
 * Note that this class is NOT thread-safe, and it is designed to be used within the event loop.
 */
final class SeriputConnection {
  private static final Logger logger = LogManager.getLogger(SeriputConnection.class.getName());

  // region Fields
  private final SeriputClient client;
  private final int clientConnectionIx;
  private final ByteChannel connection;
  private final BlockingQueue<byte[]> inboundQueue = new LinkedBlockingQueue<>();
  private final AtomicBoolean isReadyToWrite = new AtomicBoolean(false);
  private final Queue<ByteBuffer> outboundQueue = new ConcurrentLinkedQueue<>();
  // TODO: 8KB buffer, make configurable
  private final ByteBuffer readBuffer = ByteBuffer.allocate(8192);
  private final RequestHandler requestHandler;
  private final Selector selector;
  private final AtomicReference<State> state = new AtomicReference<>(State.OPEN);
  private final Thread workerThread;
  // endregion

  SeriputConnection(SeriputClient client, int clientConnectionIx, ByteChannel connection, RequestHandler requestHandler, Selector selector) {
    this.client = client;
    this.clientConnectionIx = clientConnectionIx;
    this.connection = connection;
    this.requestHandler = requestHandler;
    this.selector = selector;
    this.workerThread = this.startWorkerThread();
  }

  // region Getter & setters
  SeriputClient client() {
   return this.client;
  }

  ByteChannel connection() {
    return this.connection;
  }

  State state() {
    return this.state.get();
  }

  void state(State state) {
    this.state.set(state);
  }

  Thread workerThread() {
    return this.workerThread;
  }
  // endregion

  /**
   * Reads as much as data available from the connection open with the client.
   */
  void read() {
    if (this.state.get() != State.OPEN) {
      throw new IllegalStateException("Unexpected connection state: " + this.state.get());
    }
    if (!doRead())
      return;
    maybeDispatch();
    // TODO: Add readBuffer capacity check!
  }

  /**
   * Writes the pending responses to the connection open with the client.
   */
  void write() {
    if (!State.OPEN.equals(this.state.get())) {
      throw new IllegalStateException("Unexpected connection state: " + this.state.get());
    }
    try {
      ByteBuffer response;
      while ((response = this.outboundQueue.peek()) != null) {
        this.connection.write(response);
        if (response.hasRemaining()) { // OS send buffer is full, stop writing but keep OP_WRITE enabled
          return;
        }
        this.outboundQueue.remove();
      }
      // No more buffers left to write, tell the event loop, no interest to write!
      this.isReadyToWrite.compareAndSet(true, false);
    } catch (IOException e) {
      if (e instanceof ClosedChannelException) {
        logger.warn("Client closed the connection! Outbound queue size: {}", this.outboundQueue.size());
      } else {
        logger.error("Exception occurred during writing: {}", e.getMessage(), e);
      }
      this.state.compareAndSet(State.OPEN, State.CLOSING); // SeriputServer is going to close the connection
    }
  }

  /**
   * Returns whether the connection is ready and needs to write.
   *
   * @return {@code true} if ready to write, {@code false} otherwise
   */
  boolean isReadyToWrite() {
    return State.OPEN.equals(this.state.get()) && this.isReadyToWrite.get() && !this.outboundQueue.isEmpty();
  }

  private boolean doRead() {
    try {
      int read = this.connection.read(this.readBuffer);
      if (read == -1) {
        this.state.compareAndSet(State.OPEN, State.CLOSING);
        return false;
      }
      return true;
    } catch (IOException e) {
      logger.error("Exception occurred during reading: {}", e.getMessage(), e);
      this.state.compareAndSet(State.OPEN, State.CLOSING);
      return false;
    }
  }

  private void maybeDispatch() {
    this.readBuffer.flip(); // Switch to read mode!
    // Dispatch as much as frame possible
    while (true) {
      if (this.readBuffer.remaining() < headerSize()) {
        break;
      }

      int bodySize = bodySize(this.readBuffer);
      int frameSize = headerSize() + bodySize;
      if (this.readBuffer.remaining() < frameSize) {
        break;
      }

      // Extract the next frame from readBuffer
      int frameStart = this.readBuffer.position();
      int frameEnd = frameStart + frameSize;
      ByteBuffer view = this.readBuffer.duplicate(); // This isn't full copy!
      view.position(frameStart);
      view.limit(frameEnd);
      byte[] frame = new byte[frameSize];
      view.get(frame);
      this.inboundQueue.add(frame); // Full request

      // Set to the next frame's start.
      this.readBuffer.position(frameEnd);
    }
    // Move any remaining bytes to the front, switch back to write mode
    this.readBuffer.compact();
  }

  private Thread startWorkerThread() {
    return Thread.ofVirtual().name(workerThreadName(this.client, this.clientConnectionIx)).start(() -> {
      while (State.OPEN.equals(this.state.get())) {
        try {
          byte[] request = this.inboundQueue.take();
          ByteBuffer response = this.requestHandler.handle(request);
          this.outboundQueue.add(response);
          if (this.isReadyToWrite.compareAndSet(false, true)) { // In order to prevent unnecessary selector.wakeup() calls
            this.selector.wakeup();
          }
        } catch (Exception e) {
          if (e instanceof InterruptedException && State.CLOSING.equals(this.state.get())) {
            break; // Exit gracefully
          }
          logger.error("Exception occurred during handling the request!", e);
        }
      }
      logger.info("Worker thread is exiting, size of the inbound queue: {}", this.inboundQueue.size());
    });
  }

  private static String workerThreadName(SeriputClient client, int clientConnectionIx) {
    return "seriput-worker[client=%s:%s,conn=%s]".formatted(client.address().getHostName(), client.port(), clientConnectionIx);
  }

  enum State {
    OPEN,
    CLOSING,
    CLOSED
  }
}
