package io.seriput.benchmark;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import io.seriput.client.SeriputClient;

final class DeleteThroughputBenchmark extends ThroughputBenchmark {
  private static final int BATCH_SIZE = 1_000;
  private static final Logger logger = LogManager.getLogger(DeleteThroughputBenchmark.class);

  private final AtomicLong keyCounter = new AtomicLong();

  DeleteThroughputBenchmark(BenchmarkConfig config) {
    super(config);
  }

  @Override
  String resultFile() {
    return "delete-throughput-result.jsonl";
  }

  @Override
  void prepopulate(SeriputClient client) {
    long totalKeys = (long) ((config.warmupSec() + config.testSec()) * config.targetRps() * 1.1);
    logger.info("Pre-populating {} keys for DELETE benchmark...", totalKeys);

    var futures = new ArrayList<CompletableFuture<Void>>(BATCH_SIZE);
    for (long i = 0; i < totalKeys; i++) {
      String key = "del-" + i;
      futures.add(client.put(key, "v"));

      if (futures.size() >= BATCH_SIZE) {
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        futures.clear();
      }
    }
    if (!futures.isEmpty()) {
      CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }

    logger.info("Pre-population complete ({} keys).", totalKeys);
  }

  @Override
  void fireRequest(SeriputClient client, Measurement measurement) {
    String key = "del-" + keyCounter.getAndIncrement();
    if (measurement != null) {
      measurement.inFlight.incrementAndGet();
    }
    long startNanos = System.nanoTime();
    client.delete(key)
        .whenComplete((result, throwable) -> {
          if (measurement == null) {
            return;
          }
          if (throwable != null) {
            measurement.errors.increment();
            logger.warn("Request failed: {}", throwable.getMessage());
          } else {
            long micros = (System.nanoTime() - startNanos) / 1_000L;
            measurement.recorder.recordValue(Math.min(micros, Measurement.MAX_LATENCY_MICROS));
            measurement.success.increment();
          }
          measurement.inFlight.decrementAndGet();
        });
  }

  public static void main(String[] args) throws Exception {
    new DeleteThroughputBenchmark(BenchmarkConfig.fromArgs(args)).run();
  }
}
