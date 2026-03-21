package io.seriput.benchmark;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.seriput.client.SeriputClient;
import io.seriput.common.ObjectMapperProvider;
import io.seriput.server.SeriputServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;

/**
 * Open-loop throughput benchmark for SeriputServer.
 *
 * <p>
 * Requests are generated at a fixed target rate ({@link #TARGET_RPS})
 * regardless of server response time. This avoids coordinated omission: when
 * the server is slow, in-flight requests accumulate and latency is correctly
 * captured.
 *
 * <p>
 * Metrics collected:
 * <ul>
 * <li>RPS (requests per second, successful)</li>
 * <li>p95 and p99 latency</li>
 * <li>Error rate</li>
 * </ul>
 */
final class ThroughputTest {
  private static final int PORT = 9090;
  private static final int CONCURRENCY = 128;
  private static final int WARMUP_SEC = 30;
  private static final int TEST_SEC = 120;
  private static final int TARGET_RPS = 120_000;
  private static final long INTERVAL_NANOS = 1_000_000_000L / TARGET_RPS;
  private static final long MAX_LATENCY_MICROS = TimeUnit.SECONDS.toMicros(10);
  private static final String BENCHMARK_KEY = "k";
  private static final String RESULTS_FILE = "benchmark-results.jsonl";

  private static final Logger logger = LogManager.getLogger(ThroughputTest.class);

  public static void main() throws Exception {
    try (SeriputServer server = new SeriputServer(PORT)) {
      logger.info("Starting Seriput server on port {}...", PORT);
      server.start();
      logger.info("Server started. Running benchmark...");
      run();
    } finally {
      logger.info("Benchmark complete. Shutting down the server...");
    }
  }

  @SuppressWarnings("java:S2629")
  private static void run() throws Exception {
    var client = buildClient();

    // Pre-populate the key so GETs hit the happy path during the benchmark
    logger.info("Pre-populating benchmark key '{}'...", BENCHMARK_KEY);
    client.put(BENCHMARK_KEY, "v").join();

    try {
      // Warm-up: open-loop at target RPS, no metrics collected
      logger.info("Starting warm-up phase ({} s)...", WARMUP_SEC);
      runPhase(client, WARMUP_SEC, null);
      logger.info("Warm-up complete.");

      logger.info("Starting measurement phase ({} s)...", TEST_SEC);
      Measurement m = new Measurement();
      runPhase(client, TEST_SEC, m);
      logger.info("Measurement phase complete.");

      BenchmarkResult result = BenchmarkResult.of(m);
      persistResult(result);
      logger.info("=== Baseline Throughput Test (Seriput) ===");
      logger.info("Connections:   {}", CONCURRENCY);
      logger.info("Target RPS:    {}", TARGET_RPS);
      logger.info("Duration:      {} s", TEST_SEC);
      logger.info("RPS (success): {}", String.format("%.2f", result.rpsSuccess()));
      logger.info("p95 latency:   {} ms", String.format("%.3f", result.p95Ms()));
      logger.info("p99 latency:   {} ms", String.format("%.3f", result.p99Ms()));
      logger.info("Error rate:    {}%", String.format("%.2f", result.errorRatePct()));
    } finally {
      client.close();
    }
  }

  private static void runPhase(SeriputClient client, int durationSec, Measurement measurementOrNull)
      throws InterruptedException {
    var shouldStop = new AtomicBoolean(false);
    var loadGeneratorThread = new Thread(() -> {
      long nextFireNanos = System.nanoTime();
      while (!shouldStop.get()) {
        LockSupport.parkNanos(nextFireNanos - System.nanoTime());
        if (shouldStop.get()) {
          break;
        }

        fireRequest(client, measurementOrNull);
        nextFireNanos += INTERVAL_NANOS;
      }
    }, "load-generator");
    loadGeneratorThread.start();
    long startNanos = System.nanoTime();

    Thread.sleep(durationSec * 1_000L);
    shouldStop.set(true);
    loadGeneratorThread.join();

    if (measurementOrNull != null) {
      logger.info("Waiting for the remaining {} inflight requests...", measurementOrNull.inFlight.get());
      long drainTimeoutNanos = TimeUnit.SECONDS.toNanos(10);
      long drainStart = System.nanoTime();
      while (measurementOrNull.inFlight.get() > 0) {
        if (System.nanoTime() - drainStart > drainTimeoutNanos) {
          logger.warn("Drain timed out with {} in-flight requests remaining", measurementOrNull.inFlight.get());
          break;
        }
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
      }
      measurementOrNull.elapsedNanos = System.nanoTime() - startNanos;
    }
  }

  private static void fireRequest(SeriputClient client, Measurement measurementOrNull) {
    if (measurementOrNull != null) {
      measurementOrNull.inFlight.incrementAndGet();
    }
    final long startNanos = System.nanoTime();
    client.get(BENCHMARK_KEY, String.class)
        .whenComplete((result, throwable) -> {
          if (measurementOrNull == null) {
            return;
          }
          if (throwable != null) {
            measurementOrNull.errors.increment();
            logger.warn("Request failed: {}", throwable.getMessage());
          } else {
            long micros = (System.nanoTime() - startNanos) / 1_000L;
            measurementOrNull.recorder.recordValue(Math.min(micros, MAX_LATENCY_MICROS));
            measurementOrNull.success.increment();
          }
          measurementOrNull.inFlight.decrementAndGet();
        });
  }

  private static SeriputClient buildClient() throws IOException {
    logger.info("Building SeriputClient with pool size {} on localhost:{}...", CONCURRENCY, PORT);
    return SeriputClient.builder("localhost", PORT)
        .poolSize(CONCURRENCY)
        .build();
  }

  private static void persistResult(BenchmarkResult result) {
    try {
      String json = ObjectMapperProvider.getInstance().writeValueAsString(result);
      Files.writeString(Path.of(RESULTS_FILE), json + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      logger.info("Result persisted to {}", RESULTS_FILE);
    } catch (IOException e) {
      logger.error("Failed to persist benchmark result to {}", RESULTS_FILE, e);
    }
  }

  private static final class Measurement {
    final Recorder recorder = new Recorder(MAX_LATENCY_MICROS, 3);
    final LongAdder success = new LongAdder();
    final LongAdder errors = new LongAdder();
    final AtomicInteger inFlight = new AtomicInteger();
    volatile long elapsedNanos;
  }

  private record BenchmarkResult(
      String timestamp,
      int concurrency,
      int targetRps,
      int durationSec,
      double rpsSuccess,
      double p95Ms,
      double p99Ms,
      double errorRatePct) {

    static BenchmarkResult of(Measurement m) {
      long ok = m.success.sum();
      long err = m.errors.sum();
      long total = ok + err;
      double seconds = m.elapsedNanos / 1_000_000_000.0;
      double rpsSuccess = seconds > 0 ? (ok / seconds) : 0.0;
      double errorRatePct = total > 0 ? (err * 100.0 / total) : 0.0;
      Histogram histogram = m.recorder.getIntervalHistogram();
      double p95Ms = histogram.getValueAtPercentile(95.0) / 1_000.0;
      double p99Ms = histogram.getValueAtPercentile(99.0) / 1_000.0;
      return new BenchmarkResult(
          Instant.now().toString(),
          CONCURRENCY,
          TARGET_RPS,
          TEST_SEC,
          rpsSuccess,
          p95Ms,
          p99Ms,
          errorRatePct);
    }
  }
}
