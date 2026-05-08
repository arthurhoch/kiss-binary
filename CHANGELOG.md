# Changelog

All notable changes to KissBinary will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Optimized measured hot paths without changing the public API:
  - `BinaryWriter` primitive scalar writes avoid the previous `ByteBuffer` scratch path.
  - Successful magic validation in `BinaryReader` and `MappedBinaryReader` avoids normal-path byte-array allocation.
  - `MappedBinaryReader` offset byte reads and small typed-array reads avoid duplicate/slice/view buffer objects.
- Added optimization benchmark evidence and report under `benchmark-results/optimization/`.
- Updated performance and release-readiness documentation with the 2026-05-05 optimization results.

### Added

- GitHub repository hygiene and security setup files:
  - `.gitignore` for build output, IDE files, logs, crash dumps, local env files, and local secret files.
  - GitHub Actions CI, CodeQL, dependency review, and manual Maven Central release workflows.
  - Dependabot configuration for Maven and GitHub Actions.
  - GitHub security setup report.
- Caller-provided primitive array read APIs for allocation-sensitive hot paths:
  - `BinaryReader` target-array overloads for `short[]`, `int[]`, `long[]`, `float[]`, and `double[]`.
  - `MappedBinaryReader` target-array overloads for `short[]`, `int[]`, `long[]`, `float[]`, and `double[]`.
  - `BinaryReader.skipBytes(long)` and `BinaryReader.skipFully(long)`.
- No-allocation read benchmarks and report under `benchmark-results/no-allocation-read/`.
- Rinha de Backend 2026 dataset benchmark suite for real-world performance validation.
  - Converts references.json.gz to compact binary format (references.kbin) using kiss-binary.
  - Validates header, dimensions, vectors, and labels using BinaryReader and MappedBinaryReader.
  - JMH benchmarks for sequential read, mapped read, random access, header validation, and label bitset scan.
  - Baseline comparison with raw ByteBuffer.
  - Synthetic dataset tests that run without the real dataset in normal `mvn verify`.
  - Full dataset tests conditional on `RINHA_DATASET_DIR` environment variable.
  - Maven profile `-P rinha-benchmark` for explicit dataset benchmark runs.
  - Documentation: `docs/rinha-dataset-benchmark.md`.
  - Benchmark report templates: `benchmark-results/rinha/`.
- `BinaryWriter` — write primitives and primitive arrays to an in-memory byte buffer with explicit endianness.
- `BinaryReader` — read primitives and primitive arrays from `byte[]` or `ByteBuffer` with bounds checking and safe EOF handling.
- `MappedBinaryReader` — read-only memory-mapped binary reader for random offset access.
- `Endianness` enum — `BIG_ENDIAN` (default) and `LITTLE_ENDIAN` support.
- `BinaryException` — base exception for library errors (invalid arguments, IO failures).
- `BinaryFormatException` — exception for malformed/truncated binary data with file offset and context.
- Magic/version validation helpers: `writeMagic`, `expectMagic`, `writeVersion`, `readVersion`, `expectVersion`.
- Primitive array read/write: `byte[]`, `short[]`, `int[]`, `long[]`, `float[]`, `double[]`, `char[]`.
- Output helpers: `toByteArray()` and `writeTo(OutputStream)`.
- Chunked array operations to avoid integer overflow on large arrays.
- JUnit 5 test suite (120 tests in the latest local verification, with 6 environment-dependent tests skipped): primitive roundtrip, endianness, arrays, headers, truncated files, invalid arguments, mmap, position tracking.
- JMH benchmark profile and benchmark source for primitive IO, arrays, headers, mmap random access, sequential scan, and fair JDK baselines.
- Local benchmark results under `benchmark-results/`.
- Release readiness report (`RELEASE_READINESS_REPORT.md`).
- Maven project with Java 17, zero production dependencies, source/javadoc JAR, release profile.
- Initial documentation and architecture planning for KissBinary.
- Product specification (`PRODUCT_SPEC.md`).
- Implementation plan (`IMPLEMENTATION_PLAN.md`).
- API design document (`API_DESIGN.md`).
- Performance goals and benchmark plan (`PERFORMANCE.md`).
- Error handling model (`ERROR_HANDLING.md`).
- Binary format design guide (`BINARY_FORMAT_GUIDE.md`).
- Conceptual examples (`EXAMPLES.md`).
- Roadmap (`ROADMAP.md`).
- Release checklist (`RELEASE.md`).
- Maven Central publishing guide (`MAVEN_CENTRAL.md`).
- Contribution guidelines (`CONTRIBUTING.md`).
- Security policy (`SECURITY.md`).
- AI agent instructions (`AGENTS.md`, `CAVEMAN.md`).
- Architecture documentation (`.github/architecture/`).
- User-facing documentation (`docs/`).
- Apache License 2.0 (`LICENSE.txt`).
