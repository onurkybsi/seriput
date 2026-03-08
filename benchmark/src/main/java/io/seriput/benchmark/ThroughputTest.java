package io.seriput.benchmark;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.seriput.common.ObjectMapperProvider;
import io.seriput.server.SeriputServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;

/**
 * Open-loop throughput benchmark for SeriputServer.
 *
 * <p>
 * Requests are generated at a fixed target rate ({@link #TARGET_RPS}) using a
 * virtual-thread
 * scheduler, regardless of server response time. This avoids coordinated
 * omission: when the server
 * is slow, in-flight requests accumulate and latency is correctly captured.
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
  private static final int TARGET_RPS = 87_000;
  private static final long INTERVAL_NANOS = 1_000_000_000L / TARGET_RPS;
  private static final int WARMUP_SEC = 30;
  private static final int TEST_SEC = 120;
  private static final long MAX_LATENCY_MICROS = TimeUnit.SECONDS.toMicros(10);
  private static final String BENCHMARK_KEY = "baseline-key";
  private static final String RESULTS_FILE = "benchmark-results.jsonl";

  private static final Logger logger = LogManager.getLogger(ThroughputTest.class);

  public static void main() throws Exception {
    logger.info("Starting Seriput server on port {}...", PORT);
    SeriputServer server = new SeriputServer(PORT); // NOSONAR
    server.start();
    logger.info("Server started. Running benchmark...");

    try {
      runTestWithPhases();
    } finally {
      logger.info("Benchmark complete. Shutting down the server...");
      server.close();
      server.awaitShutdown();
    }
  }

  private static void runTestWithPhases() throws InterruptedException, IOException {
    var clients = buildClientPool();

    // Pre-populate the key so GETs hit the happy path during the benchmark
    logger.info("Pre-populating benchmark key '{}'...", BENCHMARK_KEY);
    var client = clients.take();
    client.put(BENCHMARK_KEY, "value");
    clients.put(client);

    try {
      // Warm-up: open-loop at target RPS, no metrics collected
      logger.info("Starting warm-up phase ({} s)...", WARMUP_SEC);
      runPhase(clients, WARMUP_SEC, null);
      logger.info("Warm-up complete.");

      logger.info("Starting measurement phase ({} s)...", TEST_SEC);
      Measurement m = new Measurement();
      runPhase(clients, TEST_SEC, m);
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
      close(clients);
    }
  }

  private static void runPhase(ArrayBlockingQueue<SeriputClient> pool, int durationSec, Measurement measurementOrNull)
      throws InterruptedException {
    AtomicBoolean shouldStop = new AtomicBoolean(false);
    Thread loadGeneratorThread = new Thread(() -> {
      long nextFireNanos = System.nanoTime();
      while (!shouldStop.get()) {
        LockSupport.parkNanos(nextFireNanos - System.nanoTime());
        if (shouldStop.get()) {
          break;
        }

        Thread.ofVirtual().start(() -> executeRequest(pool, measurementOrNull));
        nextFireNanos += INTERVAL_NANOS;
      }
    }, "load-generator");
    loadGeneratorThread.start();

    long startNanos = System.nanoTime();
    Thread.sleep(durationSec * 1_000L);
    long elapsedNanos = System.nanoTime() - startNanos;
    if (measurementOrNull != null) {
      measurementOrNull.elapsedNanos = elapsedNanos;
    }
    shouldStop.set(true);
    loadGeneratorThread.join();

    drain(pool);
  }

  private static void executeRequest(BlockingQueue<SeriputClient> pool, Measurement measurementOrNull) {
    SeriputClient client;
    try {
      client = pool.take();
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
      return;
    }
    long start = System.nanoTime();

    try {
      client.get(BENCHMARK_KEY, String.class);
      if (measurementOrNull != null) {
        long micros = (System.nanoTime() - start) / 1_000L;
        measurementOrNull.recorder.recordValue(Math.min(micros, MAX_LATENCY_MICROS));
        measurementOrNull.success.increment();
      }
    } catch (Exception e) {
      if (measurementOrNull != null) {
        measurementOrNull.errors.increment();
        logger.warn("Request failed: {}", e.getMessage());
      }
    } finally {
      try {
        pool.put(client);
      } catch (InterruptedException _) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private static ArrayBlockingQueue<SeriputClient> buildClientPool() {
    logger.info("Building client pool ({} connections) on localhost:{}...", CONCURRENCY, PORT);
    var clients = new ArrayList<SeriputClient>(CONCURRENCY);
    for (int i = 0; i < CONCURRENCY; i++) {
      var client = SeriputClient.of("localhost", PORT);
      while (!client.tryToConnect()) {
        Thread.yield();
      }
      clients.add(client);
    }
    logger.info("Client pool ready.");
    return new ArrayBlockingQueue<>(CONCURRENCY, false, clients);
  }

  private static void drain(ArrayBlockingQueue<SeriputClient> clientPool) throws InterruptedException {
    var drained = new ArrayList<SeriputClient>(CONCURRENCY);
    for (int i = 0; i < CONCURRENCY; i++) {
      drained.add(clientPool.take());
    }
    clientPool.addAll(drained);
  }

  private static void close(ArrayBlockingQueue<SeriputClient> clientPool) throws InterruptedException {
    logger.info("Closing {} client connections...", CONCURRENCY);
    for (int i = 0; i < CONCURRENCY; i++) {
      try {
        clientPool.take().close();
      } catch (IOException _) {
        /* best-effort close */
      }
    }
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
