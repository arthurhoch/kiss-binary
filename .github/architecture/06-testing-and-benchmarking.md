# 06 — Testing and Benchmarking

This document defines the test categories and benchmark plan for KissBinary.

**Initial implementation, tests, and JMH benchmarks exist. Local benchmark results are saved under `benchmark-results/`.**

## Test Categories

### Primitive Roundtrip Tests

For each primitive type (`byte`, `short`, `int`, `long`, `float`, `double`, `char`, `boolean`):

- Write a value, read it back, assert equal.
- Write min value, read back, assert equal.
- Write max value, read back, assert equal.
- Write zero, read back, assert equal.
- Write negative values (for signed types), read back, assert equal.

### Endianness Tests

For each multi-byte primitive type:

- Write in big-endian, read in big-endian, assert equal.
- Write in little-endian, read in little-endian, assert equal.
- Verify byte order is correct by inspecting raw bytes.
- Write in one endianness, verify reading in the other produces a different value.

### Malformed Input Tests

- Invalid magic bytes produce `BinaryFormatException` with expected vs actual.
- Invalid version produces `BinaryFormatException` with expected vs actual.
- Invalid boolean byte (not 0 or 1) produces `BinaryFormatException`.
- Corrupt data produces `BinaryFormatException` with offset.

### Truncated File Tests

- File truncated before magic bytes produce `BinaryFormatException` with EOF context.
- File truncated before version produces `BinaryFormatException` with EOF context.
- File truncated in the middle of a primitive produces `BinaryFormatException` with EOF context.
- File truncated in the middle of an array produces `BinaryFormatException` with EOF context.
- Empty file produces `BinaryFormatException` on first read.

### Array Bounds Tests

- Negative array count produces `BinaryFormatException`.
- Array count exceeding remaining bytes produces `BinaryFormatException`.
- Zero-length array read returns empty array.
- Maximum reasonable array size works.
- Partial array read at end of buffer produces `BinaryFormatException`.

### Header Validation Tests

- Valid magic and version passes without exception.
- Invalid magic throws with expected vs actual bytes.
- Invalid version throws with expected vs actual version.
- Multiple headers in sequence validate correctly.
- Header validation at non-zero offset.

### Memory-Mapped Tests

Where platform supports:

- Mmap roundtrip: write file, mmap, read, assert values.
- Mmap header validation.
- Mmap array reads.
- Mmap bounds checking (offset out of range).
- Mmap close behavior.
- Mmap with large file (multi-MB).
- Platform-specific behavior documentation (Windows file locking, etc.).

## Testing Rules

1. All tests use JUnit 5.
2. Tests must not require network access.
3. Tests must be deterministic.
4. Every public method must have at least one test.
5. Test failures must produce clear messages.

## Benchmark Plan

### JMH Setup

Benchmarks run in a separate Maven profile:

```bash
mvn -B -Pbenchmarks clean package
java -jar target/benchmarks.jar -wi 5 -i 5 -f 2 -w 200ms -r 200ms -prof gc -rf json -rff benchmark-results/jmh-results.json
```

JMH dependencies are only active in the opt-in benchmark profile. They do not ship in the published library artifact.

### Benchmark Scenarios

1. **Primitive write throughput**: Write each primitive type. Measure ops/sec.
2. **Primitive read throughput**: Read each primitive type. Measure ops/sec.
3. **Array write throughput**: Write primitive arrays. Measure ops/sec and allocation.
4. **Array read throughput**: Read primitive arrays. Measure ops/sec and allocation.
5. **Endianness behavior**: Covered by unit tests; add dedicated benchmark cases only if a future optimization depends on it.
6. **Header validation overhead**: Measure cost of magic/version validation. Measure ops/sec.
7. **Memory-mapped read throughput**: Read random offsets via mmap and compare with heap/direct `ByteBuffer`.
8. **Baseline comparison**: Compare with `DataInputStream` / `DataOutputStream` and raw `ByteBuffer`.

### Benchmark Reporting

Each benchmark result must include:

- JVM version and vendor.
- OS and architecture.
- Heap configuration.
- Warmup duration and iterations.
- Measurement duration and iterations.
- Number of forks and threads.
- Raw JMH output (throughput, error, percentiles).

### Benchmark Rules

- Do not claim benchmarks exist until they have been run.
- Do not cherry-pick results.
- Include baselines for comparison.
- Local directional results only.
- Label measured hot paths separately.
