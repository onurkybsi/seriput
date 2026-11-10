package io.seriput.benchmark;

import static io.seriput.benchmark.BenchmarkExecutionContext.executionContext;
import static io.seriput.common.serialization.response.ResponseStatus.OK;
import static io.seriput.common.serialization.response.ResponseValueType.JSON_UTF8;
import static io.seriput.common.serialization.response.ResponseValueType.VOID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class GetBenchmarkRunner {
  private static final long MAX_TRACKABLE_LATENCY_MICROS = TimeUnit.SECONDS.toMicros(10);

  private final BenchmarkConnectionFactory connectionFactory;

  GetBenchmarkRunner(BenchmarkConnectionFactory connectionFactory) {
    this.connectionFactory = connectionFactory;
  }

  BenchmarkResult run(BenchmarkWorkload workload) throws Exception {
    byte[] putRequest = ProtocolFrames.put(workload.key(), "x");
    byte[] getRequest = ProtocolFrames.get(workload.key());
    var putResponse = new ExpectedResponse(OK.status(), VOID.typeId(), new byte[0]);
    var getResponse = new ExpectedResponse(OK.status(), JSON_UTF8.typeId(), workload.payload());
    var connections = openConnections(workload);
    try {
      connections.getFirst().execute(putRequest, putResponse);
      return executeRequests(workload, connections, getRequest, getResponse);
    } finally {
      closeConnections(connections);
    }
  }

  private List<BenchmarkConnection> openConnections(BenchmarkWorkload workload) throws IOException {
    var connections = new ArrayList<BenchmarkConnection>(workload.connections());
    try {
      for (int i = 0; i < workload.connections(); i++) {
        connections.add(connectionFactory.connect(workload));
      }
      return connections;
    } catch (IOException e) {
      closeConnectionsQuietly(connections);
      throw e;
    }
  }

  private BenchmarkResult executeRequests(
      BenchmarkWorkload workload,
      List<BenchmarkConnection> connections,
      byte[] getRequest,
      ExpectedResponse getResponse)
      throws Exception {
    // Run workers
    var context = executionContext(workload.connections(), MAX_TRACKABLE_LATENCY_MICROS);
    var workers = new ArrayList<Thread>(workload.connections());
    for (int i = 0; i < workload.connections(); i++) {
      int workerIndex = i;
      var connection = connections.get(i);
      workers.add(
          Thread.ofVirtual()
              .name("seriput-get-benchmark-" + workerIndex)
              .start(
                  () ->
                      runWorker(
                          workload,
                          workerIndex,
                          connection,
                          connections,
                          getRequest,
                          getResponse,
                          context)));
    }

    context.ready().await(); // All workers read..
    context.warmupStart().countDown();
    context.warmupDone().await(); // Wait for warm-up to complete
    if (context.failed()) {
      context.measurementStart().countDown();
      context.measurementDone().await(); // Wait for measurement to complete
      join(workers);
      throwFailure(context.failure().get());
    }

    long startedNanos = System.nanoTime();
    context.measurementStart().countDown();
    context.measurementDone().await();
    long elapsedNanos = System.nanoTime() - startedNanos;
    join(workers);
    if (context.failed()) {
      throwFailure(context.failure().get());
    }

    if (context.histogram().getTotalCount() != workload.measuredRequests()) {
      throw new IllegalStateException(
          "Expected %d measurements but recorded %d"
              .formatted(workload.measuredRequests(), context.histogram().getTotalCount()));
    }
    return BenchmarkResult.from(workload, elapsedNanos, context);
  }

  private static void runWorker( // NOSONAR
      BenchmarkWorkload workload,
      int workerIndex,
      BenchmarkConnection connection,
      List<BenchmarkConnection> connections,
      byte[] getRequest,
      ExpectedResponse getResponse,
      BenchmarkExecutionContext context) {
    context.ready().countDown(); // This worker is ready!

    // Worker warm-up
    try {
      context.warmupStart().await();
      int warmUpRequests =
          requestsForWorker(workload.warmupRequests(), workload.connections(), workerIndex);
      for (int i = 0; i < warmUpRequests && !context.failed(); i++) { // Stop if failure happens!
        connection.execute(getRequest, getResponse);
      }
    } catch (Throwable t) { // NOSONAR
      fail(t, context, connections);
    } finally {
      context.warmupDone().countDown();
    }
    // Worker measurement:
    try {
      context.measurementStart().await();
      int measuredRequests =
          requestsForWorker(workload.measuredRequests(), workload.connections(), workerIndex);
      for (int i = 0; i < measuredRequests && !context.failed(); i++) { // Stop if failure happens!
        long startedNanos = System.nanoTime();
        connection.execute(getRequest, getResponse);
        long latencyMicros =
            Math.max(1, TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - startedNanos));
        context.histogram().recordValue(Math.min(latencyMicros, MAX_TRACKABLE_LATENCY_MICROS));
      }
    } catch (Throwable t) { // NOSONAR
      fail(t, context, connections);
    } finally {
      context.measurementDone().countDown();
    }
  }

  static int requestsForWorker(int totalRequests, int workers, int workerIndex) {
    int base = totalRequests / workers;
    return base + (workerIndex < totalRequests % workers ? 1 : 0);
  }

  private static void fail(
      Throwable throwable,
      BenchmarkExecutionContext context,
      List<BenchmarkConnection> connections) {
    if (context.failure().compareAndSet(null, throwable)) {
      closeConnectionsQuietly(connections);
    }
  }

  private static void join(List<Thread> workers) throws InterruptedException {
    for (Thread worker : workers) {
      worker.join();
    }
  }

  private static void throwFailure(Throwable throwable) throws Exception { // NOSONAR
    if (throwable instanceof Exception exception) {
      throw exception;
    }
    if (throwable instanceof Error error) {
      throw error;
    }
    throw new IllegalStateException(throwable);
  }

  private static void closeConnections(List<BenchmarkConnection> connections) throws IOException {
    IOException failure = null;
    for (BenchmarkConnection connection : connections) {
      try {
        connection.close();
      } catch (IOException e) {
        if (failure == null) {
          failure = e;
        } else {
          failure.addSuppressed(e);
        }
      }
    }
    if (failure != null) {
      throw failure;
    }
  }

  private static void closeConnectionsQuietly(List<BenchmarkConnection> connections) {
    for (BenchmarkConnection connection : connections) {
      try {
        connection.close();
      } catch (IOException _) {
        // The original benchmark failure is more useful than a secondary close failure.
      }
    }
  }
}
