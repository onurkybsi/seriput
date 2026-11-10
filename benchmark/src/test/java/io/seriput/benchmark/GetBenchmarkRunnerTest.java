package io.seriput.benchmark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.HdrHistogram.Histogram;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GetBenchmarkRunnerTest {
  @Nested
  final class Run {
    @Test
    void should_ExecuteExactWorkload_When_MultipleConnectionsAreUsed() throws Exception {
      // given
      var workload = workload(3, 10, 20);
      var factory = new RecordingConnectionFactory(workload);
      var underTest = new GetBenchmarkRunner(factory);

      // when
      BenchmarkResult actual = underTest.run(workload);

      // then
      assertThat(factory.connections).hasSize(3);
      assertThat(factory.totalPutRequests()).isEqualTo(1);
      assertThat(factory.totalGetRequests()).isEqualTo(30);
      assertThat(factory.connections).allMatch(connection -> connection.maxInFlight.get() == 1);
      assertThat(factory.measurementStartedBeforeWarmupFinished.get()).isZero();
      assertThat(actual.completedRequests()).isEqualTo(20);
      assertThat(actual.connections()).isEqualTo(3);
      assertThat(actual.payloadBytes()).isEqualTo(3);
    }

    @Test
    void should_DistributeRequestsDeterministically_When_CountIsNotDivisible() {
      // given
      int totalRequests = 10;
      int workers = 3;

      // when
      List<Integer> actual =
          List.of(
              GetBenchmarkRunner.requestsForWorker(totalRequests, workers, 0),
              GetBenchmarkRunner.requestsForWorker(totalRequests, workers, 1),
              GetBenchmarkRunner.requestsForWorker(totalRequests, workers, 2));

      // then
      assertThat(actual).containsExactly(4, 3, 3);
    }

    @Test
    void should_AbortRun_When_RequestTimesOut() {
      // given
      var workload = workload(1, 0, 1);
      var calls = new AtomicInteger();
      var underTest =
          new GetBenchmarkRunner(
              ignored ->
                  new BenchmarkConnection() {
                    @Override
                    public void execute(byte[] request, ExpectedResponse response)
                        throws IOException {
                      if (calls.incrementAndGet() > 1) {
                        throw new SocketTimeoutException("timed out");
                      }
                    }

                    @Override
                    public void close() {}
                  });

      // when & then
      assertThatThrownBy(() -> underTest.run(workload))
          .isInstanceOf(SocketTimeoutException.class)
          .hasMessage("timed out");
    }
  }

  @Nested
  final class Statistics {
    @Test
    void should_CalculateLatencyStatistics_When_HistogramContainsMeasurements() {
      // given
      var histogram = new Histogram(3);
      histogram.recordValue(1_000);
      histogram.recordValue(2_000);
      histogram.recordValue(3_000);

      // when
      LatencyStatistics actual = LatencyStatistics.from(histogram);

      // then
      assertThat(actual.averageMs()).isCloseTo(2.0, within(0.01));
      assertThat(actual.minimumMs()).isCloseTo(1.0, within(0.01));
      assertThat(actual.p50Ms()).isCloseTo(2.0, within(0.01));
      assertThat(actual.p95Ms()).isCloseTo(3.0, within(0.01));
      assertThat(actual.p99Ms()).isCloseTo(3.0, within(0.01));
      assertThat(actual.maximumMs()).isCloseTo(3.0, within(0.01));
    }
  }

  @Nested
  final class Output {
    @Test
    void should_FormatThreeDecimalValues_When_ResultIsPrinted() {
      // given
      BenchmarkResult result = result();

      // when
      String actual = BenchmarkConsole.format(result);

      // then
      assertThat(actual)
          .contains("100000 requests completed in 2.500 seconds")
          .contains("50 parallel clients")
          .contains("3 bytes payload")
          .contains("throughput: 40000.000 requests per second")
          .contains("avg: 1.235")
          .contains("p99: 4.568");
    }

    @Test
    void should_WriteHeaderOnceAndAppendRows_When_CsvAlreadyExists(@TempDir java.nio.file.Path temp)
        throws Exception {
      // given
      java.nio.file.Path path = temp.resolve("results.csv");
      Clock clock = Clock.fixed(Instant.parse("2026-11-10T10:15:30Z"), ZoneOffset.UTC);

      // when
      CsvResultWriter.append(path, result(), clock);
      CsvResultWriter.append(path, result(), clock);
      List<String> actual = java.nio.file.Files.readAllLines(path);

      // then
      assertThat(actual).hasSize(3);
      assertThat(actual.getFirst()).isEqualTo(CsvResultWriter.HEADER);
      assertThat(actual.get(1))
          .isEqualTo("2026-11-10T10:15:30Z,40000.000,1.235,0.123,1.111,2.346,4.568,9.877");
      assertThat(actual.get(2)).isEqualTo(actual.get(1));
    }
  }

  private static BenchmarkWorkload workload(int connections, int warmup, int measured) {
    return new BenchmarkWorkload(
        "127.0.0.1",
        8080,
        connections,
        warmup,
        measured,
        Duration.ofSeconds(1),
        "seriput-benchmark-key",
        "\"x\"".getBytes(StandardCharsets.UTF_8));
  }

  private static BenchmarkResult result() {
    return new BenchmarkResult(
        100_000,
        50,
        3,
        2.5,
        40_000,
        new LatencyStatistics(1.23456, 0.1234, 1.1114, 2.3456, 4.5678, 9.8765));
  }

  private static final class RecordingConnectionFactory implements BenchmarkConnectionFactory {
    private final List<RecordingConnection> connections = new ArrayList<>();
    private final AtomicInteger measurementStartedBeforeWarmupFinished = new AtomicInteger();
    private final AtomicInteger totalWarmupRequests = new AtomicInteger();
    private final BenchmarkWorkload workload;

    private RecordingConnectionFactory(BenchmarkWorkload workload) {
      this.workload = workload;
    }

    @Override
    public BenchmarkConnection connect(BenchmarkWorkload ignored) {
      int index = connections.size();
      var connection = new RecordingConnection(this, index);
      connections.add(connection);
      return connection;
    }

    private int totalGetRequests() {
      return connections.stream().mapToInt(connection -> connection.getRequests.get()).sum();
    }

    private int totalPutRequests() {
      return connections.stream().mapToInt(connection -> connection.putRequests.get()).sum();
    }
  }

  private static final class RecordingConnection implements BenchmarkConnection {
    private final AtomicInteger getRequests = new AtomicInteger();
    private final AtomicInteger inFlight = new AtomicInteger();
    private final AtomicInteger maxInFlight = new AtomicInteger();
    private final int warmupRequests;
    private final RecordingConnectionFactory owner;
    private final AtomicInteger putRequests = new AtomicInteger();

    private RecordingConnection(RecordingConnectionFactory owner, int index) {
      this.owner = owner;
      this.warmupRequests =
          GetBenchmarkRunner.requestsForWorker(
              owner.workload.warmupRequests(), owner.workload.connections(), index);
    }

    @Override
    public void execute(byte[] request, ExpectedResponse expectedResponse) throws IOException {
      int current = inFlight.incrementAndGet();
      maxInFlight.accumulateAndGet(current, Math::max);
      try {
        if (request[0] == 0x02) {
          putRequests.incrementAndGet();
          return;
        }
        int requestNumber = getRequests.incrementAndGet();
        if (requestNumber <= warmupRequests) {
          owner.totalWarmupRequests.incrementAndGet();
        } else if (owner.totalWarmupRequests.get() != owner.workload.warmupRequests()) {
          owner.measurementStartedBeforeWarmupFinished.incrementAndGet();
        }
      } finally {
        inFlight.decrementAndGet();
      }
    }

    @Override
    public void close() {}
  }
}
