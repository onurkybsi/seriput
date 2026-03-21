package io.seriput.benchmark;

import io.seriput.client.SeriputClient;

final class DeleteThroughputBenchmark extends ThroughputBenchmark {

  DeleteThroughputBenchmark(BenchmarkConfig config) {
    super(config);
  }

  @Override
  String resultFile() {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  void prepopulate(SeriputClient client) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  void fireRequest(SeriputClient client, Measurement measurement) {
    throw new UnsupportedOperationException("Not yet implemented");
  }
}
