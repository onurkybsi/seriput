# Benchmark

Throughput benchmarks for the Seriput cache server, measuring GET, PUT, and DELETE operations.

## Requirements

- Java 25
- No external server needed — benchmarks start an embedded server on port 9090

## Running

```bash
./gradlew :benchmark:runGetThroughput
./gradlew :benchmark:runPutThroughput
./gradlew :benchmark:runDeleteThroughput
```

Pass custom parameters with `-Pconcurrency=64 -PtargetRps=80000`. Defaults: `concurrency=128`, `targetRps=120000`.

## Results

Each benchmark writes results in JSONL format:

- `get-throughput-results.jsonl`
- `put-throughput-results.jsonl`
- `delete-throughput-results.jsonl`
