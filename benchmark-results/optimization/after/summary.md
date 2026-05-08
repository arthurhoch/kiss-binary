# After Optimization Summary

Raw JMH results: [jmh-results.json](jmh-results.json)

## Verification

- `mvn -B clean verify`: pass, 109 tests run, 0 failures, 0 errors, 6 skipped.
- `mvn -B javadoc:javadoc`: pass.
- `mvn -B dependency:list -DincludeScope=compile`: pass, resolved compile-scope dependencies: none.
- `mvn -B -Pbenchmarks clean package`: pass.
- Shaded benchmark jar execution: unreliable in this workspace; no comparison numbers were taken from that run.
- Isolated classpath JMH run: pass, 89 primary benchmark results.

## Key After Results

| Benchmark | After |
|---|---:|
| `BinaryReader.readInt` | 354.35M ops/s, ~0 B/op |
| `ByteBuffer.getInt` heap baseline | 413.86M ops/s, ~0 B/op |
| `DataInputStream.readInt` | 177.14M ops/s, 56 B/op |
| `BinaryWriter.writeInt` small-message benchmark | 159.45M ops/s, 24 B/op |
| `BinaryWriter.writeLong` small-message benchmark | 143.39M ops/s, 24 B/op |
| `BinaryWriter.writeDouble` small-message benchmark | 142.34M ops/s, 24 B/op |
| `BinaryWriter` mixed record | 79.81M ops/s, 120 B/op |
| `BinaryWriter.writeIntArray(1024)` | 2.03M ops/s, 8,416 B/op |
| `ByteBuffer` heap `int[]` write baseline | 3.17M ops/s, 4,224 B/op |
| `BinaryReader.expectMagic("KB")` | 288.92M ops/s, ~0 B/op |
| `ByteBuffer` heap magic baseline | 332.86M ops/s, ~0 B/op |
| `MappedBinaryReader.readLong(offset)` | 156.55M ops/s, ~0 B/op |
| Heap `ByteBuffer.getLong(offset)` | 178.06M ops/s, ~0 B/op |
| Sequential scan with `BinaryReader` | 48.54K ops/s, 112.70 B/op |
| Sequential scan with `MappedBinaryReader` | 77.18K ops/s, 0.45 B/op |
| Rinha synthetic sequential `BinaryReader` | 14.25K ops/s, 480,178 B/op |
| Rinha synthetic mapped random access | 84.49M ops/s, 48 B/op |
| Rinha synthetic mapped sequential read | 13.99K ops/s, 480,002 B/op |
| Rinha synthetic label scan with `BinaryReader` | 98.93K ops/s, 321,560 B/op |

## Before/After Highlights

| Benchmark | Before | After | Delta | Allocation before | Allocation after |
|---|---:|---:|---:|---:|---:|
| Magic validation | 158.62M ops/s | 288.92M ops/s | +82.1% | 48 B/op | ~0 B/op |
| Rinha header validation | 166.02M ops/s | 334.32M ops/s | +101.4% | 48 B/op | ~0 B/op |
| `writeInt` | 69.15M ops/s | 159.45M ops/s | +130.6% | 104 B/op | 24 B/op |
| `writeLong` | 63.41M ops/s | 143.39M ops/s | +126.1% | 104 B/op | 24 B/op |
| `writeDouble` | 62.47M ops/s | 142.34M ops/s | +127.9% | 104 B/op | 24 B/op |
| Mixed writer record | 61.32M ops/s | 79.81M ops/s | +30.2% | 144 B/op | 120 B/op |
| Rinha mapped random access | 55.38M ops/s | 84.49M ops/s | +52.6% | 112 B/op | 48 B/op |
| Rinha mapped sequential read | 6.00K ops/s | 13.99K ops/s | +133.2% | 1,120,006 B/op | 480,002 B/op |
| Rinha `BinaryReader` sequential read | 14.01K ops/s | 14.25K ops/s | +1.7% | 480,178 B/op | 480,178 B/op |
| `int[]` array write | 2.29M ops/s | 2.03M ops/s | -11.4% | 8,416 B/op | 8,416 B/op |
| `MappedBinaryReader.readLong(offset)` | 166.00M ops/s | 156.55M ops/s | -5.7% | ~0 B/op | ~0 B/op |
| `BinaryReader` sequential scan | 49.81K ops/s | 48.54K ops/s | -2.6% | 112.68 B/op | 112.70 B/op |

## Summary

The retained changes measurably improved successful magic validation, primitive scalar writes, and synthetic Rinha mapped small-array access. The results are still scenario-specific: `int[]` array write was lower in the final run even though the array implementation was left unchanged, direct `MappedBinaryReader.readLong(offset)` was 5.7% lower, and `BinaryReader` sequential scan was effectively flat to slightly lower.

The optimization report at [../OPTIMIZATION_REPORT.md](../OPTIMIZATION_REPORT.md) contains the baseline comparison and final recommendation.
