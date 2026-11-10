package io.seriput.benchmark;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;

final class SeriputGetBenchmark {
  private static final BenchmarkWorkload WORKLOAD =
      new BenchmarkWorkload(
          "127.0.0.1",
          8080,
          50,
          10_000,
          100_000,
          Duration.ofSeconds(10),
          "seriput-benchmark-key",
          "\"x\"".getBytes(StandardCharsets.UTF_8));
  private static final Path RESULTS_FILE = Path.of("benchmark", "get-benchmark-results.csv");

  private SeriputGetBenchmark() {}

  public static void main() throws Exception {
    var runner = new GetBenchmarkRunner(SocketBenchmarkConnection::from);
    BenchmarkResult result = runner.run(WORKLOAD);
    System.out.print(BenchmarkConsole.format(result)); // NOSONAR
    CsvResultWriter.append(RESULTS_FILE, result, Clock.systemUTC());
  }
}
