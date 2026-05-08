# Rinha Binary Benchmark Report

Generated: 2026-05-05

## 1. Executive Summary

KissBinary successfully converted the official Rinha de Backend 2026 reference dataset (3,000,000 labeled vectors) into a compact binary file using only kiss-binary APIs. The conversion, validation, sequential read, and memory-mapped read all completed without errors.

## 2. Dataset Used

- Source: official Rinha de Backend 2026 (`zanfranceschi/rinha-de-backend-2026`)
- File: `resources/references.json.gz`
- Format: gzip-compressed JSON array of `{ "vector": [...14 floats...], "label": "fraud"|"legit" }`
- Vector count: 3,000,000
- Dimensions: 14 logical, padded to 16 physical (int16)
- Fraud rate: 33.31% (999,406 fraud / 3,000,000 total)
- Dataset type: real

## 3. Binary Format Layout

| Offset | Size | Field |
|--------|------|-------|
| 0 | 4 | magic "KBRN" |
| 4 | 4 | version (1) |
| 8 | 4 | logical_dimensions (14) |
| 12 | 4 | physical_dimensions (16) |
| 16 | 4 | vector_count |
| 20 | 4 | label_word_count |
| 24 | 4 | reserved_1 (0) |
| 28 | 4 | reserved_2 (0) |
| 32 | N*32 | vectors (short[16] each, 14 data + 2 zero-pad) |
| 32+N*32 | W*8 | labels (long[] bitset, bit 1 = fraud) |

Endianness: little-endian. Quantization: int16 with scale 10,000.

## 4. Conversion Result

| Metric | Value |
|--------|-------|
| Input file (gzip) | 47.9 MB |
| Output file (.kbin) | 91.9 MB |
| Vector count | 3,000,000 |
| Bytes per vector | 32 |
| Conversion time | 3.587 s |
| Write throughput | 25.6 MB/s |

The conversion streams the gzipped JSON file, parses each record, quantizes to int16, and writes the binary file using `BinaryWriter`.

## 5. File Size Result

| Format | Size |
|--------|------|
| references.json.gz (compressed) | 47.9 MB |
| references.json (decompressed) | ~284 MB |
| references.kbin (compact binary) | 91.9 MB |

The compact binary file is 3.1x smaller than the decompressed JSON and 1.9x larger than the gzipped JSON. The binary file supports random access, memory mapping, and direct short array reads — none of which the JSON format supports.

## 6. Sequential Read Result

Sequential scan with `BinaryReader`: completed successfully across all 3,000,000 vectors. Checksum verified: 121088029481.

## 7. Mapped Read Result

Sequential scan with `MappedBinaryReader`: completed successfully. Checksum matches heap-based read exactly.

## 8. Random Access Result

Random access reads verified for multiple vector indices (0, 1, 42, 499, 500, 999) using `MappedBinaryReader`. All matched the heap-based reads exactly.

## 9. Label Bitset Result

| Metric | Value |
|--------|-------|
| Total vectors | 3,000,000 |
| Fraud count | 999,406 |
| Legit count | 2,000,594 |
| Fraud rate | 33.31% |

Label bitset stored as 46,875 longs (375,000 bytes). Popcount used for fraud counting.

## 10. Baseline Comparison

JMH benchmarks available under `src/jmh/java/.../benchmarks/rinha/`. Run with:

```bash
RINHA_DATASET_DIR=/path/to/rinha/files mvn -B -P rinha-benchmark,benchmarks clean package
java -jar target/benchmarks.jar '.*RinhaBinaryBenchmark' -rf json -rff benchmark-results/rinha/jmh-rinha-results.json
```

The macro-benchmark (full dataset conversion + validation) measured:
- Conversion: 25.6 MB/s write throughput using `BinaryWriter`
- Full sequential read of 3M vectors completed in the test phase

## 11. Memory Behavior

- The conversion uses streaming JSON parsing (one record at a time in memory)
- The label bitset accumulates in memory: 46,875 longs = ~375 KB
- The `BinaryWriter` internal buffer grows to accommodate the vector data (~96 MB)
- `MappedBinaryReader` provides zero-copy read access after mmap
- Total heap usage during conversion is dominated by the writer's internal buffer

## 12. What This Proves About kiss-binary

1. KissBinary can write a 91.9 MB compact binary file from a real-world dataset.
2. KissBinary can validate the file header (magic, version, dimensions, counts, file size).
3. KissBinary can sequentially read all 3,000,000 vectors using `BinaryReader`.
4. KissBinary can sequentially read all vectors using `MappedBinaryReader` with matching results.
5. KissBinary can randomly access individual vectors by offset using `MappedBinaryReader`.
6. KissBinary can read and decode a label bitset stored as a long array.
7. The conversion throughput was 25.6 MB/s on this machine (including JSON parsing overhead).
8. Zero external dependencies were used — only JDK standard APIs.

## 13. What This Does Not Prove

- This does not prove kiss-binary is the fastest binary IO library.
- This does not prove zero allocation on all code paths (BinaryWriter allocates an internal buffer).
- This does not prove production readiness for the Rinha fraud detection engine.
- This does not prove correctness of the fraud detection scoring — only binary IO roundtrip.
- This is one machine/JVM run, not a cross-platform result.
- JMH micro-benchmarks for the Rinha-shaped data have not been executed yet.

## 14. Remaining Work Before Rinha Runtime

- Implement the full fraud detection scoring engine (not part of kiss-binary).
- Integrate with kiss-server for the HTTP API.
- Implement the transaction processing pipeline.
- Implement normalization using normalization.json.
- Implement MCC risk lookup using mcc_risk.json.
- Run JMH micro-benchmarks on the Rinha-shaped data.

## 15. Exact Commands to Reproduce

```bash
# Download dataset
mkdir -p /path/to/rinha-dataset
cd /path/to/rinha-dataset
curl -LO https://raw.githubusercontent.com/zanfranceschi/rinha-de-backend-2026/main/resources/references.json.gz
curl -LO https://raw.githubusercontent.com/zanfranceschi/rinha-de-backend-2026/main/resources/mcc_risk.json
curl -LO https://raw.githubusercontent.com/zanfranceschi/rinha-de-backend-2026/main/resources/normalization.json

# Synthetic tests (always work)
mvn -B clean verify

# Full dataset tests
RINHA_DATASET_DIR=/path/to/rinha-dataset mvn -B -P rinha-benchmark clean verify

# JMH benchmarks
RINHA_DATASET_DIR=/path/to/rinha-dataset mvn -B -P rinha-benchmark,benchmarks clean package
java -jar target/benchmarks.jar '.*RinhaBinaryBenchmark' -rf json -rff benchmark-results/rinha/jmh-rinha-results.json
```
