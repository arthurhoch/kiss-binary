# Testing Report

Date: 2026-05-08

## What Was Tested

- `BinaryWriter` and `BinaryReader` primitive reads and writes, strings, magic bytes, versions, primitive arrays, bounds checks, and round trips.
- Endianness behavior and explicit byte-order handling.
- `MappedBinaryReader` behavior for memory-mapped file reads.
- Rinha-shaped synthetic dataset reading and writing.
- Optional full Rinha dataset tests were discovered but skipped because `RINHA_DATASET_DIR` was not set.
- Project sanity checks for Java 17 compilation, jar/source/javadoc artifacts, Javadocs, zero compile-scope production dependencies, and benchmark profile wiring.

## Commands Run

```bash
mvn -B clean verify
mvn -B javadoc:javadoc
mvn -B dependency:list -DincludeScope=compile
mvn -B -Pbenchmarks test-compile
mvn -B -Prinha-benchmark test-compile
```

Results:

- `mvn -B clean verify`: passing, 120 tests, 0 failures, 0 errors, 6 skipped full-dataset tests.
- `mvn -B javadoc:javadoc`: passing.
- `mvn -B dependency:list -DincludeScope=compile`: passing, no compile-scope production dependencies beyond the project artifact.
- `mvn -B -Pbenchmarks test-compile`: passing.
- `mvn -B -Prinha-benchmark test-compile`: passing.

## Known Limits

- Full Rinha dataset validation requires setting `RINHA_DATASET_DIR` to a local dataset path.
- Benchmark profiles were compile-checked only; long JMH benchmark runs were not executed during this pass.
- The report records automated verification only and does not replace the release workflow.

## Future Tests Recommended

- Run full Rinha dataset verification with a pinned dataset revision.
- Record fresh JMH results with hardware, JDK, warmup, and dataset details.
- Add more corrupted-header and truncated-file fixtures.
