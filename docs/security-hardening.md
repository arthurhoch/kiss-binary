# Security Hardening

KissBinary uses free GitHub-native and open-source checks. Normal CI, CodeQL, Dependency Review, Dependabot, and OpenSSF Scorecard do not require repository secrets.

## Automated Checks

- CI runs `mvn -B clean verify`, generates Surefire and JaCoCo reports, and uploads report artifacts.
- CodeQL runs with Java 17, Maven cache, `security-and-quality` queries, and the analysis build `mvn -B -DskipTests -Djacoco.skip=true package`.
- Dependabot monitors Maven and GitHub Actions dependencies.
- Dependency Review checks pull request dependency changes and fails on moderate or higher vulnerability findings.
- OpenSSF Scorecard runs on schedule and manual dispatch, then uploads SARIF to code scanning.

## Local Commands

```bash
mvn -B clean verify
mvn -B test jacoco:report
mvn -B javadoc:javadoc
mvn -Pspotbugs verify
mvn -Pdependency-check verify
mvn -Pbenchmarks test-compile
mvn -Prinha-benchmark test-compile
```

The coverage reports are:

```text
target/site/jacoco/jacoco.xml
target/site/jacoco/index.html
```

## GitHub Repository Settings

Enable these settings in GitHub under **Settings > Code security and analysis**:

- Dependency graph;
- Dependabot alerts;
- Dependabot security updates;
- Code scanning.

No Codecov, Sonar, Snyk, or other paid-service token is required by the current setup. Coverage badges should be added only after a real external coverage service is configured.

## Cleanup And Release Gates

Before deleting code, follow [Safe Code Cleanup](code-cleanup.md). Treat public API, binary parsing, bounds checks, header validation, memory-mapped reads, and benchmark-referenced behavior as compatibility-sensitive.

Before release, run the normal build, Javadocs, coverage generation, compile-scope dependency check, and any relevant optional quality/security/benchmark profile. Release secrets must stay limited to the release workflow and must not be used by security workflows.
