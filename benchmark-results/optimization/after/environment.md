# After Optimization Environment

Generated: 2026-05-05T19:32:17-0300

## Machine

- OS: macOS 26.4.1
- Kernel: Darwin Arthurs-MacBook-Air.local 25.4.0 Darwin Kernel Version 25.4.0: Thu Mar 19 19:31:09 PDT 2026; root:xnu-12377.101.15~1/RELEASE_ARM64_T8132 arm64
- CPU: Apple M4
- Architecture: arm64 / aarch64

## Java and Maven

- Java: OpenJDK 21.0.11 LTS, Temurin-21.0.11+10
- Maven: Apache Maven 3.9.15
- Project target: Java 17 via `--release 17`
- JVM flags: none supplied explicitly

## Commands

```bash
mvn -B clean verify
mvn -B javadoc:javadoc
mvn -B dependency:list -DincludeScope=compile
mvn -B -Pbenchmarks clean package
java -jar target/benchmarks.jar -wi 5 -i 5 -f 2 -w 200ms -r 200ms -prof gc -rf json -rff benchmark-results/optimization/after/jmh-results-shaded-jar-attempt.json
mvn -B -Pbenchmarks dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=target/benchmark-classpath.txt
rm -rf benchmark-results/optimization/after/jmh-classes
mkdir -p benchmark-results/optimization/after/jmh-classes
cp -R target/classes/. benchmark-results/optimization/after/jmh-classes/
find target/generated-sources/annotations -name '*.java' -print > target/jmh-generated-sources.txt
javac --release 17 -cp "benchmark-results/optimization/after/jmh-classes:$(cat target/benchmark-classpath.txt)" -d benchmark-results/optimization/after/jmh-classes @target/jmh-generated-sources.txt
java -cp "benchmark-results/optimization/after/jmh-classes:$(cat target/benchmark-classpath.txt)" org.openjdk.jmh.Main -wi 5 -i 5 -f 1 -w 200ms -r 200ms -prof gc -rf json -rff benchmark-results/optimization/after/jmh-results.json
```

The shaded benchmark jar built, but benchmark-jar execution was unreliable in this workspace during the optimization pass. The measured after results therefore used the same isolated classpath method as the baseline.

Exploratory targeted JMH files in this directory were used to reject or confirm candidate changes:

- `jmh-primitive-write-local-size.json`
- `jmh-rinha-sequential-small-array.json`
- `jmh-rinha-sequential-backing-array.json`
- `jmh-rinha-sequential-short-view.json`

Those exploratory runs are not used as the final before/after comparison.

## Rinha Dataset

Full Rinha dataset optimization was not measured because RINHA_DATASET_DIR was not provided or required files were missing.

Synthetic Rinha-shaped JMH benchmarks were included in the after JMH run.
