# KissBinary Optimization Report

Date: 2026-05-05

Raw results:

- Baseline: [baseline/jmh-results.json](baseline/jmh-results.json)
- After: [after/jmh-results.json](after/jmh-results.json)
- Baseline environment: [baseline/environment.md](baseline/environment.md)
- After environment: [after/environment.md](after/environment.md)

## 1. Executive Summary

This pass optimized three measured hot paths: primitive scalar writes in `BinaryWriter`, successful magic validation in `BinaryReader` and `MappedBinaryReader`, and small-array/byte access in `MappedBinaryReader`.

Performance improved in the targeted benchmarks. `writeInt` improved from 69.15M to 159.45M ops/s, magic validation improved from 158.62M to 288.92M ops/s, Rinha synthetic mapped random access improved from 55.38M to 84.49M ops/s, and Rinha synthetic mapped sequential read improved from 6.00K to 13.99K ops/s.

The final Maven verification passed, public API was unchanged, and normal compile-scope dependencies remained none. Results are scenario-specific; several unrelated benchmarks were flat or lower, and no broad performance claim is justified.

## 2. Baseline Environment

- OS: macOS 26.4.1
- Kernel: Darwin 25.4.0
- CPU: Apple M4
- Architecture: arm64 / aarch64
- Java: OpenJDK 21.0.11 LTS, Temurin-21.0.11+10
- Maven: Apache Maven 3.9.15
- Project target: Java 17 via `--release 17`
- JVM flags: none supplied explicitly
- Date/time: 2026-05-05T18:54:08-0300

Baseline commands:

```bash
mvn -B clean verify
mvn -B javadoc:javadoc
mvn -B dependency:list -DincludeScope=compile
mvn -B -Pbenchmarks clean package
java -jar target/benchmarks.jar -wi 5 -i 5 -f 2 -w 200ms -r 200ms -prof gc -rf json -rff benchmark-results/optimization/baseline/jmh-results.json
mvn -B -Pbenchmarks compile dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=target/benchmark-classpath.txt
mkdir -p benchmark-results/optimization/baseline/jmh-classes
cp -R target/classes/. benchmark-results/optimization/baseline/jmh-classes/
find target/generated-sources/annotations -name '*.java' -print > target/jmh-generated-sources.txt
javac --release 17 -cp "benchmark-results/optimization/baseline/jmh-classes:$(cat target/benchmark-classpath.txt)" -d benchmark-results/optimization/baseline/jmh-classes @target/jmh-generated-sources.txt
java -cp "benchmark-results/optimization/baseline/jmh-classes:$(cat target/benchmark-classpath.txt)" org.openjdk.jmh.Main -wi 5 -i 5 -f 1 -w 200ms -r 200ms -prof gc -rf json -rff benchmark-results/optimization/baseline/jmh-results.json
```

The shaded benchmark jar built but did not produce usable baseline results in this workspace, so the baseline comparison uses the isolated JMH classpath run.

## 3. Baseline Results

Key baseline metrics:

- `BinaryWriter.writeInt`: 69.15M ops/s, 104 B/op.
- `BinaryWriter.writeLong`: 63.41M ops/s, 104 B/op.
- `BinaryWriter.writeDouble`: 62.47M ops/s, 104 B/op.
- `BinaryWriter` mixed primitive record: 61.32M ops/s, 144 B/op.
- `BinaryReader.expectMagic("KB")`: 158.62M ops/s, 48 B/op.
- Rinha synthetic header validation: 166.02M ops/s, 48 B/op.
- Rinha synthetic mapped random access: 55.38M ops/s, 112 B/op.
- Rinha synthetic mapped sequential read: 6.00K ops/s, 1,120,006 B/op.
- Rinha synthetic `BinaryReader` sequential read: 14.01K ops/s, 480,178 B/op.
- `int[]` array write: 2.29M ops/s, 8,416 B/op.

Full Rinha dataset optimization was not measured because RINHA_DATASET_DIR was not provided or required files were missing.

## 4. Bottlenecks Found

- `BinaryWriter` primitive writes used a per-writer `ByteBuffer` scratch path. Code inspection showed avoidable buffer-position work for every scalar write, and baseline writer benchmarks showed lower throughput and higher allocation than the comparable heap `ByteBuffer` small-message benchmark.
- `BinaryReader.expectMagic(String)` and `MappedBinaryReader.expectMagic(String)` allocated ASCII bytes on the normal success path. `BinaryReader.validateMagic(byte[])` also allocated actual bytes before knowing whether an error would be thrown. Baseline header validation measured 48 B/op.
- `MappedBinaryReader.readByteArray` and `readBytes` used slice-based access for offset reads. Code inspection found avoidable `ByteBuffer` duplicate/slice objects.
- `MappedBinaryReader` small primitive array offset reads used duplicate/slice/view buffer creation. The Rinha synthetic mapped benchmarks repeatedly read short vectors of 16 elements, making this allocation measurable: mapped sequential read reported 1,120,006 B/op.
- Rinha label bitset scanning allocates because the benchmark/API path reads skipped bytes into arrays. This was not changed because adding a public skip API would be a feature/API change.

## 5. Changes Made

| File | Reason | Expected effect | Risk | Tests covering it |
|---|---|---|---|---|
| `src/main/java/io/github/arthurhoch/kissbinary/BinaryWriter.java` | Replace primitive scalar `ByteBuffer` scratch writes with direct byte stores and cache the endian flag. | Reduce scalar write overhead and small-message allocation. | Manual endian conversion could corrupt binary layout. | Existing primitive roundtrip and endian tests; `BinaryWriterTest.writeMagicPreservesAsciiEncodingFallback`. |
| `src/main/java/io/github/arthurhoch/kissbinary/BinaryWriter.java` | Fast-path ASCII `writeMagic(String)` without `String.getBytes` on ASCII input. | Avoid one small allocation for common ASCII magic writes. | Non-ASCII fallback behavior must remain US-ASCII replacement. | `BinaryWriterTest.writeMagicPreservesAsciiEncodingFallback`. |
| `src/main/java/io/github/arthurhoch/kissbinary/BinaryReader.java` | Compare magic bytes directly and allocate the `actual` byte array only on mismatch. Fast-path ASCII `expectMagic(String)`. | Remove normal-path magic validation allocation. | Mismatch position and error message must remain useful. | Magic success/mismatch/truncation tests and `BinaryReaderTest.expectMagicPreservesAsciiEncodingFallback`. |
| `src/main/java/io/github/arthurhoch/kissbinary/BinaryReader.java` | Use a single short view for small sequential `short[]` reads. | Avoid extra slice object in Rinha-shaped 16-short reads while preserving bulk conversion. | Cursor advancement could be wrong. | Existing array and Rinha synthetic tests. |
| `src/main/java/io/github/arthurhoch/kissbinary/MappedBinaryReader.java` | Use absolute bulk byte-buffer gets for byte array/range reads. | Avoid duplicate/slice allocation for offset byte copies. | Offset bounds and target offsets must remain correct. | Existing mapped byte range and out-of-bounds tests. |
| `src/main/java/io/github/arthurhoch/kissbinary/MappedBinaryReader.java` | Use absolute primitive gets for small offset typed arrays and allocate mismatch magic bytes only on errors. | Reduce mapped small-array allocation and improve Rinha random/sequential access. | Manual offset math could be wrong. | Existing mapped array, endianness, magic, and Rinha synthetic tests; `MappedBinaryReaderTest.expectMagicPreservesAsciiEncodingFallback`. |

No public API methods were added, removed, or renamed.

## 6. After Results

Key after metrics:

- `BinaryWriter.writeInt`: 159.45M ops/s, 24 B/op.
- `BinaryWriter.writeLong`: 143.39M ops/s, 24 B/op.
- `BinaryWriter.writeDouble`: 142.34M ops/s, 24 B/op.
- `BinaryWriter` mixed primitive record: 79.81M ops/s, 120 B/op.
- `BinaryReader.expectMagic("KB")`: 288.92M ops/s, approximately 0 B/op in the GC profiler.
- Rinha synthetic header validation: 334.32M ops/s, approximately 0 B/op in the GC profiler.
- Rinha synthetic mapped random access: 84.49M ops/s, 48 B/op.
- Rinha synthetic mapped sequential read: 13.99K ops/s, 480,002 B/op.
- Rinha synthetic `BinaryReader` sequential read: 14.25K ops/s, 480,178 B/op.
- `int[]` array write: 2.03M ops/s, 8,416 B/op.

## 7. Before/After Comparison

| Benchmark | Before | After | Delta | Interpretation |
|---|---:|---:|---:|---|
| Magic validation | 158.62M ops/s | 288.92M ops/s | +82.1% | Improved by removing success-path allocation. |
| Rinha header validation | 166.02M ops/s | 334.32M ops/s | +101.4% | Improved for the same magic-validation reason. |
| `writeInt` | 69.15M ops/s | 159.45M ops/s | +130.6% | Improved by direct scalar byte stores. |
| `writeLong` | 63.41M ops/s | 143.39M ops/s | +126.1% | Improved by direct scalar byte stores. |
| `writeDouble` | 62.47M ops/s | 142.34M ops/s | +127.9% | Improved through the optimized long write path. |
| Mixed writer record | 61.32M ops/s | 79.81M ops/s | +30.2% | Improved, though still allocates final output bytes in this benchmark. |
| Rinha mapped random access | 55.38M ops/s | 84.49M ops/s | +52.6% | Improved by avoiding slice/view objects for small mapped arrays. |
| Rinha mapped sequential read | 6.00K ops/s | 13.99K ops/s | +133.2% | Improved by small mapped array specialization. |
| Rinha `BinaryReader` sequential read | 14.01K ops/s | 14.25K ops/s | +1.7% | Essentially unchanged; below the threshold for a meaningful claim. |
| `int[]` array write | 2.29M ops/s | 2.03M ops/s | -11.4% | Lower in this run; array write code was not kept changed after rejected attempts. |
| `MappedBinaryReader.readLong(offset)` | 166.00M ops/s | 156.55M ops/s | -5.7% | Slightly lower and noisy; direct scalar mapped path was not intentionally changed. |
| `BinaryReader` sequential scan | 49.81K ops/s | 48.54K ops/s | -2.6% | Effectively flat to slightly lower. |

## 8. Allocation Discussion

Allocation was measured with the JMH GC profiler (`gc.alloc.rate.norm`). Successful magic validation dropped from 48 B/op to approximately 0 B/op in both the core and Rinha header benchmarks. Primitive scalar writer benchmarks dropped from 104 B/op to 24 B/op for single writes because the writer no longer carries the scratch `ByteBuffer` object in each small-message operation.

Mapped Rinha random access allocation dropped from 112 B/op to 48 B/op, and mapped Rinha sequential read allocation dropped from 1,120,006 B/op to 480,002 B/op. The remaining allocation is mainly the returned primitive arrays required by the current public API and benchmark shape.

This report does not claim zero allocation universally. Values reported as approximately 0 B/op are only the GC-profiler measurements for the named benchmarks.

## 9. Correctness and Safety

- `mvn -B clean verify` passed.
- `mvn -B javadoc:javadoc` passed.
- `mvn -B dependency:list -DincludeScope=compile` passed with no compile-scope dependencies.
- Magic mismatch errors still include expected and actual bytes.
- Bounds checks remain before reads and writes.
- Non-ASCII magic string behavior is preserved through US-ASCII replacement fallback.
- Existing malformed input, truncated input, endian, array, and mapped tests remain covered.

## 10. Public API Compatibility

The public API is unchanged. No public class, method, constructor, enum value, package, or exception type was added, removed, or renamed.

## 11. Dependency Status

- Production dependencies: none.
- Normal compile-scope dependencies: none.
- Test dependencies: JUnit 5 in test scope.
- Benchmark dependencies: JMH only in the opt-in `benchmarks` profile.

## 12. What Improved

- `writeInt` improved from 69.15M to 159.45M ops/s.
- `writeLong` improved from 63.41M to 143.39M ops/s.
- `writeDouble` improved from 62.47M to 142.34M ops/s.
- Mixed primitive writer records improved from 61.32M to 79.81M ops/s.
- Magic validation improved from 158.62M to 288.92M ops/s and from 48 B/op to approximately 0 B/op.
- Rinha header validation improved from 166.02M to 334.32M ops/s and from 48 B/op to approximately 0 B/op.
- Rinha mapped random access improved from 55.38M to 84.49M ops/s and from 112 B/op to 48 B/op.
- Rinha mapped sequential read improved from 6.00K to 13.99K ops/s and from 1,120,006 B/op to 480,002 B/op.

## 13. What Did Not Improve

- Rinha `BinaryReader` sequential read changed only from 14.01K to 14.25K ops/s, which is too small to claim a meaningful improvement.
- `int[]` array write was lower in the final run: 2.29M to 2.03M ops/s. The array write implementation was left on the original bulk `ByteBuffer` path after manual-loop attempts regressed benchmarks.
- `MappedBinaryReader.readLong(offset)` changed from 166.00M to 156.55M ops/s. This is a small negative movement in a noisy scalar benchmark.
- `BinaryReader` sequential scan changed from 49.81K to 48.54K ops/s, effectively flat to slightly lower.
- Heap/direct `ByteBuffer` baselines remain ahead in several read, array, and sequential scan benchmarks.

## 14. Reverted or Rejected Optimizations

- Manual primitive array write loops in `BinaryWriter` were tried and rejected because targeted JMH runs regressed array writes.
- Direct standalone `writeFloat` and `writeDouble` bodies were tried and rejected because they worsened the mixed writer record benchmark. The retained implementation routes through optimized `writeInt` and `writeLong`.
- Direct `ByteBuffer.getShort()` loops and backing-array byte decoding for `BinaryReader.readShortArray` were tried and rejected because they fixed some allocation symptoms but left Rinha sequential throughput around 7K ops/s. The retained single-view short path restored the Rinha `BinaryReader` sequential benchmark to 14.25K ops/s.
- No `Unsafe`, reflection, compression, schema engine, async IO, or public skip API was added.

## 15. Remaining Opportunities

High impact:

- Fix the shaded JMH jar execution path so `java -jar target/benchmarks.jar ...` is the canonical benchmark command.
- Add an internal or public no-allocation skip/range strategy only if the API decision is explicitly approved; Rinha label scanning is allocation-heavy because skipped bytes are currently read into arrays.

Medium impact:

- Investigate array write allocation from `toByteArray()` in small-message benchmarks without changing the public API.
- Add focused allocation benchmarks for `readShortArray(16)` and mapped typed arrays to separate returned-array allocation from internal buffer/view allocation.

Low impact:

- Re-run benchmarks with longer warmups/measurements and more forks on a real Git commit to reduce local-run noise.
- Benchmark Java 17 and Java 21 runtimes separately.

## 16. Final Recommendation

Keep the retained optimizations because they are simple, preserve behavior, and have clear benchmark evidence in targeted hot paths. Continue benchmarking before release: fix the benchmark jar execution issue, rerun on a release-candidate Git commit, and keep public performance claims limited to the measured benchmark scenarios.
