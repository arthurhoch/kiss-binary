---
layout: default
---

# Release

KissBinary releases are manual and must not be published from local machines outside the documented release process.

## Current Release

Latest stable release: `0.1.0`.

## Local Preflight

```bash
mvn -B clean verify
mvn -B javadoc:javadoc
mvn -B dependency:list -DincludeScope=compile
```

The compile-scope dependency list must remain empty.

## Benchmark Preflight

Long benchmark runs are opt-in. Use the configured benchmark profiles when validating performance claims:

```bash
mvn -Pbenchmarks test-compile
mvn -Prinha-benchmark test-compile
```

Do not claim benchmark results unless the raw output and environment are recorded.

## Release Workflow

The repository has a manual Maven Central release workflow. It verifies the requested release version, runs tests, generates Javadocs, checks compile-scope dependencies, imports signing material from GitHub secrets, and deploys through the Central Portal Maven plugin.

See the root [release guide](https://github.com/arthurhoch/kiss-binary/blob/main/RELEASE.md) and [Maven Central guide](maven-central.md).
