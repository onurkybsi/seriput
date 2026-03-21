package io.seriput.benchmark;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.seriput.client.SeriputClient;
import io.seriput.benchmark.Measurement.BenchmarkResult;
import io.seriput.common.ObjectMapperProvider;
import io.seriput.server.SeriputServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

abstract sealed class ThroughputBenchmark permits GetThroughputBenchmark, PutThroughputBenchmark, DeleteThroughputBenchmark {
  private static final Logger logger = LogManager.getLogger(ThroughputBenchmark.class);

  protected final BenchmarkConfig config;

  protected ThroughputBenchmark(BenchmarkConfig config) {
    this.config = config;
  }

  abstract String resultsFile();

  abstract void prepopulate(SeriputClient client);

  abstract void fireRequest(SeriputClient client, Measurement measurement);

  void run() throws Exception {
    try (SeriputServer server = new SeriputServer(config.port())) {
      logger.info("Starting Seriput server on port {}...", config.port());
      server.start();
      logger.info("Server started. Running benchmark...");

      var client = buildClient();
      try {
        prepopulate(client);

        logger.info("Starting warm-up phase ({} s)...", config.warmupSec());
        runPhase(client, config.warmupSec(), null);
        logger.info("Warm-up complete.");

        logger.info("Starting measurement phase ({} s)...", config.testSec());
        Measurement m = new Measurement();
        runPhase(client, config.testSec(), m);
        logger.info("Measurement phase complete.");

        BenchmarkResult result = BenchmarkResult.of(m, config);
        persistResult(result);
        logSummary(result);
      } finally {
        client.close();
      }
    } finally {
      logger.info("Benchmark complete. Shutting down the server...");
    }
  }

  private void runPhase(SeriputClient client, int durationSec, Measurement measurementOrNull)
      throws InterruptedException {
    long intervalNanos = config.intervalNanos();
    var shouldStop = new AtomicBoolean(false);
    var loadGeneratorThread = new Thread(() -> {
      long nextFireNanos = System.nanoTime();
      while (!shouldStop.get()) {
        LockSupport.parkNanos(nextFireNanos - System.nanoTime());
        if (shouldStop.get()) {
          break;
        }

        fireRequest(client, measurementOrNull);
        nextFireNanos += intervalNanos;
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

  private SeriputClient buildClient() throws IOException {
    logger.info("Building SeriputClient with pool size {} on localhost:{}...", config.concurrency(), config.port());
    return SeriputClient.builder("localhost", config.port())
        .poolSize(config.concurrency())
        .build();
  }

  private void persistResult(BenchmarkResult result) {
    try {
      String json = ObjectMapperProvider.getInstance().writeValueAsString(result);
      Files.writeString(Path.of(resultsFile()), json + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      logger.info("Result persisted to {}", resultsFile()); // NOSONAR
    } catch (IOException e) {
      logger.error("Failed to persist benchmark result to {}", resultsFile(), e);
    }
  }

  @SuppressWarnings("java:S2629")
  private void logSummary(BenchmarkResult result) {
    logger.info("=== Throughput Benchmark Result ===");
    logger.info("Connections:   {}", config.concurrency());
    logger.info("Target RPS:    {}", config.targetRps());
    logger.info("Duration:      {} s", config.testSec());
    logger.info("RPS (success): {}", String.format("%.2f", result.rpsSuccess()));
    logger.info("p95 latency:   {} ms", String.format("%.3f", result.p95Ms()));
    logger.info("p99 latency:   {} ms", String.format("%.3f", result.p99Ms()));
    logger.info("Error rate:    {}%", String.format("%.2f", result.errorRatePct()));
  }
}
