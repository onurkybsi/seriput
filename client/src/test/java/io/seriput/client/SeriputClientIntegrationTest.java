package io.seriput.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.seriput.client.exception.NotFoundException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.node.ObjectNode;

@Testcontainers
final class SeriputClientIntegrationTest {
  private static final int SERIPUT_PORT = 9090;

  @Container
  @SuppressWarnings("resource")
  static GenericContainer<?> server =
      new GenericContainer<>(
              new ImageFromDockerfile("seriput-client-impl-test", false)
                  .withFileFromPath(".", Paths.get(".."))
                  .withBuildArg("SERIPUT_PORT", "9090")
                  .withDockerfilePath("server/Dockerfile"))
          .withExposedPorts(SERIPUT_PORT)
          .waitingFor(Wait.forListeningPort());

  private SeriputClient client;

  @BeforeEach
  void setUp() throws Exception {
    client =
        SeriputClient.builder(server.getHost(), server.getMappedPort(SERIPUT_PORT))
            .poolSize(2)
            .build();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (client != null) {
      client.close();
    }
  }

  @Test
  void should_Get_Value_When_Key_Exists() {
    // given
    client.put("test-key", Map.of("name", "John")).join();

    // when
    var actual = client.get("test-key", ObjectNode.class).join();

    // then
    assertThat(actual.get("name").asString()).isEqualTo("John");
  }

  @Test
  void should_Throw_NotFoundException_When_Key_Does_Not_Exist() {
    // given & when
    var thrown = assertThatThrownBy(() -> client.get("missing-key", String.class).join());

    // then
    var expected = new NotFoundException(null, null);
    thrown.isInstanceOf(CompletionException.class).hasCause(expected);
  }

  @Test
  void should_Delete_Value_When_Key_Exists() {
    // given
    client.put("del-key", "value").join();

    // when
    client.delete("del-key").join();

    // then
    assertThatThrownBy(() -> client.get("del-key", String.class).join()) // NOSONAR
        .isInstanceOf(CompletionException.class)
        .hasCause(new NotFoundException(null, null));
  }

  @Test
  void should_Handle_Multiple_Sequential_Operations_When_Invoked_In_Order() {
    // given
    for (int i = 0; i < 10; i++) {
      client.put("seq-key-" + i, Map.of("index", i)).join();
    }

    // when & then
    for (int i = 0; i < 10; i++) {
      var actual = client.get("seq-key-" + i, ObjectNode.class).join();
      assertThat(actual.get("index").asInt()).isEqualTo(i);
    }
  }

  @Test
  void should_Handle_Concurrent_Operations_When_Multiple_Requests_Sent_In_Parallel() {
    // given
    List<CompletableFuture<Void>> futures =
        IntStream.range(0, 50).mapToObj(i -> client.put("conc-key-" + i, Map.of("i", i))).toList();
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    // when & then
    for (int i = 0; i < 50; i++) {
      var actual = client.get("conc-key-" + i, ObjectNode.class).join();
      assertThat(actual.get("i").asInt()).isEqualTo(i);
    }
  }
}
