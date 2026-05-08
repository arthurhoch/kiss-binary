# Baseline Optimization Summary

Raw JMH results: [jmh-results.json](jmh-results.json)

## Verification

- `mvn -B clean verify`: pass, 106 tests run, 0 failures, 0 errors, 6 skipped.
- `mvn -B javadoc:javadoc`: pass.
- `mvn -B dependency:list -DincludeScope=compile`: pass, resolved compile-scope dependencies: none.
- `mvn -B -Pbenchmarks clean package`: pass.
- Shaded benchmark jar execution: failed in forked benchmarks with `NoClassDefFoundError: InfraControl`; no numbers used from that failed run.
- Isolated classpath JMH run: pass, 89 primary benchmark results.

## Key Baseline Results

| Benchmark | Baseline |
|---|---:|
| `BinaryReader.readInt` | 376.19M ops/s, ~0 B/op |
| `ByteBuffer.getInt` heap baseline | 447.03M ops/s, ~0 B/op |
| `DataInputStream.readInt` | 188.39M ops/s, 56 B/op |
| `BinaryWriter.writeInt` small-message benchmark | 69.15M ops/s, 104 B/op |
| `BinaryWriter.writeIntArray(1024)` | 2.29M ops/s, 8,416 B/op |
| `ByteBuffer` heap `int[]` write baseline | 3.36M ops/s, 4,224 B/op |
| `BinaryReader.expectMagic("KB")` | 158.62M ops/s, 48 B/op |
| `ByteBuffer` heap magic baseline | 346.60M ops/s, ~0 B/op |
| `MappedBinaryReader.readLong(offset)` | 166.00M ops/s, ~0 B/op |
| Heap `ByteBuffer.getLong(offset)` | 182.27M ops/s, ~0 B/op |
| Sequential scan with `BinaryReader` | 49.81K ops/s, 112.68 B/op |
| Sequential scan with `MappedBinaryReader` | 79.39K ops/s, 0.42 B/op |
| Rinha synthetic sequential `BinaryReader` | 14.01K ops/s, 480,178 B/op |
| Rinha synthetic sequential heap `ByteBuffer` | 15.65K ops/s, 106 B/op |
| Rinha synthetic mapped random access | 55.38M ops/s, 112 B/op |
| Rinha synthetic heap random access | 148.82M ops/s, ~0 B/op |
| Rinha synthetic label scan with `BinaryReader` | 95.69K ops/s, 321,560 B/op |
| Rinha synthetic label scan with heap `ByteBuffer` | 16.37M ops/s, 0.002 B/op |

## Baseline Bottleneck Signals

- Magic validation allocates on success because `expectMagic(String)` creates an ASCII byte array and `validateMagic(byte[])` creates an `actual` byte array.
- `BinaryWriter` primitive writes route through a per-writer `ByteBuffer` scratch buffer; code inspection shows avoidable indirection in primitive hot paths.
- Primitive array writes allocate roughly twice the heap `ByteBuffer` baseline in the current benchmark because data is written into the writer's internal buffer and then copied by `toByteArray()`.
- Rinha-shaped mapped small-array reads allocate 112 B/op because each `readShortArray(offset, 16)` returns a new array and uses slice/view access internally.
- Rinha label scanning is dominated by benchmark/API usage that calls `readByteArray(labelOffset)` to skip data, allocating the skipped bytes. This is not optimized in this task because adding a public skip API would be a feature/API change.
