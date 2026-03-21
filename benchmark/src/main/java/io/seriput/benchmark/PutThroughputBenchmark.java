package io.seriput.benchmark;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicLong;

import io.seriput.client.SeriputClient;

final class PutThroughputBenchmark extends ThroughputBenchmark {
  private static final Logger logger = LogManager.getLogger(PutThroughputBenchmark.class);

  private final AtomicLong keyCounter = new AtomicLong();

  PutThroughputBenchmark(BenchmarkConfig config) {
    super(config);
  }

  @Override
  String resultFile() {
    return "put-throughput-result.jsonl";
  }

  @Override
  void prepopulate(SeriputClient client) {
    logger.info("No prepopulation needed for PUT benchmark.");
  }

  @Override
  void fireRequest(SeriputClient client, Measurement measurement) {
    String key = "put-" + keyCounter.getAndIncrement();
    if (measurement != null) {
      measurement.inFlight.incrementAndGet();
    }
    long startNanos = System.nanoTime();
    client.put(key, "v")
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
    new PutThroughputBenchmark(BenchmarkConfig.fromArgs(args)).run();
  }
}
