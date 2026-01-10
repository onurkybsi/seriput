package io.seriput.server;

import io.seriput.common.serialization.request.KeyType;
import io.seriput.common.serialization.request.RequestSerializer;
import io.seriput.common.serialization.request.ValueType;
import io.seriput.server.serialization.response.ResponseSerializer;
import io.seriput.server.fixture.SeriputClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

final class SeriputServerTest {
  private static final int SERVER_PORT = 8080;
  private static final RequestSerializer<String, ObjectNode> serializer = RequestSerializer.build(KeyType.UTF8, ValueType.JSON_UTF8);

  private RequestHandler requestHandler;

  private SeriputServer underTest;

  @BeforeEach
  void setUp() throws Exception {
    this.requestHandler = mock(RequestHandler.class);
    when(requestHandler.handle(any())).thenReturn(ResponseSerializer.ok());
    this.underTest = new SeriputServer(SERVER_PORT, requestHandler);
  }

  @AfterEach
  void cleanUp() throws Exception {
    if (this.underTest != null) {
      try {
        this.underTest.close();
      } finally {
        this.underTest.awaitShutdown();
      }
    }
  }

  @Nested
  class Lifecycle {
    @Test
    void should_Transition_From_READY_To_RUNNING_On_Start() throws IOException {
      // given

      // when & then
      assertThat(underTest.state()).isEqualTo(SeriputServer.State.READY);
      underTest.start();
      assertThat(underTest.state()).isEqualTo(SeriputServer.State.RUNNING);
    }

    @ParameterizedTest
    @EnumSource(value = SeriputServer.State.class, mode = EnumSource.Mode.EXCLUDE, names = { "RUNNING" })
    void should_Throw_IllegalStateException_On_Start_When_Server_Is_Already_Running() throws IOException {
      // given
      underTest.start();

      // when & then
      assertThatThrownBy(() -> underTest.start())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Server isn't ready to start!");
    }

    @Test
    void should_Start_EventLoop_On_Server_Thread() throws IOException {
      // given

      // when
      underTest.start();

      // then
      await().untilAsserted(() -> assertThat(underTest.state()).isEqualTo(SeriputServer.State.RUNNING));
    }

    @Test
    void awaitShutdown_should_Block_Until_Server_Is_Closed() throws IOException, InterruptedException {
      // given
      long serverStart = System.currentTimeMillis();
      underTest.start();
      Thread.ofVirtual().start(() -> {
        try {
          Thread.sleep(250);
          underTest.close();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });

      // then
      underTest.awaitShutdown();

      // then
      assertThat(underTest.state()).isEqualTo(SeriputServer.State.CLOSED);
      assertThat(System.currentTimeMillis() - serverStart).isGreaterThanOrEqualTo(250);
    }
  }

  @Nested
  class ConnectionAcceptance {
    @Test
    void should_Accept_New_Connection() throws Exception {
      // given
      underTest.start();
      var client = SeriputClient.of("localhost", SERVER_PORT);

      // when & then
      await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> {
        assertThat(client.tryToConnect()).isTrue();
        assertThat(client.isConnected()).isTrue();
        var connections = underTest.connections().entrySet();
        assertThat(connections.size()).isEqualTo(1);
        var seriputClient = connections.stream().findFirst().orElseThrow();
        assertThat(seriputClient.getKey().address().getHostName()).isEqualTo("localhost");
        assertThat(seriputClient.getKey().port()).isEqualTo(client.localPort());
        assertThat(seriputClient.getValue().size()).isEqualTo(1);
      });
      client.close();
    }

    @Test
    void should_Register_New_Connection_With_OP_READ() throws IOException {
      // given
      underTest.start();
      var client = SeriputClient.of("localhost", SERVER_PORT);

      // when & then
      await().atMost(Duration.ofSeconds(1)).until(() -> {
        client.tryToConnect();
        var registered = underTest
          .selector()
          .keys()
          .stream()
          .filter(k -> k.attachment() instanceof SeriputConnection)
          .findFirst();
        if (registered.isPresent()) {
          assertThat(registered.get().isValid()).isTrue();
          assertThat(registered.get().interestOps()).isEqualTo(SelectionKey.OP_READ);
          return true;
        }
        return false;
      });
      client.close();
    }
  }

  @Nested
  class ReadingWriting {
    @Test
    void should_Read_From_SeriputConnection_When_SelectionKey_Is_Applicable() throws IOException {
      // given
      underTest.start();
      var client = SeriputClient.of("localhost", SERVER_PORT);
      when(requestHandler.handle(any())).thenReturn(ResponseSerializer.notFound());

      // when
      await().until(client::tryToConnect);
      var request = serializer.serializeGet("user:1");
      var requestBytes = new byte[request.limit()];
      request.get(requestBytes);
      client.write(requestBytes);

      // then
      await()
        .atMost(Duration.ofSeconds(1))
        .untilAsserted(() -> verify(requestHandler, times(1)).handle(requestBytes));
      client.close();
    }

    @Test
    void should_Write_Into_SeriputConnection_When_SelectionKey_Is_Applicable() throws IOException {
      // given
      underTest.start();
      var client = SeriputClient.of("localhost", SERVER_PORT);
      when(requestHandler.handle(any())).thenReturn(ResponseSerializer.notFound());

      // when
      await().until(client::tryToConnect);
      var requestBuffer = serializer.serializeGet("user:1");
      var requestBytes = new byte[requestBuffer.limit()];
      requestBuffer.get(requestBytes);
      client.write(requestBytes);

      // then
      await()
        .atMost(Duration.ofSeconds(1))
        .until(() -> {
          var expectedBuffer = ResponseSerializer.notFound();
          byte[] expectedBytes = new byte[expectedBuffer.limit()];
          if (client.available() < expectedBytes.length) {
            return false;
          }
          expectedBuffer.get(expectedBytes);

          var actualBuffer = client.read(expectedBytes.length);
          var actualBytes = new byte[actualBuffer.limit()];
          actualBuffer.get(actualBytes);
          assertThat(actualBytes).isEqualTo(expectedBytes);
          return true;
        });
      client.close();
    }
  }

  @Nested
  class WriteInterestManagement {
    @Test
    void should_Interest_OP_WRITE_When_Connection_Is_ReadyToWrite() throws IOException {
      // given
      underTest.start();
      Pipe pipe = Pipe.open();
      try(Pipe.SinkChannel channel = pipe.sink()) {
        channel.configureBlocking(false);
        SeriputConnection connection = mock(SeriputConnection.class);
        when(connection.isReadyToWrite()).thenReturn(true);
        SelectionKey key = channel.register(
          underTest.selector(),
          SelectionKey.OP_WRITE,
          connection
        );

        // when
        underTest.selector().wakeup();

        // then
        await()
          .untilAsserted(() -> assertThat(key.interestOps() & SelectionKey.OP_WRITE).isEqualTo(SelectionKey.OP_WRITE));
      }
    }

    @Test
    void should_Remove_OP_WRITE_Interest_When_Connection_Is_Not_ReadyToWrite() throws IOException {
      // given
      underTest.start();
      Pipe pipe = Pipe.open();
      try(Pipe.SinkChannel channel = pipe.sink()) {
        channel.configureBlocking(false);
        SeriputConnection connection = mock(SeriputConnection.class);
        when(connection.isReadyToWrite()).thenReturn(false);
        SelectionKey key = channel.register(
          underTest.selector(),
          SelectionKey.OP_WRITE,
          connection
        );

        // when
        underTest.selector().wakeup();

        // then
        await().untilAsserted(() -> assertThat(key.interestOps() & SelectionKey.OP_WRITE).isZero());
      }
    }
  }

  @Nested
  class ConnectionLifecycle {
    @Test
    @SuppressWarnings("resource")
    void should_Close_Connection_When_Connection_State_Is_CLOSING() throws IOException {
      // given
      underTest.start();
      var client = SeriputClient.of("localhost", SERVER_PORT);
      AtomicReference<SeriputConnection> connection = new AtomicReference<>();
      await().until(() -> {
        if (client.tryToConnect() && !underTest.connections().isEmpty()) {
          var connections = underTest.connections().values().stream().findFirst().orElseThrow();
          connection.set(connections.stream().findFirst().orElseThrow());
          connection.get().state(SeriputConnection.State.CLOSING);
          return true;
        }
        return false;
      });

      // when
      underTest.selector().wakeup();

      // then
      await().untilAsserted(() -> {
        assertThat(connection.get().state()).isEqualTo(SeriputConnection.State.CLOSED);
        assertThat(connection.get().connection().isOpen()).isFalse();
        assertThat(connection.get().workerThread().isAlive()).isFalse();
        assertThat(underTest.connections().isEmpty()).isTrue();
      });
    }

    @Test
    void should_Cancel_SelectionKey_On_Connection_Close() throws IOException {
      // given
      underTest.start();
      var client = SeriputClient.of("localhost", SERVER_PORT);
      var serverSelector = underTest.selector();
      await().until(() -> {
        if (client.tryToConnect()) {
          return serverSelector
            .keys()
            .stream()
            .anyMatch(k -> k.attachment() instanceof SeriputConnection);
        }
        return false;
      });
      var selectionKey = serverSelector
        .keys()
        .stream()
        .filter(k -> k.attachment() instanceof SeriputConnection)
        .findFirst()
        .orElseThrow();
      var connection = (SeriputConnection) selectionKey.attachment();

      // when
      connection.state(SeriputConnection.State.CLOSING);
      serverSelector.wakeup();

      // then
      await().untilAsserted(() -> assertThat(selectionKey.isValid()).isFalse());
      client.close();
    }

    @Test
    void should_Not_Close_Connection_When_Its_State_Is_OPEN() throws IOException {
      // given
      underTest.start();
      var client = SeriputClient.of("localhost", SERVER_PORT);
      var serverSelector = underTest.selector();
      await().until(() -> {
        if (client.tryToConnect()) {
          return serverSelector
            .keys()
            .stream()
            .anyMatch(k -> k.attachment() instanceof SeriputConnection);
        }
        return false;
      });
      var selectionKey = serverSelector
        .keys()
        .stream()
        .filter(k -> k.attachment() instanceof SeriputConnection)
        .findFirst()
        .orElseThrow();
      var connection = (SeriputConnection) selectionKey.attachment();

      // when
      serverSelector.wakeup();

      // then
      assertThat(selectionKey.isValid()).isTrue();
      assertThat(connection.state()).isEqualTo(SeriputConnection.State.OPEN);
      client.close();
    }
  }

  @Nested
  class ServerShutdown {
    @Test
    void should_Close_All_Connections_On_Server_Shutdown() throws Exception {
      // given
      underTest.start();
      var clients = IntStream
        .range(0, 3)
        .mapToObj(_ -> SeriputClient.of("localhost", SERVER_PORT))
        .toList();
      await().until(() -> clients.stream().allMatch(SeriputClient::tryToConnect));
      var serverSelector = underTest.selector();
      await()
        .untilAsserted(() ->
          assertThat(
            serverSelector
              .keys()
              .stream()
              .filter(k -> k.attachment() instanceof SeriputConnection)
              .count()
          ).isEqualTo(3)
        );
      var selectionKeys = serverSelector.keys().stream().toList();

      // when
      underTest.close();

      // then
      underTest.awaitShutdown();
      assertThat(selectionKeys.stream().noneMatch(SelectionKey::isValid)).isTrue();
      for (var client : clients) {
        assertThat(client.read(Integer.MAX_VALUE).limit()).isZero();
        client.close();
      }
    }

    @Test
    void should_Interrupt_All_WorkerThreads_On_Shutdown() throws Exception {
      // given
      underTest.start();
      var clients = IntStream
        .range(0, 3)
        .mapToObj(_ -> SeriputClient.of("localhost", SERVER_PORT))
        .toList();
      await().until(() -> clients.stream().allMatch(SeriputClient::tryToConnect));
      var serverSelector = underTest.selector();
      await()
        .untilAsserted(() -> // All connections are established
          assertThat(
            serverSelector
              .keys()
              .stream()
              .filter(k -> k.attachment() instanceof SeriputConnection)
              .count()
          ).isEqualTo(3)
        );
      var connections = serverSelector
        .keys()
        .stream()
        .filter(k -> k.attachment() instanceof SeriputConnection)
        .map(k -> (SeriputConnection) k.attachment())
        .toList();

      // when
      underTest.close();

      // then
      underTest.awaitShutdown();
      assertThat(connections.stream().noneMatch(c -> c.workerThread().isAlive())).isTrue();
    }

    @Test
    void should_Terminate_EventLoop_On_Close() throws Exception {
      // given
      underTest.start();

      // when
      underTest.close();

      // then
      await().atMost(Duration.ofMillis(500)).until(() -> !underTest.serverThread().isAlive());
    }

    @Test
    void should_Close_Be_Idempotent_When_Called_Multiple_Times() throws Exception {
      // given
      underTest.start();

      // when
      underTest.close();

      // then
      underTest.awaitShutdown();
      assertThatNoException().isThrownBy(() -> underTest.close());
      assertThat(underTest.state()).isEqualTo(SeriputServer.State.CLOSED);
    }
  }
}
