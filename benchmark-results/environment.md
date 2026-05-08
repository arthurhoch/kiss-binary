# Benchmark Environment

Generated: 2026-05-05 16:24:59 -03

## Machine

- OS: macOS 26.4.1, Darwin 25.4.0
- Kernel: Darwin Arthurs-MacBook-Air.local 25.4.0 Darwin Kernel Version 25.4.0: Thu Mar 19 19:31:09 PDT 2026; root:xnu-12377.101.15~1/RELEASE_ARM64_T8132 arm64
- CPU: Apple M4, 10 cores (4 performance, 6 efficiency)
- Architecture: arm64 / aarch64
- Power: AC Power, internal battery 100%, charged

## Java and Maven

- Java: OpenJDK 21.0.11 LTS, Temurin-21.0.11+10
- JVM: OpenJDK 64-Bit Server VM Temurin-21.0.11+10, mixed mode, sharing
- Maven: Apache Maven 3.9.15
- Maven Java runtime: /Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
- Project target: Java 17 via `--release 17`

## Repository

- Commit hash: unavailable; this directory is not currently inside a Git repository.
- Raw JMH JSON: `benchmark-results/jmh-results.json`
- Summary: `benchmark-results/JMH_RESULTS.md`

## Benchmark Command

The benchmark profile builds a shaded JMH jar:

```bash
mvn -B -Pbenchmarks clean package
```

In this workspace, JMH-generated benchmark classes under `target/classes` were inconsistent when the shaded jar executed. The measured run therefore used the Maven-compiled classes plus manually recompiled JMH generated sources on an isolated classpath:

```bash
mvn -B -Pbenchmarks compile dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=target/benchmark-classpath.txt
mkdir -p benchmark-results/jmh-classes
cp -R target/classes/. benchmark-results/jmh-classes/
find target/generated-sources/annotations -name '*.java' -print > target/jmh-generated-sources.txt
javac --release 17 -cp "benchmark-results/jmh-classes:$(cat target/benchmark-classpath.txt)" -d benchmark-results/jmh-classes @target/jmh-generated-sources.txt
java -cp "benchmark-results/jmh-classes:$(cat target/benchmark-classpath.txt)" org.openjdk.jmh.Main -wi 5 -i 5 -f 2 -w 200ms -r 200ms -prof gc -rf json -rff benchmark-results/jmh-results.json
```

## JMH Configuration

- Harness: JMH 1.37
- Mode: throughput
- Warmup: 5 iterations
- Measurement: 5 iterations
- Forks: 2
- Warmup time: 200 ms per iteration
- Measurement time: 200 ms per iteration
- Threads: 1
- Profiler: `gc`
- JVM flags: none supplied explicitly
