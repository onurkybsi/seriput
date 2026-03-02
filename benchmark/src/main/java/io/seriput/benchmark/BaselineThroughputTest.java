package io.seriput.benchmark;

import io.seriput.server.SeriputServer;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * Collects the metrics below:
 * </p>
 * <ul>
 * <li>RPS (requests per second)</li>
 * <li>Concurrency (number of simultaneous connections)</li>
 * <li>p95 latency (95th percentile response time)</li>
 * <li>p99 latency (99th percentile response time)</li>
 * <li>Error rate (percentage of failed requests)</li>
 * </ul>
 */
final class BaselineThroughputTest {
  private static final int PORT = 9090;
  private static final int CONCURRENCY = 256;
  private static final int WARMUP_SEC = 30;
  private static final int TEST_SEC = 120;
  // Configure expected max latency for histogram range (pick something sane for
  // your env)
  private static final long MAX_LATENCY_MICROS = TimeUnit.SECONDS.toMicros(10);

  private static final Logger logger = LogManager.getLogger(BaselineThroughputTest.class);

  public static void main(String[] args) throws Exception {
    SeriputServer server = new SeriputServer(PORT); // NOSONAR
    server.start();

    try {
      runTestWithPhases();
    } finally {
      server.close();
      server.awaitShutdown();
    }
  }

  private static void runTest() throws InterruptedException, IOException {
    // 1. Create and connect clients
    List<SeriputClient> clients = new ArrayList<>(CONCURRENCY);
    for (int i = 0; i < CONCURRENCY; i++) {
      SeriputClient client = SeriputClient.of("localhost", PORT);
      while (!client.tryToConnect()) {
        // Retry until connected
      }
      clients.add(client);
    }

    // 2. Start load workers
    final AtomicBoolean stop = new AtomicBoolean(false);
    final LongAdder requests = new LongAdder();
    final LongAdder errors = new LongAdder();
    try (ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY)) {
      for (SeriputClient client : clients) {
        executor.submit(() -> {
          while (!stop.get()) {
            try {
              client.get("baseline-key", String.class);
              requests.increment();
            } catch (Exception _) {
              errors.increment();
            }
          }
        });
      }

      // 3. Warm-up
      Thread.sleep(WARMUP_SEC * 1_000L);
      requests.reset();
      errors.reset();

      // 4. Measurement
      long startNanos = System.nanoTime();
      Thread.sleep(TEST_SEC * 1_000L);
      long elapsedNanos = System.nanoTime() - startNanos;

      // 5. Stop
      stop.set(true);
      executor.shutdownNow();
      for (SeriputClient client : clients) {
        client.close();
      }

      // 6. Results
      long totalRequests = requests.sum();
      long totalErrors = errors.sum();
      double seconds = elapsedNanos / 1_000_000_000.0;
      double throughput = totalRequests / seconds;
      System.out.println("=== Baseline Throughput Test ===");
      System.out.println("Concurrency: " + CONCURRENCY);
      System.out.println("Duration: " + TEST_SEC + "s");
      System.out.println("Requests: " + totalRequests);
      System.out.println("Errors: " + totalErrors);
      System.out.printf("Throughput: %.2f req/s%n", throughput);
    }
  }

  private static void runTestWithPhases() throws InterruptedException {
    // 1. Create and connect clients
    var clients = new ArrayList<SeriputClient>(CONCURRENCY);
    for (int i = 0; i < CONCURRENCY; i++) {
      var client = SeriputClient.of("localhost", PORT);
      while (!client.tryToConnect()) {
        // Retry until connected
      }
      clients.add(client);
    }

    try (ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY)) {
      // Warm-up
      runPhase(clients, executor, WARMUP_SEC, null);

      Measurement m = new Measurement();
      runPhase(clients, executor, TEST_SEC, m);

      long ok = m.success.sum();
      long err = m.errors.sum();
      long total = ok + err;

      double seconds = m.elapsedNanos / 1_000_000_000.0;
      double rpsSuccess = seconds > 0 ? (ok / seconds) : 0.0;
      double errorRatePct = total > 0 ? (err * 100.0 / total) : 0.0;

      // Recorder is thread-safe for recording; snapshot at end:
      Histogram histogram = m.recorder.getIntervalHistogram(); // captures what was recorded so far
      double p95Ms = histogram.getValueAtPercentile(95.0) / 1_000.0;
      double p99Ms = histogram.getValueAtPercentile(99.0) / 1_000.0;

      System.out.println("=== Baseline Throughput Test (Seriput) ===");
      System.out.println("Concurrency: " + CONCURRENCY);
      System.out.println("Duration: " + TEST_SEC + "s");
      System.out.printf("RPS (success): %.2f%n", rpsSuccess);
      System.out.printf("p95 latency:   %.3f ms%n", p95Ms);
      System.out.printf("p99 latency:   %.3f ms%n", p99Ms);
      System.out.printf("Error rate:    %.2f%%%n", errorRatePct);
    }
  }

  private static void runPhase(List<SeriputClient> clients, ExecutorService executor, int durationSeconds,
      Measurement measurementOrNull) throws InterruptedException {
    var started = new CountDownLatch(clients.size());
    var stop = new AtomicBoolean(false);

    for (SeriputClient client : clients) {
      executor.submit(() -> {
        started.countDown();
        while (!stop.get()) {
          if (measurementOrNull == null) {
            // Warm-up: no metrics
            try {
              client.get("baseline-key", String.class);
            } catch (Exception _) {
              // ignore in warm-up
            }
            continue;
          }

          long start = System.nanoTime();
          try {
            client.get("baseline-key", String.class);
            long micros = (System.nanoTime() - start) / 1_000L;

            // Record latency only for successful ops
            measurementOrNull.recorder.recordValue(micros);
            measurementOrNull.success.increment();
          } catch (Exception _) {
            measurementOrNull.errors.increment();
          }
        }
      });
    }
    started.await(); // Ensure workers are running before we start timing

    long startNanos = System.nanoTime();
    Thread.sleep(durationSeconds * 1_000L);
    long elapsedNanos = System.nanoTime() - startNanos;
    stop.set(true);
    if (measurementOrNull != null) {
      measurementOrNull.elapsedNanos = elapsedNanos;
    }
  }

  private static final class Measurement {
    // Thread-safe aggregator for many writers
    final Recorder recorder = new Recorder(MAX_LATENCY_MICROS, 3);
    final LongAdder success = new LongAdder();
    final LongAdder errors = new LongAdder();
    volatile long elapsedNanos;
  }
}
