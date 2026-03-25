package io.seriput.benchmark;

/**
 * Configuration for throughput benchmarks.
 *
 * @param port server port
 * @param concurrency connection pool size
 * @param warmupSec warm-up phase duration in seconds
 * @param testSec measurement phase duration in seconds
 * @param targetRps target requests per second
 */
record BenchmarkConfig(int port, int concurrency, int warmupSec, int testSec, int targetRps) {
  static final int DEFAULT_PORT = 9090;
  static final int DEFAULT_CONCURRENCY = 128;
  static final int DEFAULT_WARMUP_SEC = 30;
  static final int DEFAULT_TEST_SEC = 120;
  static final int DEFAULT_TARGET_RPS = 120_000;

  long intervalNanos() {
    return 1_000_000_000L / targetRps;
  }

  static BenchmarkConfig fromArgs(String[] args) {
    int concurrency = DEFAULT_CONCURRENCY;
    int targetRps = DEFAULT_TARGET_RPS;

    if (args.length >= 1) {
      concurrency = Integer.parseInt(args[0]);
    }
    if (args.length >= 2) {
      targetRps = Integer.parseInt(args[1]);
    }

    return new BenchmarkConfig(
        DEFAULT_PORT, concurrency, DEFAULT_WARMUP_SEC, DEFAULT_TEST_SEC, targetRps);
  }
}
