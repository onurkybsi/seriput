# GET benchmark

This module measures GET throughput and latency against a Seriput server running at
`127.0.0.1:8080`.

The fixed workload uses 50 persistent connections, 10,000 warm-up requests, and 100,000 measured
requests for one hot key containing the JSON value `"x"`.

## Running

Start the server:

```bash
./gradlew :server:run
```

In another terminal, run the benchmark:

```bash
./gradlew :benchmark:runGetBenchmark
```

Each successful run prints a summary and appends a row to `benchmark/get-benchmark-results.csv`.

For comparisons, run the benchmark several times before and after a change and compare median
throughput and latency percentiles under the same machine and JDK conditions.
