package io.seriput.benchmark;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.HdrHistogram.ConcurrentHistogram;

record BenchmarkExecutionContext(
    ConcurrentHistogram histogram,
    AtomicReference<Throwable> failure,
    CountDownLatch ready,
    CountDownLatch warmupStart,
    CountDownLatch warmupDone,
    CountDownLatch measurementStart,
    CountDownLatch measurementDone) {

  static BenchmarkExecutionContext executionContext(int workers, long maximumLatencyMicros) {
    return new BenchmarkExecutionContext(
        new ConcurrentHistogram(1, maximumLatencyMicros, 3),
        new AtomicReference<>(),
        new CountDownLatch(workers),
        new CountDownLatch(1),
        new CountDownLatch(workers),
        new CountDownLatch(1),
        new CountDownLatch(workers));
  }

  boolean failed() {
    return failure.get() != null;
  }
}
