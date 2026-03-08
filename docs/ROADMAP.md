# Roadmap

## Next Release
- [ ] Server doesn't wait for in-flight requests to finish when shutting down
- [ ] `SeriputConnection`'s inbound queue has no capacity configured which ends up with `OutOfMemoryError`. Consider _TCP backpressure_ for the fix.
- [ ] 

## Future
- [ ] Configuration values taken from environment variables only
- [ ] Take optional header values from clients (e.g. for tracing)
- 