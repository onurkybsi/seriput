package io.seriput.benchmark;

import java.time.Duration;

record BenchmarkWorkload( // NOSONAR
    String host,
    int port,
    int connections,
    int warmupRequests,
    int measuredRequests,
    Duration socketTimeout,
    String key,
    byte[] payload) {
  BenchmarkWorkload {
    if (connections <= 0 || warmupRequests < 0 || measuredRequests <= 0) {
      throw new IllegalArgumentException("Request and connection count must be greater than zero!");
    }
    payload = payload.clone();
  }

  @Override
  public byte[] payload() {
    return payload.clone();
  }
}
