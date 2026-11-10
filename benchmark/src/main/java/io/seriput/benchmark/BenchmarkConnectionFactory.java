package io.seriput.benchmark;

import java.io.IOException;

@FunctionalInterface
interface BenchmarkConnectionFactory {
  BenchmarkConnection connect(BenchmarkWorkload workload) throws IOException;
}
