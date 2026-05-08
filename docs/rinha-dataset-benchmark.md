---
layout: default
---

# Rinha Dataset Benchmark

This document explains the Rinha de Backend 2026 dataset benchmark for kiss-binary.

## Why This Exists

The Rinha de Backend is a Brazilian backend performance challenge. The 2026 edition provides a labeled reference dataset with 3,000,000 vectors (14 dimensions each) used for fraud detection scoring.

This dataset is useful as a real-world benchmark for kiss-binary because:

- It is a realistic large binary dataset (millions of records)
- It tests kiss-binary's ability to write, read, validate, and memory-map a compact binary file
- It provides a concrete data shape for measuring sequential and random access throughput
- It proves kiss-binary handles files in the ~100 MB range

**This is a kiss-binary benchmark. It is not the Rinha fraud engine.** The fraud detection logic, HTTP API, and runtime are separate concerns.

## Dataset Files

The benchmark requires official Rinha files placed in a local directory:

- `references.json.gz` — 3,000,000 labeled reference vectors (14 dimensions, gzip-compressed JSON)
- `mcc_risk.json` — MCC risk table (not used in this benchmark)
- `normalization.json` — normalization parameters (not used in this benchmark)

Only `references.json.gz` is required for the binary conversion benchmark.

## How to Get the Dataset

The official Rinha de Backend 2026 dataset is in the GitHub repository at:
`https://github.com/zanfranceschi/rinha-de-backend-2026/tree/main/resources`

Download the three files and place them in a directory:

```bash
mkdir -p /path/to/rinha-dataset
cd /path/to/rinha-dataset
curl -LO https://raw.githubusercontent.com/zanfranceschi/rinha-de-backend-2026/main/resources/references.json.gz
curl -LO https://raw.githubusercontent.com/zanfranceschi/rinha-de-backend-2026/main/resources/mcc_risk.json
curl -LO https://raw.githubusercontent.com/zanfranceschi/rinha-de-backend-2026/main/resources/normalization.json
```

Then set `RINHA_DATASET_DIR` to that directory. Only `references.json.gz` is required for the binary benchmark.

The dataset contains:
- `references.json.gz` — ~48 MB gzipped, ~284 MB decompressed, 3,000,000 labeled vectors
- `mcc_risk.json` — MCC risk scores (<1 KB)
- `normalization.json` — normalization constants (<1 KB)

Each vector record in `references.json.gz` has the format:
```json
{ "vector": [0.01, 0.0833, 0.05, 0.8261, 0.1667, -1, -1, 0.0432, 0.25, 0, 1, 0, 0.2, 0.0416], "label": "legit" }
```

- 14 dimensions, already normalized (values in -1.0 to 1.0 range)
- Label is `"fraud"` or `"legit"` (string)
- Index 5 and 6 may be `-1` (sentinel for no previous transaction)

## Binary Format (.kbin)

The conversion produces a compact binary file with this layout:

```
Offset  Size                    Field
------  ----                    -----
0       4                       magic "KBRN"
4       4                       version (1)
8       4                       logical_dimensions (14)
12      4                       physical_dimensions (16, padded for alignment)
16      4                       vector_count
20      4                       label_word_count (ceil(vector_count / 64))
24      4                       reserved_1 (0)
28      4                       reserved_2 (0)
32      vector_count * 32       vectors: short[16] per vector (14 data + 2 zero)
32+vc*32  label_word_count * 8  labels: long[] bitset (bit 1 = fraud)
```

- Endianness: little-endian
- Vectors are stored as int16 (short) with scale 10,000 from original floating-point values
- Labels are stored as a bitset: bit = 1 means fraud, bit = 0 means legit

For 3,000,000 vectors: ~96.4 MB total (91.6 MB vectors + 0.4 MB labels + 32 B header).

## Running the Tests

### Synthetic tests (always run, no dataset required)

```bash
mvn -B clean verify
```

This runs `RinhaSyntheticDatasetTest` with 1,000 synthetic vectors. It validates:
- Header magic, version, dimensions
- File size matches layout
- First/last vector readable
- First/last label readable
- Mapped reader matches heap reader
- Truncated file detection
- Magic/version mismatch detection
- Quantization (clamp, NaN, Infinity)

### Full dataset tests (requires RINHA_DATASET_DIR)

```bash
RINHA_DATASET_DIR=/path/to/rinha/files mvn -B -P rinha-benchmark clean verify
```

This additionally runs `RinhaFullDatasetTest` with the real 3M vector dataset. It:
- Converts references.json.gz to references.kbin
- Validates header, dimensions, counts
- Reads first/last vector and label
- Runs sequential and mapped sequential scans
- Reports conversion metrics

### JMH benchmarks

```bash
RINHA_DATASET_DIR=/path/to/rinha/files mvn -B -P rinha-benchmark,benchmarks clean package
java -jar target/benchmarks.jar '.*RinhaBinaryBenchmark' -rf json -rff benchmark-results/rinha/jmh-rinha-results.json
```

JMH benchmarks measure:
1. Sequential read with BinaryReader
2. Sequential read with MappedBinaryReader
3. Sequential read with heap ByteBuffer (baseline)
4. Random vector access with MappedBinaryReader
5. Random vector access with heap ByteBuffer (baseline)
6. Header validation cost (kiss-binary vs ByteBuffer)
7. Label bitset scan (kiss-binary vs ByteBuffer)

## Generated Files

- `target/rinha/references.kbin` — generated compact binary file
- `benchmark-results/rinha/jmh-rinha-results.json` — JMH results (if JMH was run)
- `benchmark-results/rinha/RINHA_BINARY_BENCHMARK_REPORT.md` — benchmark report
- `benchmark-results/rinha/environment.md` — environment details

## Reading the Benchmark Report

The benchmark report follows a specific structure. Key sections:

- **Conversion Result**: input/output sizes, vector count, throughput
- **Sequential Read Result**: vectors/sec and MB/s for BinaryReader and MappedBinaryReader
- **Random Access Result**: ns/op for mapped random vector reads
- **Baseline Comparison**: kiss-binary vs raw ByteBuffer
- **What This Proves**: specific measured facts about kiss-binary
- **What This Does Not Prove**: explicit limitations of the benchmark

## What This Is Not

- Not the Rinha fraud detection engine
- Not a kiss-server integration
- Not production business logic
- Not a public API format guarantee
- The .kbin format is a benchmark artifact, not a stable format

## See Also

- [PERFORMANCE.md](../PERFORMANCE.md)
- [BINARY_FORMAT_GUIDE.md](../BINARY_FORMAT_GUIDE.md)
- [benchmark-results/rinha/RINHA_BINARY_BENCHMARK_REPORT.md](../benchmark-results/rinha/RINHA_BINARY_BENCHMARK_REPORT.md)
