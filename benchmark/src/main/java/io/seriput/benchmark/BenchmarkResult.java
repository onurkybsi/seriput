package io.seriput.benchmark;

record BenchmarkResult(
    int completedRequests,
    int connections,
    int payloadBytes,
    double elapsedSeconds,
    double throughputRps,
    LatencyStatistics latency) {
  static BenchmarkResult from(
      BenchmarkWorkload workload, long elapsedNanos, BenchmarkExecutionContext context) {
    double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
    return new BenchmarkResult(
        workload.measuredRequests(),
        workload.connections(),
        workload.payload().length,
        elapsedSeconds,
        workload.measuredRequests() / elapsedSeconds,
        LatencyStatistics.from(context.histogram()));
  }
}
