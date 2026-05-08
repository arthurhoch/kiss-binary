# KissBinary Release Readiness Report

Date: 2026-05-05

## 1. Executive Summary

- Ready for v0.1.0: Not Ready.
- Ready for Maven Central: Not Ready.
- Performance proven: Scenario-specific JMH evidence exists, including the 2026-05-05 optimization pass; a broad "extremely high performance" claim is not proven.
- Main risks: no Git repository metadata in this workspace, no GitHub Actions release workflow, the shaded benchmark jar remains unreliable here, and local benchmark results show mixed performance against `ByteBuffer` baselines.

The implementation is small, uses the intended package `io.github.arthurhoch.kissbinary`, has zero normal compile-scope dependencies, and passes the current Maven test suite. Release should wait until benchmark execution is clean from the packaged jar, CI/release automation exists, and documentation claims are limited to the measured benchmark scenarios.

## 2. Commands Run

| Command | Result | Summary |
|---|---|---|
| `mvn -B clean verify` | Pass | 109 tests run, 0 failures, 0 errors, 6 skipped. Source and Javadoc jars generated. |
| `mvn -B javadoc:javadoc` | Pass | Public Javadocs generated successfully. |
| `mvn -B dependency:list -DincludeScope=compile` | Pass | Resolved compile-scope dependencies: none. |
| `mvn -B -Pbenchmarks clean package` | Pass | Benchmark profile compiled and built `target/benchmarks.jar`. |
| `java -jar target/benchmarks.jar -wi 5 -i 5 -f 2 -w 200ms -r 200ms -prof gc -rf json -rff benchmark-results/jmh-results.json` | Failed, not used for results | Sandbox run failed with `java.net.SocketException: Operation not permitted`; escalated run launched JMH but benchmark forks failed with `java.lang.Error: Unresolved compilation problem: The hierarchy of the type KissBinaryBenchmark_jmhType_B2 is inconsistent`. |
| `mvn -B -Pbenchmarks compile dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=target/benchmark-classpath.txt` | Pass | Built benchmark classes and runtime classpath for the isolated JMH run. |
| `mkdir -p benchmark-results/jmh-classes` | Pass | Created isolated benchmark class output directory. |
| `cp -R target/classes/. benchmark-results/jmh-classes/` | Pass | Copied Maven-compiled project and benchmark classes. |
| `find target/generated-sources/annotations -name '*.java' -print > target/jmh-generated-sources.txt` | Pass | Captured JMH-generated source list. |
| `javac --release 17 -cp "benchmark-results/jmh-classes:$(cat target/benchmark-classpath.txt)" -d benchmark-results/jmh-classes @target/jmh-generated-sources.txt` | Pass | Recompiled JMH-generated sources into the isolated output directory. |
| `java -cp "benchmark-results/jmh-classes:$(cat target/benchmark-classpath.txt)" org.openjdk.jmh.Main -wi 5 -i 5 -f 2 -w 200ms -r 200ms -prof gc -rf json -rff benchmark-results/jmh-results.json` | Pass | Earlier release-readiness JMH run completed successfully and wrote 80 primary benchmark results plus GC allocation metrics. |
| `java -cp "benchmark-results/optimization/after/jmh-classes:$(cat target/benchmark-classpath.txt)" org.openjdk.jmh.Main -wi 5 -i 5 -f 1 -w 200ms -r 200ms -prof gc -rf json -rff benchmark-results/optimization/after/jmh-results.json` | Pass | Optimization after-run completed successfully and wrote 89 primary benchmark results plus GC allocation metrics. |
| `git rev-parse HEAD` | Fail | Not a Git repository; commit hash unavailable. |

Environment commands also run: `sw_vers`, `uname -a`, `uname -m`, `sysctl -n machdep.cpu.brand_string`, `system_profiler SPHardwareDataType`, `java -version`, `mvn -version`, `pmset -g batt`, and `date`. `sysctl` was blocked by the sandbox; `system_profiler` provided CPU details. The sanitized environment is recorded in [benchmark-results/environment.md](benchmark-results/environment.md).

## 3. Documentation Review

Reviewed files:

- Top-level docs: `README.md`, `PRODUCT_SPEC.md`, `IMPLEMENTATION_PLAN.md`, `API_DESIGN.md`, `PERFORMANCE.md`, `ERROR_HANDLING.md`, `BINARY_FORMAT_GUIDE.md`, `EXAMPLES.md`, `ROADMAP.md`, `RELEASE.md`, `MAVEN_CENTRAL.md`, `CONTRIBUTING.md`, `SECURITY.md`, `CHANGELOG.md`, `AGENTS.md`, `CAVEMAN.md`.
- IDE/agent docs: `.github/copilot-instructions.md`.
- Architecture docs: every file under `.github/architecture/`.
- User docs: every file under `docs/`.

Inconsistencies found:

- `.github/architecture/06-testing-and-benchmarking.md` still said benchmarks had not been written and showed an outdated benchmark profile command.
- `API_DESIGN.md` omitted public convenience methods that exist in the implementation, including byte-range helpers, magic/version helpers, and `position()`.
- Benchmark markdown summaries contained stale numbers from a previous local run after the raw JSON was refreshed.
- Some user docs had stale release-review language, outdated test-count/performance wording, or unclear wording around unchecked IO wrapping and the 4-byte `writeVersion` helper.
- Release docs describe tag-triggered release automation, but there is no `.github/workflows` directory in this workspace.

Fixes made before the optimization pass:

- Updated `API_DESIGN.md` to list the implemented public helper methods.
- Updated `.github/architecture/06-testing-and-benchmarking.md` to reflect the current JMH profile and benchmark status.
- Updated user-facing docs and changelog language to match the then-current implementation status, unchecked IO wrapping, and measured benchmark evidence.
- Updated `PERFORMANCE.md`, [benchmark-results/JMH_RESULTS.md](benchmark-results/JMH_RESULTS.md), [benchmark-results/environment.md](benchmark-results/environment.md), and this report with the current benchmark run.

Optimization pass updates:

- Updated `BinaryWriter` scalar primitive writes, magic string writing, `BinaryReader` magic validation, and `MappedBinaryReader` offset read internals without changing the public API.
- Added magic fallback regression tests for writer, reader, and mapped reader paths.
- Added [benchmark-results/optimization/OPTIMIZATION_REPORT.md](benchmark-results/optimization/OPTIMIZATION_REPORT.md), baseline/after summaries, and raw JMH JSON results.
- Updated `PERFORMANCE.md`, `CHANGELOG.md`, and this report with the measured 2026-05-05 optimization results.

## 4. API Review

Public classes:

- `Endianness`: `BIG_ENDIAN`, `LITTLE_ENDIAN`.
- `BinaryException`: unchecked base exception.
- `BinaryFormatException`: unchecked malformed/truncated data exception with `offset()`.
- `BinaryWriter`: in-memory primitive/array writer.
- `BinaryReader`: cursor-based primitive/array reader from `byte[]` or `ByteBuffer`.
- `MappedBinaryReader`: read-only offset-based memory-mapped reader.

Public method summary:

- `BinaryWriter`: factories, primitive writes, primitive array writes, byte range writes, `writeMagic`, `writeVersion`, `toByteArray`, `writeTo`, `size`, `position`.
- `BinaryReader`: factories, primitive reads, primitive array reads, byte range reads, `readFully`, magic/version validation helpers, `readVersion`, `position`, `remaining`, `hasRemaining`.
- `MappedBinaryReader`: factories, offset primitive reads, offset array reads, offset byte range read, magic/version validation, `size`, `close`.

API concerns:

- `MappedBinaryReader` maps one `MappedByteBuffer` and rejects files larger than `Integer.MAX_VALUE`; this is acceptable for v0.1.0 if kept documented.
- `MappedBinaryReader.close()` closes the channel, but Java 17 has no standard explicit unmap API; Javadocs state this.
- `MappedBinaryReader` has `validateVersion(offset, expected)` but no `expectVersion` alias. This is a small consistency concern, not a blocker.

Backward compatibility concerns:

- No released artifact exists, so the current public API can still be adjusted before v0.1.0.
- No public API change was made in this review.

## 5. Test Coverage Review

Final test count in the latest local verification: 109 tests, 0 failures, 0 errors, 6 skipped.

Coverage by required category:

- Primitive roundtrip: covered for byte, boolean, short, int, long, float, double, char, and byte arrays.
- Endianness: little-endian layout, big-endian layout, and cross-endian mismatch behavior covered.
- Primitive arrays: short, int, long, float, double, and char arrays covered.
- Header validation: magic success/mismatch, version success/mismatch, truncated magic, and truncated version covered.
- Truncated/corrupt input: truncated short, int, long, float, double, byte arrays, and typed arrays covered.
- Invalid arguments: null paths, null arrays, negative lengths/counts, invalid offset/length, impossible array sizes, and mapped out-of-bounds reads covered.
- `MappedBinaryReader`: primitive offset reads, byte range reads, `size()`, magic/version validation, out-of-bounds behavior, and close/reopen behavior covered.
- Position behavior: `BinaryReader` and `BinaryWriter` positions covered; mapped offset reads are stateless and covered.

Tests added in the optimization pass:

- Magic string ASCII fallback preservation tests for `BinaryWriter`, `BinaryReader`, and `MappedBinaryReader`.

Residual test gaps:

- No CI workflow currently runs the test suite in GitHub Actions.
- Examples are reviewed manually; there is no automated example compilation check.
- Read-after-close behavior for `MappedBinaryReader` is not explicitly specified as public behavior.

## 6. Benchmark Review

Optimization pass results are recorded separately in [benchmark-results/optimization/OPTIMIZATION_REPORT.md](benchmark-results/optimization/OPTIMIZATION_REPORT.md). Key measured improvements include `BinaryWriter.writeInt` from 69.15M to 159.45M ops/s, `BinaryReader.expectMagic("KB")` from 158.62M to 288.92M ops/s, and Rinha synthetic mapped sequential read from 6.00K to 13.99K ops/s. The same optimization report also records unchanged or lower scenarios, including `int[]` array write and direct mapped scalar offset reads.

Benchmark structure:

- Maven profile: `benchmarks`.
- Source layout: `src/jmh/java`.
- Benchmark class: `io.github.arthurhoch.kissbinary.benchmarks.KissBinaryBenchmark`.
- Raw results: [benchmark-results/jmh-results.json](benchmark-results/jmh-results.json).
- Summary: [benchmark-results/JMH_RESULTS.md](benchmark-results/JMH_RESULTS.md).
- Environment: [benchmark-results/environment.md](benchmark-results/environment.md).

References checked:

- OpenJDK JMH project: <https://openjdk.org/projects/code-tools/jmh/>
- Sonatype Central Maven publishing docs: <https://central.sonatype.org/publish/publish-portal-maven/>

Benchmark scenarios:

- Primitive writes: int, long, double, mixed primitive record.
- Primitive reads: int, long, double, mixed primitive record.
- Array writes and reads: short, int, long, float, double arrays.
- Header validation: `expectMagic`, `expectVersion`.
- Mapped random offset reads: int, long, double.
- Sequential scan: fixed-size records with int, long, double.
- Baselines: `DataInputStream`, `DataOutputStream`, heap `ByteBuffer`, direct `ByteBuffer`.

Measured command:

```bash
java -cp "benchmark-results/jmh-classes:$(cat target/benchmark-classpath.txt)" org.openjdk.jmh.Main -wi 5 -i 5 -f 2 -w 200ms -r 200ms -prof gc -rf json -rff benchmark-results/jmh-results.json
```

Environment:

- macOS 26.4.1, Apple M4, arm64/aarch64.
- OpenJDK 21.0.11 LTS, Temurin.
- Maven 3.9.15.
- AC power, internal battery charged.
- Commit hash unavailable because this directory is not a Git repository.

Results summary:

- `BinaryReader.readInt`: 338.55M ops/s, GC allocation `<0.001 B/op`.
- `DataInputStream.readInt`: 158.76M ops/s, 56.000 B/op.
- Heap `ByteBuffer.getInt`: 396.66M ops/s, `<0.001 B/op`.
- `BinaryWriter.writeInt` small-message construction: 41.05M ops/s, 104.001 B/op.
- `DataOutputStream.writeInt` small-message construction: 58.39M ops/s, 72.001 B/op.
- Heap `ByteBuffer.putInt` small-message construction: 106.73M ops/s, 24.000 B/op.
- `MappedBinaryReader.readLong(offset)`: 113.68M ops/s, `<0.001 B/op`.
- Heap `ByteBuffer.getLong(offset)`: 115.67M ops/s, `<0.001 B/op`.
- Direct `ByteBuffer.getLong(offset)`: 155.75M ops/s, `<0.001 B/op`.
- Sequential scan: `BinaryReader` 45.57K ops/s, `MappedBinaryReader` 69.46K ops/s, heap `ByteBuffer` 83.98K ops/s, direct `ByteBuffer` 122.99K ops/s, `DataInputStream` 23.59K ops/s.

Baseline comparison:

- kiss-binary primitive reads were ahead of `DataInputStream` in this run.
- heap `ByteBuffer` was ahead of kiss-binary in several primitive read/write, header validation, random-offset, and sequential scan scenarios.
- `MappedBinaryReader` had low allocation in random offset and sequential scan scenarios, but direct `ByteBuffer` had higher random-offset and sequential scan throughput in this run.

Allocation discussion:

- JMH GC profiler reported `<0.001 B/op` for kiss-binary primitive reads and mapped random offset reads in this run.
- Array reads allocate destination arrays by API design.
- Write benchmarks allocate output buffers because each operation constructs a new small message; they should not be used to claim reusable-buffer behavior.
- Header magic validation allocates because this benchmark path compares against an actual byte array.

Limitations:

- One local machine and JVM only.
- JDK 21 runtime, although code compiles with `--release 17`.
- The standard shaded benchmark jar path is currently fragile in this workspace.
- No historical baseline exists because this is the first benchmark evidence captured in this repository.

## 7. Performance Conclusion

The repository now has credible local JMH evidence for the measured scenarios. That evidence does not prove a broad "extremely high performance" claim.

What can be claimed:

- In this benchmark run, kiss-binary primitive reads had higher throughput than `DataInputStream` for int, long, double, and mixed primitive records.
- In this benchmark run, kiss-binary primitive reads and mapped random offset reads reported `<0.001 B/op` allocation from JMH GC profiling.
- In this benchmark run, `MappedBinaryReader` sequential scan had lower allocation than the tested heap/direct `ByteBuffer` scan variants.

What cannot be claimed:

- The results do not support saying kiss-binary has extremely high performance overall.
- The results do not support saying kiss-binary generally has higher throughput than heap or direct `ByteBuffer`.
- The results do not support a universal allocation claim.
- The results do not prove release-level performance across machines, Java versions, and CI.

## 8. Security and Robustness Review

Malformed input:

- `BinaryReader` validates remaining bytes before primitive and array reads.
- `BinaryReader` rejects invalid boolean bytes.
- Magic/version mismatch errors include offset and expected vs actual values.

Truncated files:

- Tests cover truncated primitive reads, array reads, magic validation, and version validation.

Huge length/count handling:

- `BinaryReader.checkedByteCount` validates negative counts, remaining bytes, and integer overflow before allocating arrays.
- `MappedBinaryReader.checkedArrayBounds` validates count and range before allocating arrays.
- `MappedBinaryReader.checkBounds` uses a non-overflowing `bytes > fileSize - offset` check.

Mapped IO risks:

- Files larger than `Integer.MAX_VALUE` are rejected because the implementation maps a single region.
- Java 17 does not provide a standard explicit unmap API; the implementation documents this.
- Closing releases the file channel, not the mapped memory immediately.

Resource handling:

- `BinaryWriter.writeTo(OutputStream)` does not close caller-owned streams.
- `MappedBinaryReader` owns and closes its `FileChannel`.

## 9. Dependency Review

Compile/runtime dependency status:

- Normal compile-scope dependencies: none.
- Production dependencies: none.

Test/benchmark dependency status:

- Tests use JUnit 5 in test scope.
- Benchmarks use JMH only in the opt-in `benchmarks` profile.
- The benchmark profile shades JMH and its runtime dependencies into `target/benchmarks.jar`; these are not part of the normal library artifact.

## 10. Maven Central Readiness

Metadata:

- `groupId`: `io.github.arthurhoch`.
- `artifactId`: `kiss-binary`.
- Current version: `0.1.0-SNAPSHOT`.
- License, developer, SCM, name, description, and URL metadata exist in `pom.xml`.

Artifacts:

- Source jar is configured and generated by normal `mvn -B clean verify`.
- Javadoc jar is configured and generated by normal `mvn -B clean verify`.
- Standalone Javadocs pass with `mvn -B javadoc:javadoc`.

Release profile:

- GPG signing is configured in the `release` profile.
- `central-publishing-maven-plugin` is configured in the `release` profile.
- Official Sonatype docs confirm Central Portal Maven publishing uses `central-publishing-maven-plugin` and requires sources, Javadocs, GPG signatures, and required POM metadata.

Remaining manual steps:

- Restore/use a real Git repository with a clean working tree.
- Add GitHub Actions CI and release workflows.
- Verify Sonatype namespace ownership and credentials.
- Provide GPG signing secrets.
- Change version from `0.1.0-SNAPSHOT` to release version.
- Tag the release as `v0.1.0`.
- Run release from CI; do not publish from this review session.

## 11. Issues Found and Fixed

- Found stale benchmark documentation in `.github/architecture/06-testing-and-benchmarking.md`; updated it to the current `benchmarks` profile and status.
- Found incomplete public API listing in `API_DESIGN.md`; updated it to include implemented helper methods.
- Updated README/product/user docs/changelog wording to match the implementation, unchecked IO wrapping, and measured benchmark evidence.
- Found stale benchmark markdown numbers; refreshed [benchmark-results/JMH_RESULTS.md](benchmark-results/JMH_RESULTS.md) from the current raw JMH JSON.
- Updated [benchmark-results/environment.md](benchmark-results/environment.md) with the current run environment and exact measured command.
- Updated `PERFORMANCE.md` and this report to avoid unsupported performance claims.
- Ran an evidence-based optimization pass and recorded baseline/after results under [benchmark-results/optimization/](benchmark-results/optimization/).

Java source and test code changed only for the optimization pass. Public API was unchanged.

## 12. Remaining Work

1. Fix benchmark jar execution so `java -jar target/benchmarks.jar ...` works without the isolated classpath workaround.
2. Add GitHub Actions CI and release workflows.
3. Run benchmarks on a release candidate Git commit and save results with a real commit hash.
4. Run longer or multi-machine benchmark passes before making broader performance claims.
5. Add automated compilation checks for README and docs examples.
6. Specify `MappedBinaryReader` read-after-close behavior if it should be public behavior.
7. Complete Maven Central namespace, GPG, and repository secret setup.

## 13. Final Recommendation

Do not release v0.1.0 yet. The implementation and tests are in good shape for a small v0.1 API, and the retained optimization changes have scenario-specific benchmark evidence, but the repository is not release-ready until CI/release automation exists, benchmark jar execution is fixed, release-candidate benchmarks are captured from a real Git commit, and performance claims remain limited to the measured scenarios in `benchmark-results/`.
