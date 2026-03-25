package io.seriput.server;

import static io.seriput.server.fixture.RequestFixtures.testKey;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

import io.seriput.common.HeapByteBufferAllocator;
import io.seriput.common.ObjectMapperProvider;
import io.seriput.common.serialization.response.Response;
import io.seriput.common.serialization.response.ResponseStatus;
import io.seriput.server.fixture.RequestFixtures;
import io.seriput.server.fixture.SeriputClient;
import io.seriput.server.serialization.response.ResponseSerializer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

final class SeriputServerIntegrationTest {
  private static final int SERVER_PORT = 8080;
  private static final ObjectMapper objectMapper = ObjectMapperProvider.getInstance();

  private SeriputServer underTest;

  @BeforeEach
  void setUp() throws Exception {
    this.underTest =
        new SeriputServer(
            SERVER_PORT,
            new RequestHandlerImpl(
                new ResponseSerializer(new HeapByteBufferAllocator()), Collections.emptyList()));
    this.underTest.start();
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

  @Test
  void should_Handle_Single_Request_EndToEnd() throws Exception {
    // given
    try (SeriputClient client = SeriputClient.of("localhost", SERVER_PORT)) {
      await().until(client::tryToConnect);

      // when
      Response<?> response = client.get(testKey(), ObjectNode.class);

      // then
      assertThat(response.status()).isEqualTo(ResponseStatus.NOT_FOUND);
      assertThat(response.value()).isNull();
    }
  }

  @Test
  void should_Handle_Multiple_Requests_From_Same_Client() throws Exception {
    // given
    try (SeriputClient client = SeriputClient.of("localhost", SERVER_PORT)) {
      await().until(client::tryToConnect);

      // when
      for (int i = 0; i < 5; i++) {
        // when
        Response<?> response = client.get(testKey(), ObjectNode.class);

        // then
        assertThat(response.status()).isEqualTo(ResponseStatus.NOT_FOUND);
      }
    }
  }

  @Test
  void should_Handle_Multiple_Clients_Concurrently() throws Exception {
    // given
    int numOfClient = 10;

    // when
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<Void>> futures = new ArrayList<>();
      for (int i = 0; i < numOfClient; i++) {
        futures.add(
            executor.submit(
                () -> {
                  try (SeriputClient client = SeriputClient.of("localhost", SERVER_PORT)) {
                    await().until(client::tryToConnect);

                    String key = testKey();
                    var value = objectMapper.createObjectNode().put("name", "John Doe");
                    Response<?> putResponse = client.put(key, value);
                    assertThat(putResponse.status()).isEqualTo(ResponseStatus.OK);
                    assertThat(putResponse.value()).isNull();
                    Response<?> getResponse = client.get(key, ObjectNode.class);
                    assertThat(getResponse.status()).isEqualTo(ResponseStatus.OK);
                    assertThat(getResponse.value()).isEqualTo(value);
                    Response<?> deleteResponse = client.delete(key);
                    assertThat(deleteResponse.status()).isEqualTo(ResponseStatus.OK);
                    assertThat(deleteResponse.value()).isNull();
                  }
                  return null;
                }));
      }

      // then
      for (var future : futures) {
        future.get(1, TimeUnit.SECONDS);
      }
    }
  }

  @Test
  void should_Handle_Client_Disconnect_Gracefully() throws Exception {
    // given
    SeriputClient client = SeriputClient.of("localhost", SERVER_PORT);
    await().until(client::tryToConnect);
    String key = testKey();
    client.write(RequestFixtures.testPutRequestPayload(key));

    // when
    client.close();

    // then
    await().until(() -> underTest.connections().isEmpty());
    // Verify that the value was stored despite the client disconnect
    client = SeriputClient.of("localhost", SERVER_PORT);
    await().until(client::tryToConnect);
    var response = client.get(key, ObjectNode.class);
    assertThat(response.status()).isEqualTo(ResponseStatus.OK);
    var expectedValue = objectMapper.convertValue(RequestFixtures.testValue, ObjectNode.class);
    assertThat(response.value()).isEqualTo(expectedValue);
  }

  @Test
  void should_Shutdown_Even_There_Is_A_Connected_Client() throws Exception {
    try (SeriputClient client = SeriputClient.of("localhost", SERVER_PORT)) {
      await().until(client::tryToConnect);
      client.write(RequestFixtures.testGetRequestPayload);

      // when
      underTest.close();
      underTest.awaitShutdown();

      // then
      assertThat(client.read(Integer.MAX_VALUE).capacity())
          .isZero(); // Server is closed, no response was sent.
    }
  }
}
