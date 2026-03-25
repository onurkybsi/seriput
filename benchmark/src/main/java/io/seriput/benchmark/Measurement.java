package io.seriput.benchmark;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

final class Measurement {
  static final long MAX_LATENCY_MICROS = TimeUnit.SECONDS.toMicros(10);

  final Recorder recorder = new Recorder(MAX_LATENCY_MICROS, 3);
  final LongAdder success = new LongAdder();
  final LongAdder errors = new LongAdder();
  final AtomicInteger inFlight = new AtomicInteger();
  volatile long elapsedNanos;

  record BenchmarkResult(
      String timestamp,
      int concurrency,
      int targetRps,
      int durationSec,
      double rpsSuccess,
      double p95Ms,
      double p99Ms,
      double errorRatePct) {

    static BenchmarkResult of(Measurement m, BenchmarkConfig config) {
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
          config.concurrency(),
          config.targetRps(),
          config.testSec(),
          rpsSuccess,
          p95Ms,
          p99Ms,
          errorRatePct);
    }
  }
}
