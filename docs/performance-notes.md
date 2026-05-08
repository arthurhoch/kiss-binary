---
layout: default
---

# Performance Notes

**Status: Initial implementation complete; local JMH benchmark results captured.**

Benchmarks live under `src/jmh/java`, with local results saved under `benchmark-results/`.

## Philosophy

KissBinary's performance goals come from a small direct API:

- Primitives are read and written through simple byte-buffer operations.
- Arrays use bulk `ByteBuffer` operations.
- Bounds checking happens before reads.
- There is no reflection, object serialization, or schema runtime.

## Benchmark Approach

Benchmarks use JMH (Java Microbenchmark Harness) in the `benchmarks` Maven profile. JMH dependencies are only active for benchmark builds.

Benchmark categories:

- Primitive read/write throughput
- Array read/write throughput
- Header validation
- Memory-mapped random offset reads
- Sequential scan
- Baseline comparison with `DataInputStream` / `ByteBuffer`

## Expected Performance Characteristics

Measured local results are summarized in [JMH_RESULTS.md](../benchmark-results/JMH_RESULTS.md). The current evidence is scenario-specific:

- Primitive reads are ahead of `DataInputStream` in this run, while heap `ByteBuffer` is ahead of kiss-binary in several read scenarios.
- Primitive and array writes are not consistently ahead of heap `ByteBuffer`.
- `MappedBinaryReader` shows low allocation in random offset reads, but direct and heap `ByteBuffer` have higher throughput in this run.
- Sequential scan is ahead of `DataInputStream`, but behind heap and direct `ByteBuffer`.

Do not claim broader performance properties without quoting the specific benchmark and environment.

## Tips for Users

- Use `MappedBinaryReader` for large files that are read many times.
- Use `BinaryReader` for small files or one-time reads.
- Use big-endian unless the format requires little-endian.
- Batch array operations instead of reading one primitive at a time.
- Avoid creating new reader/writer instances in tight loops.

## See Also

- [PERFORMANCE.md](../PERFORMANCE.md) — detailed performance goals and benchmark results.
- [JMH_RESULTS.md](../benchmark-results/JMH_RESULTS.md) — local benchmark summary.
- [.github/architecture/04-performance-rules.md](../.github/architecture/04-performance-rules.md) — hot path rules.
