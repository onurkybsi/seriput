package io.seriput.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Seriput server that manages the Seriput connections and the event loop.
 */
final class SeriputServer implements AutoCloseable {
  private static final Logger logger = LogManager.getLogger(SeriputServer.class);

  // region Fields
  private final ServerSocketChannel channel;
  private final HashMap<SeriputClient, Collection<SeriputConnection>> connections = new HashMap<>();
  private final int port;
  private final RequestHandler requestHandler;
  private final Selector selector;
  private final Thread serverThread = new Thread(this::startEventLoop, "server");
  private final AtomicReference<State> state = new AtomicReference<>(State.READY);
  // endregion

  SeriputServer(int port, RequestHandler requestHandler) throws IOException {
    this.channel = ServerSocketChannel.open();
    this.port = port;
    this.requestHandler = requestHandler;
    this.selector = Selector.open();
  }

  @Override
  public void close() throws Exception {
    if (!this.state.compareAndSet(State.RUNNING, State.CLOSING)) {
      return;
    }
    this.selector.wakeup();
    if (!this.serverThread.join(Duration.ofSeconds(5))) { // Wait for connections to be closed...
      logger.warn("Server thread could not be terminated in 5 seconds!");
    }
  }

  /**
   * Starts the event loop if it's ready to start.
   *
   * @throws IOException if {@code this} server's state is other than {@code READY}
   */
  public void start() throws IOException {
    if (!this.state.compareAndSet(State.READY, State.RUNNING)) {
      throw new IllegalStateException("Server isn't ready to start!");
    }
    this.channel.configureBlocking(false);
    this.channel.register(this.selector, SelectionKey.OP_ACCEPT);
    this.channel.socket().bind(new InetSocketAddress(port));
    this.serverThread.setDaemon(false);
    this.serverThread.start();
  }

  /**
   * Blocks the current thread until {@code this} server gets closed.
   *
   * @throws InterruptedException if the current thread is interrupted
   */
  public void awaitShutdown() throws InterruptedException {
    this.serverThread.join();
  }

  // region Getter & setters
  // Visible for testing
  Map<SeriputClient, Collection<SeriputConnection>> connections() {
    return this.connections;
  }

  // Visible for testing
  Selector selector() {
    return this.selector;
  }

  // Visible for testing
  Thread serverThread() {
    return this.serverThread;
  }

  State state() {
    return this.state.get();
  }
  // endregion

  private void startEventLoop() {
    logger.info("Event loop is being started on {}...", this.port);
    while (State.RUNNING.equals(this.state.get())) {
      try {
        select();
        maybeClose();
        setWriteInterest();
      } catch (IOException e) {
        logger.error("Exception occurred when selecting keys!", e);
      }
    }

    logger.info("Event loop is being stopped...");
    closeConnections();
    try {
      this.channel.close();
      this.selector.close();
    } catch (IOException e) {
      logger.warn("Exception occurred during stopping the event loop!", e);
    } finally {
      this.state.compareAndSet(State.CLOSING, State.CLOSED);
      logger.info("Event loop stopped!");
    }
  }

  private void select() throws IOException {
    this.selector.select(key -> {
      try {
        if (!key.isValid()) {
          return;
        }
        if (key.isAcceptable()) {
          accept(key);
        }
        if (key.isReadable() && key.attachment() instanceof SeriputConnection) {
          ((SeriputConnection) key.attachment()).read();
        }
        if (key.isWritable() && key.attachment() instanceof SeriputConnection) {
          ((SeriputConnection) key.attachment()).write();
        }
      } catch (Exception e) {
        logger.error("Exception occurred when processing key: {}", e.getMessage(), e);
      }
    });
  }

  private void accept(SelectionKey key) throws IOException {
    ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
    SocketChannel connection = serverChannel.accept();
    if (connection == null) {
      return;
    }

    var client = SeriputClient.from(connection);
    var clientConnections = this.connections.getOrDefault(client, new HashSet<>());
    var seriputConnection = new SeriputConnection(client, clientConnections.size(), connection, requestHandler, this.selector);
    clientConnections.add(seriputConnection);
    connection.configureBlocking(false);
    connection.register(this.selector, SelectionKey.OP_READ, seriputConnection);
    this.connections.put(client, clientConnections);
    logger.info("Connection accepted: {}", client);
  }

  private void maybeClose() throws IOException {
    for (SelectionKey key : this.selector.keys()) {
      if (!key.isValid())
        continue;
      if (!(key.attachment() instanceof SeriputConnection seriputConnection))
        continue;
      if (SeriputConnection.State.CLOSING.equals(seriputConnection.state())) {
        try {
          key.cancel();
          seriputConnection.workerThread().interrupt();
          seriputConnection.connection().close();
        } catch (IOException e) {
          logger.error("Exception occurred during closing the connection {}!", seriputConnection.connection(), e);
        } finally {
          seriputConnection.state(SeriputConnection.State.CLOSED);
          this.connections.get(seriputConnection.client()).remove(seriputConnection);
          if (this.connections.get(seriputConnection.client()).isEmpty()) {
            this.connections.remove(seriputConnection.client());
          }
        }
      }
    }
  }

  private void setWriteInterest() {
    for (SelectionKey key : selector.keys()) {
      if (!key.isValid())
        continue;
      if (!(key.attachment() instanceof SeriputConnection connection))
        continue;
      if (connection.isReadyToWrite()) {
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
      } else {
        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
      }
    }
  }

  private void closeConnections() {
    for (SelectionKey key : this.selector.keys()) {
      if (!key.isValid())
        continue;
      if (!(key.attachment() instanceof SeriputConnection seriputConnection))
        continue;
      try {
        seriputConnection.state(SeriputConnection.State.CLOSING);
        key.cancel();
        seriputConnection.workerThread().interrupt();
        // Wait for the worker thread to finish, should it?
        seriputConnection.workerThread().join(Duration.ofSeconds(1));
        seriputConnection.connection().close();
      } catch (InterruptedException | IOException e) {
        logger.warn("Exception occurred during closing the connection {}!", seriputConnection.connection(), e);
      } finally {
        seriputConnection.state(SeriputConnection.State.CLOSED);
        this.connections.get(seriputConnection.client()).remove(seriputConnection);
      }
    }
  }

  enum State {
    READY,
    RUNNING,
    CLOSING,
    CLOSED,
  }
}
