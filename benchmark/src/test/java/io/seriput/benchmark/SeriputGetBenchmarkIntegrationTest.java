package io.seriput.benchmark;

import static org.assertj.core.api.Assertions.assertThat;

import io.seriput.server.SeriputServer;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;

final class SeriputGetBenchmarkIntegrationTest {
  @Test
  void should_CompleteReducedWorkload_When_RealServerIsRunning() throws Exception {
    // given
    int port = availablePort();
    var workload =
        new BenchmarkWorkload(
            "127.0.0.1",
            port,
            2,
            10,
            20,
            Duration.ofSeconds(2),
            "seriput-benchmark-integration-key",
            "\"x\"".getBytes(StandardCharsets.UTF_8));
    var underTest = new GetBenchmarkRunner(SocketBenchmarkConnection::from);

    // when
    BenchmarkResult actual;
    try (var server = new SeriputServer(port)) {
      server.start();
      actual = underTest.run(workload);
    }

    // then
    assertThat(actual.completedRequests()).isEqualTo(20);
    assertThat(actual.connections()).isEqualTo(2);
    assertThat(actual.latency().maximumMs()).isGreaterThanOrEqualTo(0.001);
  }

  private static int availablePort() throws Exception {
    try (var socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }
}
