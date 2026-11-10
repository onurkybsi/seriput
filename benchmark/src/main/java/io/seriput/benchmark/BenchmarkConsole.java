package io.seriput.benchmark;

import java.util.Locale;

final class BenchmarkConsole {
  private BenchmarkConsole() {}

  static String format(BenchmarkResult result) {
    LatencyStatistics latency = result.latency();
    return String.format(
        Locale.ROOT,
        """
        ====== SERIPUT GET ======
          %d requests completed in %.3f seconds
          %d parallel clients
          %d bytes payload
          keep alive: 1
          throughput: %.3f requests per second
        Latency (ms):
          avg: %.3f
          min: %.3f
          p50: %.3f
          p95: %.3f
          p99: %.3f
          max: %.3f
        """,
        result.completedRequests(),
        result.elapsedSeconds(),
        result.connections(),
        result.payloadBytes(),
        result.throughputRps(),
        latency.averageMs(),
        latency.minimumMs(),
        latency.p50Ms(),
        latency.p95Ms(),
        latency.p99Ms(),
        latency.maximumMs());
  }
}
