package io.seriput.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

final class CsvResultWriter {
  static final String HEADER = "timestamp,throughput_rps,avg_ms,min_ms,p50_ms,p95_ms,p99_ms,max_ms";

  private CsvResultWriter() {}

  static void append(Path path, BenchmarkResult result, Clock clock) throws IOException {
    boolean writeHeader = !Files.exists(path) || Files.size(path) == 0;
    LatencyStatistics latency = result.latency();
    String row =
        String.format(
            Locale.ROOT,
            "%s,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f%n",
            Instant.now(clock),
            result.throughputRps(),
            latency.averageMs(),
            latency.minimumMs(),
            latency.p50Ms(),
            latency.p95Ms(),
            latency.p99Ms(),
            latency.maximumMs());
    String content = (writeHeader ? HEADER + System.lineSeparator() : "") + row;
    Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
  }
}
