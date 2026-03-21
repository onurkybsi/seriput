package io.seriput.benchmark;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.seriput.client.SeriputClient;

final class GetThroughputBenchmark extends ThroughputBenchmark {
  private static final String KEY = "k";
  private static final Logger logger = LogManager.getLogger(GetThroughputBenchmark.class);

  GetThroughputBenchmark(BenchmarkConfig config) {
    super(config);
  }

  @Override
  String resultFile() {
    return "get-throughput-result.jsonl";
  }

  @Override
  void prepopulate(SeriputClient client) {
    logger.info("Pre-populating benchmark key '{}'...", KEY);
    client.put(KEY, "v").join();
  }

  @Override
  void fireRequest(SeriputClient client, Measurement measurement) {
    if (measurement != null) {
      measurement.inFlight.incrementAndGet();
    }
    long startNanos = System.nanoTime();
    client.get(KEY, String.class)
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
    new GetThroughputBenchmark(BenchmarkConfig.fromArgs(args)).run();
  }
}
