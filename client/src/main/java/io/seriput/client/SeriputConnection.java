package io.seriput.client;

import static io.seriput.common.serialization.response.ResponseDeserializer.bodySize;
import static io.seriput.common.serialization.response.ResponseDeserializer.headerSize;

import io.seriput.client.exception.ConnectionClosedException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents a single connection to the Seriput server.
 *
 * <p>This class is <b>not thread-safe</b> and is intended to be used by the event loop thread.
 */
@Accessors(fluent = true)
final class SeriputConnection implements AutoCloseable {
  private static final Logger logger = LogManager.getLogger(SeriputConnection.class);

  private final Executor callbackExecutor;
  @Getter private final SocketChannel channel;
  @Getter private final Queue<PendingRequest> pendingRequests = new ArrayDeque<>();
  private final Queue<PendingResponse> pendingResponses = new ArrayDeque<>();
  private final ByteBuffer readBuffer;
  @Getter private final SelectionKey selectionKey;

  private boolean isWriteInterested = false;
  @Getter @Setter private State state = State.OPEN;

  SeriputConnection(
      String host, int port, Executor callbackExecutor, Selector selector, int readBufferSize)
      throws IOException {
    this.callbackExecutor = callbackExecutor;
    this.channel = SocketChannel.open();
    this.channel.configureBlocking(true);
    this.channel.connect(new InetSocketAddress(host, port));
    this.channel.configureBlocking(false);
    this.readBuffer = ByteBuffer.allocate(readBufferSize);
    this.selectionKey = this.channel.register(selector, SelectionKey.OP_READ, this);
  }

  @Override
  public void close() throws IOException {
    this.selectionKey.cancel();
    this.channel.close();

    var cause = new ConnectionClosedException("Connection closed!");
    PendingRequest req;
    while ((req = this.pendingRequests.poll()) != null) {
      req.onPayloadConsumed().run();
      completeExceptionally(req.onCompleted(), cause);
    }
    PendingResponse resp;
    while ((resp = this.pendingResponses.poll()) != null) {
      completeExceptionally(resp.onCompleted(), cause);
    }
  }

  void enqueue(PendingRequest pendingRequest) {
    this.pendingRequests.add(pendingRequest);
    maybeSetWriteInterest(true);
  }

  void maybeSetWriteInterest(boolean isWriteInterested) {
    if (this.isWriteInterested != isWriteInterested) {
      this.isWriteInterested = isWriteInterested;
      if (isWriteInterested) {
        this.selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
      } else {
        this.selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
      }
    }
  }

  void write() {
    try {
      while (!this.pendingRequests.isEmpty()) {
        PendingRequest request = this.pendingRequests.peek();
        this.channel.write(request.payload());
        if (request.payload().hasRemaining()) {
          break;
        } else {
          PendingRequest completed = this.pendingRequests.remove();
          completed.onPayloadConsumed().run();
          this.pendingResponses.add(new PendingResponse(completed.onCompleted()));
        }
      }
    } catch (IOException e) {
      logger.error("Exception occurred when writing to connection {}", this, e);
      this.state(State.CLOSING);
    }
  }

  void read() {
    try {
      int bytesRead = this.channel.read(this.readBuffer);
      if (bytesRead == -1) {
        logger.debug("Connection {} closed by server", this);
        this.state(State.CLOSING);
        return;
      }

      this.readBuffer.flip(); // Switch to read mode
      // Complete as many responses as possible
      while (!this.pendingResponses.isEmpty()) { // NOSONAR
        if (this.readBuffer.remaining() < headerSize()) {
          break;
        }
        logger.info(
            "Read buffer position: {}, remaining: {}",
            this.readBuffer.position(),
            this.readBuffer.remaining());

        int bodySize = bodySize(this.readBuffer);
        int frameSize = headerSize() + bodySize;
        if (this.readBuffer.remaining() < frameSize) {
          break;
        }

        // Extract the frame from readBuffer
        int frameStart = this.readBuffer.position();
        int frameEnd = frameStart + frameSize;
        ByteBuffer view = this.readBuffer.duplicate(); // This isn't full copy!
        view.position(frameStart);
        view.limit(frameEnd);
        byte[] frame = new byte[frameSize];
        view.get(frame);
        tryToComplete(frame);

        // Set to the next frame's start.
        this.readBuffer.position(frameEnd);
      }
      // Move any remaining bytes to the front, switch back to write mode
      this.readBuffer.compact();
    } catch (IOException e) {
      logger.error("Exception occurred when reading from connection {}", this, e);
      this.state(State.CLOSING);
    }
  }

  private void tryToComplete(byte[] responsePayload) {
    drainPendingResponses();
    var pendingResponseToSet = firstNotReadyToBeCompleted();
    if (pendingResponseToSet == null) {
      logger.warn("Response payload could not be attached to the corresponding callback!");
      return;
    }
    pendingResponseToSet.payload(responsePayload);
    drainPendingResponses();
  }

  private void drainPendingResponses() {
    while (true) {
      PendingResponse nextToComplete = this.pendingResponses.peek();
      if (nextToComplete == null || !nextToComplete.isReadyToBeCompleted()) {
        return; // Nothing to complete!
      }

      try {
        this.callbackExecutor.execute(
            () -> nextToComplete.onCompleted().complete(nextToComplete.payload()));
        this.pendingResponses.remove();
      } catch (RejectedExecutionException e) {
        logger.warn("Callback executor rejected completion; will retry later.", e);
        return;
      }
    }
  }

  private PendingResponse firstNotReadyToBeCompleted() {
    for (PendingResponse pr : this.pendingResponses) {
      if (!pr.isReadyToBeCompleted()) {
        return pr;
      }
    }
    return null;
  }

  private void completeExceptionally(CompletableFuture<?> future, Exception cause) {
    try {
      this.callbackExecutor.execute(() -> future.completeExceptionally(cause));
    } catch (RejectedExecutionException e) {
      logger.warn("Callback executor rejected; completing on the caller's thread.", e);
      future.completeExceptionally(cause);
    }
  }

  enum State {
    OPEN,
    CLOSING,
    CLOSED
  }
}
