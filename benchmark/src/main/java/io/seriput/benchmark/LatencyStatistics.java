package io.seriput.benchmark;

import org.HdrHistogram.Histogram;

record LatencyStatistics(
    double averageMs,
    double minimumMs,
    double p50Ms,
    double p95Ms,
    double p99Ms,
    double maximumMs) {
  static LatencyStatistics from(Histogram histogram) {
    return new LatencyStatistics(
        histogram.getMean() / 1_000.0,
        histogram.getMinValue() / 1_000.0,
        histogram.getValueAtPercentile(50) / 1_000.0,
        histogram.getValueAtPercentile(95) / 1_000.0,
        histogram.getValueAtPercentile(99) / 1_000.0,
        histogram.getMaxValue() / 1_000.0);
  }
}
