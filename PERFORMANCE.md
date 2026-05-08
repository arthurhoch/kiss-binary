# KissBinary — Performance

**Status: Initial implementation complete; local JMH benchmark results captured.**

Benchmarks exist under `src/jmh/java` and local results are saved under `benchmark-results/`.

## Performance Goals

KissBinary's performance goals come from a small direct API, not from hiding complexity behind a framework runtime.

Target characteristics:

- Primitive reads and writes are direct `ByteBuffer` operations.
- No reflection, object serialization, or schema runtime between the user and the bytes.
- No reflection, no object serialization, no annotation processing.
- Bounds checking adds minimal overhead (one comparison per read).
- Array operations use bulk `ByteBuffer` puts and gets.
- Memory-mapped reads are competitive with direct `ByteBuffer` access.

Do not claim specific performance numbers until benchmarks exist.

## Hot Path Principles

The hot path is any method that reads or writes a primitive or array. These methods must:

1. Never allocate objects on the hot path.
2. Never use reflection.
3. Never box primitives.
4. Never perform hidden IO.
5. Never catch and swallow exceptions internally.
6. Bounds-check before the read, not after.

## Allocation Minimization

- `BinaryWriter` uses a single growable byte buffer internally.
- `BinaryReader` wraps a `ByteBuffer`.
- `MappedBinaryReader` wraps a `MappedByteBuffer`.
- Array reads allocate the result array (unavoidable), but use bulk operations to fill it.

## Bounds Checks

Bounds checking must:

- Happen before the read or write, not after.
- Be a single comparison: `remaining() >= bytesRequired`.
- Throw `BinaryFormatException` with the current position, required bytes, and remaining bytes.
- Never silently truncate.

## Primitive Arrays

Array operations must use `ByteBuffer` bulk methods:

- `ByteBuffer.get(byte[])` for byte arrays.
- `ByteBuffer.asShortBuffer().get(short[])` for short arrays.
- `ByteBuffer.asIntBuffer().get(int[])` for int arrays.
- `ByteBuffer.asLongBuffer().get(long[])` for long arrays.
- `ByteBuffer.asFloatBuffer().get(float[])` for float arrays.
- `ByteBuffer.asDoubleBuffer().get(double[])` for double arrays.
- `ByteBuffer.asCharBuffer().get(char[])` for char arrays.

These avoid per-element loops and leverage JVM-optimized buffer operations.

## File IO vs Memory-Mapped IO

### BinaryWriter / BinaryReader

- Operate on `byte[]` and `ByteBuffer` in memory.
- No file IO in the core read/write path.
- Users call `writeTo(OutputStream)` to persist data.
- Users create `BinaryReader` from `byte[]` loaded from files.

### MappedBinaryReader

- Uses `FileChannel.map()` for read-only memory mapping.
- Position-based reads: `readInt(offset)`, no cursor advancement.
- Suitable for large static files that are read many times.
- Not suitable for files that change during reading.

Performance expectation: mmap reads should be competitive with direct `ByteBuffer` access for random-access workloads. Sequential reads may not benefit from mmap.

## Why No Reflection / Object Serialization

Reflection and object serialization add:

- Per-access overhead (field lookup, accessibility checks, boxing).
- Hidden allocation (intermediate objects, proxy objects, reflection arrays).
- Unpredictable performance (JIT may not optimize reflective access as well).
- Complexity that conflicts with the KISS principle.

KissBinary reads and writes primitives directly. The user defines the layout. The library moves the bytes. Nothing more.

## Benchmarks

### JMH Setup

Benchmarks use JMH (Java Microbenchmark Harness) in the `benchmarks` Maven profile:

```bash
mvn -B -Pbenchmarks clean package
java -jar target/benchmarks.jar -wi 5 -i 5 -f 2 -w 200ms -r 200ms -prof gc -rf json -rff benchmark-results/jmh-results.json
```

The profile is opt-in. JMH dependencies are only active for benchmark builds and do not appear in the normal compile-scope dependency list.

In the 2026-05-05 local workspace, the shaded benchmark jar built but did not execute cleanly because generated JMH classes under `target/classes` were inconsistent. The measured run used the isolated classpath command recorded in [benchmark-results/environment.md](benchmark-results/environment.md). Fixing the jar execution path remains release work.

### Benchmark Categories

1. **Primitive writes**: `writeInt`, `writeLong`, `writeDouble`, and a mixed primitive record.
2. **Primitive reads**: `readInt`, `readLong`, `readDouble`, and a mixed primitive record.
3. **Array writes**: `short[]`, `int[]`, `long[]`, `float[]`, and `double[]`.
4. **Array reads**: `short[]`, `int[]`, `long[]`, `float[]`, and `double[]`.
5. **Header validation**: `expectMagic` and `expectVersion`.
6. **Mapped random offset reads**: `readInt(offset)`, `readLong(offset)`, and `readDouble(offset)`.
7. **Sequential scan**: Scan fixed-size primitive records with `BinaryReader`, `MappedBinaryReader`, `ByteBuffer`, and `DataInputStream`.
8. **Baseline comparison**: Compare with `DataInputStream` / `DataOutputStream`, heap `ByteBuffer`, and direct `ByteBuffer`.

### What Should Be Measured

- Throughput (operations per second).
- Allocation rate (bytes per operation) via JMH GC profiler.
- Fair baseline comparison for the same data layout and endianness where possible.

### Environment Documentation

Each benchmark result must include:

- JVM version and vendor.
- OS and architecture.
- Heap configuration.
- Warmup and measurement durations.
- Number of threads and forks.
- Raw JMH JSON output.

### Reporting Rules

- Do not claim performance without linking to the benchmark result and environment.
- Label measured scenarios separately.
- Include baseline comparisons.
- Local directional results only, not universal production guarantees.

## Current Result Summary

The local run on 2026-05-05 produced numeric evidence, but it does not support a broad "extremely high performance" claim. Examples:

- `BinaryReader.readInt`: 338.55M ops/s vs 158.76M ops/s for `DataInputStream` and 396.66M ops/s for heap `ByteBuffer`.
- `MappedBinaryReader.readLong(offset)`: 113.68M ops/s vs 115.67M ops/s for heap `ByteBuffer` and 155.75M ops/s for direct `ByteBuffer`.
- Sequential scan: `BinaryReader` 45.57K ops/s, `MappedBinaryReader` 69.46K ops/s, heap `ByteBuffer` 83.98K ops/s, direct `ByteBuffer` 122.99K ops/s.

See [benchmark-results/JMH_RESULTS.md](benchmark-results/JMH_RESULTS.md) for the full measured summary.

## Optimization Pass: 2026-05-05

An evidence-based optimization pass was captured under [benchmark-results/optimization/](benchmark-results/optimization/).

Retained changes were limited to measured hot paths:

- `BinaryWriter` primitive scalar writes now use direct byte stores instead of a per-writer `ByteBuffer` scratch path.
- `BinaryReader` and `MappedBinaryReader` successful magic validation avoid normal-path byte-array allocation.
- `MappedBinaryReader` uses absolute byte reads and small typed-array offset reads to avoid duplicate/slice/view objects in Rinha-shaped access.

Key local before/after results on macOS 26.4.1, Apple M4, OpenJDK 21.0.11:

| Benchmark | Before | After | Delta |
|---|---:|---:|---:|
| `BinaryWriter.writeInt` | 69.15M ops/s, 104 B/op | 159.45M ops/s, 24 B/op | +130.6% |
| `BinaryWriter.writeLong` | 63.41M ops/s, 104 B/op | 143.39M ops/s, 24 B/op | +126.1% |
| `BinaryReader.expectMagic("KB")` | 158.62M ops/s, 48 B/op | 288.92M ops/s, ~0 B/op | +82.1% |
| Rinha synthetic mapped random access | 55.38M ops/s, 112 B/op | 84.49M ops/s, 48 B/op | +52.6% |
| Rinha synthetic mapped sequential read | 6.00K ops/s, 1,120,006 B/op | 13.99K ops/s, 480,002 B/op | +133.2% |

The same run also showed unchanged or lower results in some scenarios: `int[]` array write was 2.29M to 2.03M ops/s, `MappedBinaryReader.readLong(offset)` was 166.00M to 156.55M ops/s, and `BinaryReader` sequential scan was 49.81K to 48.54K ops/s. Do not generalize the improvements beyond the measured benchmarks.

See [benchmark-results/optimization/OPTIMIZATION_REPORT.md](benchmark-results/optimization/OPTIMIZATION_REPORT.md) for the full comparison, allocation discussion, rejected experiments, and final recommendation.

## No-Allocation Read APIs: 2026-05-07

KissBinary now includes caller-provided target-array read methods for `BinaryReader` and `MappedBinaryReader`, plus `BinaryReader.skipBytes(long)` and `skipFully(long)`. These APIs are intended for hot paths that reuse buffers instead of allocating a fresh primitive array per read.

Local JMH evidence is recorded in [benchmark-results/no-allocation-read/NO_ALLOCATION_READ_REPORT.md](benchmark-results/no-allocation-read/NO_ALLOCATION_READ_REPORT.md). The results justify documenting these as allocation-control APIs, not as universal throughput wins. In the measured Rinha-shaped benchmarks, reused mapped random access reduced allocation from 48 B/op to approximately 0 B/op with similar throughput, while reused mapped sequential read reduced allocation from about 480 KB/op to 2.36 B/op but had lower throughput. Label bitset scanning improved because `skipFully` avoids allocating the skipped vector bytes.

## Rinha Dataset Benchmark

KissBinary includes a real-world performance validation using the Rinha de Backend 2026 reference dataset (3,000,000 labeled vectors).

This benchmark:

- Converts `references.json.gz` (gzip JSON) into a compact binary file (`references.kbin`) using kiss-binary.
- Reads and validates the binary file using `BinaryReader` and `MappedBinaryReader`.
- Measures sequential read, mapped sequential read, random vector access, header validation, and label bitset scan.
- Compares kiss-binary against raw `ByteBuffer` baselines.

See [docs/rinha-dataset-benchmark.md](docs/rinha-dataset-benchmark.md) for setup and commands.

Benchmark results are reported under [benchmark-results/rinha/](benchmark-results/rinha/). Do not claim results until the benchmark has been executed.
