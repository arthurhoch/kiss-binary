# KissBinary No-Allocation Read Report

Date: 2026-05-07

Raw results:

- Before: [before/jmh-results.json](before/jmh-results.json)
- After: [after/jmh-results.json](after/jmh-results.json)

## Summary

This pass added caller-provided primitive array read methods and `BinaryReader` skip methods so hot paths can reuse buffers instead of allocating fresh arrays or materializing skipped bytes.

The allocation evidence is strong. Reused `short[16]` reads reduced measured allocation from 48 B/op to approximately 0 B/op in standalone `BinaryReader`, standalone `MappedBinaryReader`, and Rinha mapped random access benchmarks. Rinha mapped sequential read allocation dropped from about 480 KB/op to 2.36 B/op. Label bitset scanning dropped from about 321 KB/op to 184 B/op with `skipFully` plus reused `long[]`, and to 64 B/op with direct mapped offset reads.

Throughput is mixed. Reused `BinaryReader` `short[16]` was faster than the returning-array version, Rinha mapped random access was similar, label scanning improved sharply, but reused mapped sequential reads were slower than the returning-array path in this local run.

## API Added

`BinaryReader`:

- `readShortArray(short[] target)`
- `readShortArray(short[] target, int offset, int length)`
- `readIntArray(int[] target)`
- `readIntArray(int[] target, int offset, int length)`
- `readLongArray(long[] target)`
- `readLongArray(long[] target, int offset, int length)`
- `readFloatArray(float[] target)`
- `readFloatArray(float[] target, int offset, int length)`
- `readDoubleArray(double[] target)`
- `readDoubleArray(double[] target, int offset, int length)`
- `skipBytes(long byteCount)`
- `skipFully(long byteCount)`

`MappedBinaryReader`:

- `readShortArray(long fileOffset, short[] target)`
- `readShortArray(long fileOffset, short[] target, int targetOffset, int length)`
- `readIntArray(long fileOffset, int[] target)`
- `readIntArray(long fileOffset, int[] target, int targetOffset, int length)`
- `readLongArray(long fileOffset, long[] target)`
- `readLongArray(long fileOffset, long[] target, int targetOffset, int length)`
- `readFloatArray(long fileOffset, float[] target)`
- `readFloatArray(long fileOffset, float[] target, int targetOffset, int length)`
- `readDoubleArray(long fileOffset, double[] target)`
- `readDoubleArray(long fileOffset, double[] target, int targetOffset, int length)`

Existing array-returning methods were kept unchanged.

## Why It Was Added

The optimization report showed remaining allocation in Rinha-shaped benchmarks because array-returning methods allocate one primitive array per vector read. Label bitset scanning also allocated the skipped vector section because it used `readByteArray(labelOffset)` to advance to the labels.

The new APIs let callers:

- Reuse a target primitive array across reads.
- Read mapped primitive arrays into caller-owned storage.
- Skip cursor bytes without allocating a discarded byte array.

## Environment

- OS: macOS 26.4.1
- Kernel: Darwin 25.4.0
- CPU: Apple M4, 10 cores
- Architecture: arm64 / aarch64
- Java: OpenJDK 21.0.11 LTS, Temurin-21.0.11+10
- Maven: Apache Maven 3.9.15
- Project target: Java 17 via `--release 17`
- JVM flags: none supplied explicitly
- Date/time: 2026-05-07T22:28:00-0300

Full Rinha dataset optimization was not measured because `RINHA_DATASET_DIR` was not provided or required files were missing. The benchmarks here use the synthetic Rinha-shaped JMH dataset.

## Benchmark Commands

Both before and after used the same JMH regex and measurement settings. The before run produced 18 benchmark results because the new additive benchmark methods did not exist yet; the after run produced 26 benchmark results.

Before:

```bash
mvn -B -Pbenchmarks clean package
mvn -B -Pbenchmarks dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=target/benchmark-classpath.txt
rm -rf target/no-allocation-read-before-jmh-classes
mkdir -p target/no-allocation-read-before-jmh-classes
cp -R target/classes/. target/no-allocation-read-before-jmh-classes/
find target/generated-sources/annotations -name '*.java' -print > target/jmh-generated-sources.txt
javac --release 17 -cp "target/no-allocation-read-before-jmh-classes:$(cat target/benchmark-classpath.txt)" -d target/no-allocation-read-before-jmh-classes @target/jmh-generated-sources.txt
java -cp "target/no-allocation-read-before-jmh-classes:$(cat target/benchmark-classpath.txt)" org.openjdk.jmh.Main "$NOALLOC_BENCHMARKS" -wi 5 -i 5 -f 1 -w 300ms -r 300ms -prof gc -rf json -rff benchmark-results/no-allocation-read/before/jmh-results.json
```

After:

```bash
mvn -B -Pbenchmarks clean package
mvn -B -Pbenchmarks dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=target/benchmark-classpath.txt
rm -rf target/no-allocation-read-after-jmh-classes
mkdir -p target/no-allocation-read-after-jmh-classes
cp -R target/classes/. target/no-allocation-read-after-jmh-classes/
find target/generated-sources/annotations -name '*.java' -print > target/jmh-generated-sources.txt
javac --release 17 -cp "target/no-allocation-read-after-jmh-classes:$(cat target/benchmark-classpath.txt)" -d target/no-allocation-read-after-jmh-classes @target/jmh-generated-sources.txt
java -cp "target/no-allocation-read-after-jmh-classes:$(cat target/benchmark-classpath.txt)" org.openjdk.jmh.Main "$NOALLOC_BENCHMARKS" -wi 5 -i 5 -f 1 -w 300ms -r 300ms -prof gc -rf json -rff benchmark-results/no-allocation-read/after/jmh-results.json
```

The actual `NOALLOC_BENCHMARKS` regex was:

```text
io.github.arthurhoch.kissbinary.benchmarks.KissBinaryBenchmark.arrayRead|io.github.arthurhoch.kissbinary.benchmarks.KissBinaryBenchmark.binaryReaderReadShortArrayReturning|io.github.arthurhoch.kissbinary.benchmarks.KissBinaryBenchmark.binaryReaderReadShortArrayReused|io.github.arthurhoch.kissbinary.benchmarks.KissBinaryBenchmark.mappedBinaryReaderReadShortArrayReturning|io.github.arthurhoch.kissbinary.benchmarks.KissBinaryBenchmark.mappedBinaryReaderReadShortArrayReused|io.github.arthurhoch.kissbinary.benchmarks.rinha.RinhaBinaryBenchmark.randomAccess_MappedBinaryReader|io.github.arthurhoch.kissbinary.benchmarks.rinha.RinhaBinaryBenchmark.randomAccess_MappedBinaryReader_reusedArray|io.github.arthurhoch.kissbinary.benchmarks.rinha.RinhaBinaryBenchmark.sequentialRead_MappedBinaryReader|io.github.arthurhoch.kissbinary.benchmarks.rinha.RinhaBinaryBenchmark.sequentialRead_MappedBinaryReader_reusedArray|io.github.arthurhoch.kissbinary.benchmarks.rinha.RinhaBinaryBenchmark.labelBitsetScan_KissBinary|io.github.arthurhoch.kissbinary.benchmarks.rinha.RinhaBinaryBenchmark.labelBitsetScan_KissBinary_skipFullyReusedArray|io.github.arthurhoch.kissbinary.benchmarks.rinha.RinhaBinaryBenchmark.labelBitsetScan_MappedBinaryReader_reusedArray
```

## Benchmark Before/After

| Benchmark | Before | After | Delta | Allocation before | Allocation after |
|---|---:|---:|---:|---:|---:|
| `BinaryReader.readShortArray(1024)` returning array | 2.97M ops/s | 5.33M ops/s | +79.4% | 2,064 B/op | 2,064 B/op |
| Rinha mapped random, returning `short[16]` | 89.38M ops/s | 82.20M ops/s | -8.0% | 48 B/op | 48 B/op |
| Rinha mapped random, reused `short[16]` | 89.38M ops/s baseline old path | 83.37M ops/s | -6.7% | 48 B/op | ~0 B/op |
| Rinha mapped sequential, returning `short[16]` | 15.39K ops/s | 14.45K ops/s | -6.1% | 480,002 B/op | 480,002 B/op |
| Rinha mapped sequential, reused `short[16]` | 15.39K ops/s baseline old path | 9.87K ops/s | -35.9% | 480,002 B/op | 2.36 B/op |
| Label scan old skip/read allocation path | 85.00K ops/s | 96.34K ops/s | +13.4% | 321,560 B/op | 321,560 B/op |
| Label scan using `skipFully` + reused `long[]` | 85.00K ops/s baseline old path | 10.88M ops/s | +12,699.3% | 321,560 B/op | 184 B/op |
| Label scan using mapped offset + reused `long[]` | 85.00K ops/s baseline old path | 12.76M ops/s | +14,914.3% | 321,560 B/op | 64 B/op |

## After-Only API Comparison

These benchmarks compare the new target-array API against the returning-array API in the same after run.

| Benchmark | Returning array | Reused target | Throughput delta | Returning allocation | Reused allocation |
|---|---:|---:|---:|---:|---:|
| `BinaryReader` `short[16]` | 19.24M ops/s | 22.41M ops/s | +16.5% | 48 B/op | ~0 B/op |
| `MappedBinaryReader` `short[16]` | 153.95M ops/s | 88.51M ops/s | -42.5% | 48 B/op | ~0 B/op |
| Rinha mapped random access | 82.20M ops/s | 83.37M ops/s | +1.4% | 48 B/op | ~0 B/op |
| Rinha mapped sequential read | 14.45K ops/s | 9.87K ops/s | -31.7% | 480,002 B/op | 2.36 B/op |
| Label scan old vs `skipFully` reused | 96.34K ops/s | 10.88M ops/s | +11,191.9% | 321,560 B/op | 184 B/op |
| Label scan old vs mapped reused | 96.34K ops/s | 12.76M ops/s | +13,146.0% | 321,560 B/op | 64 B/op |

## Allocation Discussion

Allocation was measured with the JMH GC profiler (`gc.alloc.rate.norm`). The target-array methods remove the returned primitive array allocation in the measured short-vector paths. The remaining small nonzero values in some reused benchmarks are profiler noise or surrounding benchmark/object setup costs; this report does not claim universal zero allocation.

The most important allocation reductions were:

- Rinha random mapped access: 48 B/op to approximately 0 B/op.
- Rinha sequential mapped read: about 480 KB/op to 2.36 B/op.
- Label scan skip/read path: about 321 KB/op to 184 B/op with `skipFully`, or 64 B/op with mapped offset reads.

## Tests Added

Added tests for:

- Target-array reads matching existing array-returning methods.
- Target offsets and partial target ranges.
- Null target failures.
- Invalid target offset/length failures.
- Truncated source failures.
- Mapped offset target-array reads.
- `skipBytes` partial skipping and position advancement.
- `skipFully` exact skipping and truncated-input failure.

The final `mvn -B clean verify` run passed with 120 tests, 0 failures, 0 errors, and 6 skipped full-dataset tests.

## Compatibility Impact

This is an additive public API change:

- No existing public methods were removed or changed.
- Existing array-returning methods remain available.
- Existing behavior and validation remain covered by tests.
- Production dependencies remain zero.
- No `Unsafe`, internal JDK APIs, reflection, object serialization, schema engine, compression, or async IO was added.

## Quality Gates

| Command | Result |
|---|---|
| `mvn -B clean verify` | Pass, 120 tests run, 0 failures, 0 errors, 6 skipped |
| `mvn -B javadoc:javadoc` | Pass |
| `mvn -B dependency:list -DincludeScope=compile` | Pass, resolved dependencies: none |
| `mvn -B -Pbenchmarks clean package` | Pass |
| No-allocation JMH command | Pass, before 18 results and after 26 results |

## Should These Be Documented As Performance APIs?

Yes, but narrowly. These methods are justified as allocation-control APIs for hot paths that reuse caller-owned buffers. The benchmarks prove clear allocation reductions and major label-scan gains.

They should not be documented as universal throughput improvements. In this run, reused mapped sequential reads allocated dramatically less but were slower, and standalone mapped reused `short[16]` reads were also slower than returning arrays. Documentation should say these APIs let callers trade allocation pressure for explicit buffer reuse, and users should benchmark their own hot paths when throughput is the primary goal.

## Recommendation

Keep the APIs. They are small, explicit, additive, dependency-free, and directly address measured allocation bottlenecks. For release notes, describe them as caller-provided target-array read methods and skip helpers for allocation-sensitive code. Do not claim broad speedups beyond the measured label-scan and random-access cases.
