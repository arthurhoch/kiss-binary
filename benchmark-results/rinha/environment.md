# Rinha Benchmark Environment

Generated: 2026-05-05 17:07 -03

## Machine

- OS: macOS 26.4.1, Darwin 25.4.0
- Kernel: Darwin Arthurs-MacBook-Air.local 25.4.0 Darwin Kernel Version 25.4.0 arm64
- CPU: Apple M4, 10 cores (4 performance, 6 efficiency)
- Architecture: arm64 / aarch64

## Java and Maven

- Java: OpenJDK 21.0.11 LTS, Temurin-21.0.11+10
- Maven: Apache Maven 3.9.15
- Project target: Java 17 via `--release 17`

## Dataset

- RINHA_DATASET_DIR: /var/folders/.../rinha
- Dataset type: real (official Rinha de Backend 2026)
- references.json.gz size: 47.9 MB (gzipped), ~284 MB decompressed
- references.kbin size: 91.9 MB
- Vector count: 3,000,000
- Dimensions: 14 logical, 16 physical
- Fraud rate: 33.31%

## JVM Flags

No flags specified (default JVM settings).

## Command Used

```bash
RINHA_DATASET_DIR=/path/to/rinha/files mvn -B -P rinha-benchmark clean verify
```

## Date

2026-05-05T17:07:05-03:00
