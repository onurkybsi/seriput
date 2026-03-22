package io.seriput.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

final class SeriputConnectionPool implements AutoCloseable {
  private static final Logger logger = LogManager.getLogger(SeriputConnectionPool.class.getName());

  private final ExecutorService callbackExecutor;
  private final ArrayList<SeriputConnection> connectionPool = new ArrayList<>();
  private final AtomicBoolean isRunning = new AtomicBoolean(false);
  private final int maxOutboundQueueSize;
  // The pending requests to be dispatched to SeriputConnections.
  private final ConcurrentLinkedQueue<PendingRequest> outboundQueue = new ConcurrentLinkedQueue<>();
  private final Thread poolThread;
  private final Selector selector;

  SeriputConnectionPool(String host, int port, int poolSize, ExecutorService callbackExecutor,
                         int readBufferSize, int maxOutboundQueueSize) throws IOException {
    this.callbackExecutor = callbackExecutor;
    this.maxOutboundQueueSize = maxOutboundQueueSize;
    this.poolThread = new Thread(this::startEventLoop, "seriput-connection-pool");
    this.poolThread.setDaemon(false);
    this.selector = Selector.open();
    for (int i = 0; i < poolSize; i++) {
      this.connectionPool.add(new SeriputConnection(host, port, callbackExecutor, this.selector, readBufferSize));
    }
  }

  @Override
  public void close() throws Exception {
    if (this.isRunning.compareAndSet(true, false)) {
      logger.debug("Seriput connection pool is being shut down...");
      this.clearOutboundQueue();
      this.connectionPool.forEach(conn -> conn.state(SeriputConnection.State.CLOSING));
      this.selector.wakeup();
      this.selector.close();
      logger.debug("Waiting for connection pool thread to shut down...");
      this.poolThread.join(1_000);
      this.callbackExecutor.shutdown();
      logger.info("Seriput connection pool was shut down!");
    }
  }

  // Starts the connection pool's event loop.
  void start()  {
    if (this.isRunning.compareAndSet(false, true)) {
      this.poolThread.start();
      logger.info("Seriput connection pool started with {} connections!", this.connectionPool.size());
    }
  }

  // Enqueue a request and return a future that will be completed with the response.
  CompletableFuture<byte[]> enqueue(ByteBuffer request, Runnable onRequestSent) {
    CompletableFuture<byte[]> future = new CompletableFuture<>();
    if (!this.isRunning.get()) {
      future.completeExceptionally(new IllegalStateException("Connection pool is not running!"));
      return future;
    }
    this.outboundQueue.offer(new PendingRequest(request, onRequestSent, future));
    this.selector.wakeup();
    return future;
  }

  private void startEventLoop() {
    while (this.isRunning.get()) {
      select();
      maybeClose();
      maybeRemoveWriteInterest();
      dispatch();
    }
  }

  private void select() {
    try {
      this.selector.select(selectionKey -> {
        if (!selectionKey.isValid() || !(selectionKey.attachment() instanceof SeriputConnection conn)) {
          return;
        }

        if (selectionKey.isWritable()) {
          conn.write();
        }
        if (selectionKey.isReadable()) {
          conn.read();
        }
      });
    } catch (ClosedSelectorException e) {
      logger.debug("Selector was closed...", e);
    } catch (IOException e) {
      logger.error("Exception occurred when selecting on the selector!", e);
    }
  }

  private void maybeClose() {
    Iterator<SeriputConnection> it = connectionPool.iterator();
    while (it.hasNext()) {
      SeriputConnection conn = it.next();
      if (!SeriputConnection.State.CLOSING.equals(conn.state())) {
        continue;
      }

      try {
        logger.debug("Closing connection {}...", conn);
        conn.close();
      } catch (IOException e) {
        logger.warn("Exception occurred when closing connection {}", conn, e);
      } finally {
        conn.state(SeriputConnection.State.CLOSED);
        it.remove();
        logger.debug("Connection {} closed!", conn);
      }
    }
  }

  private void maybeRemoveWriteInterest() {
    if (this.isRunning.get()) {
      for(var conn : this.connectionPool) {
        if (conn.pendingRequests().isEmpty()) {
          conn.maybeSetWriteInterest(false);
        }
      }
    }
  }

  private void dispatch() {
    if (this.isRunning.get()) {
      PendingRequest pendingRequest;
      while ((pendingRequest = this.outboundQueue.poll()) != null) {
        SeriputConnection conn = next();
        if (conn == null || conn.pendingRequests().size() >= this.maxOutboundQueueSize) {
          this.outboundQueue.offer(pendingRequest); // Re-enqueue the request if no connection is available
          return;
        }
        conn.enqueue(pendingRequest);
        conn.maybeSetWriteInterest(true);
      }
    }
  }

  private SeriputConnection next() {
    return this.connectionPool
      .stream()
      .filter(c -> SeriputConnection.State.OPEN.equals(c.state()))
      .min(Comparator.comparingInt(conn -> conn.pendingRequests().size()))
      .orElse(null);
  }

  private void clearOutboundQueue() {
    int numOfPendingRequestCleared = 0;
    PendingRequest pendingRequest;
    while ((pendingRequest = this.outboundQueue.poll()) != null) { // If not yet dispatched, complete exceptionally
      final PendingRequest request = pendingRequest;
      try {
        this.callbackExecutor.execute(() ->
          request
            .onCompleted()
            .completeExceptionally(new IllegalStateException("Connection pool is shutting down!"))
        );
      } catch (RejectedExecutionException e) {
        logger.warn("Callback executor rejected the pending request, completing on the event loop's thread!", e);
        request
          .onCompleted()
          .completeExceptionally(new IllegalStateException("Connection pool is shutting down!"));
      } finally {
        numOfPendingRequestCleared++;
      }
    }
    logger.debug("Cleared {} pending requests from outbound queue...", numOfPendingRequestCleared);
  }
}
