# Safe Code Cleanup

KissBinary is a Java 17 binary IO library with a small public API and performance-sensitive internals. Cleanup must preserve binary safety, explicit layout behavior, benchmark evidence, and release compatibility.

## Public API First

Treat these as public API unless a maintainer explicitly changes the contract:

- public types and methods under `io.github.arthurhoch.kissbinary`;
- documented reader, writer, mapped-reader, endianness, header validation, and exception APIs;
- examples in `README.md`, `docs/`, root design docs, benchmark reports, and GitHub Pages.

Do not remove public API directly after a Maven Central release without considering a deprecation cycle. Prefer deprecation first, update documentation, and remove only in a planned compatible release window.

## Reference Search

Before deleting anything, search references in all consumer-facing and maintainer-facing surfaces:

```bash
rg "SymbolName|methodName" src README.md docs benchmark-results .github *.md
```

Include source, tests, README, docs, examples, architecture notes, GitHub Pages content, benchmark code, benchmark reports, and release documentation. A code element with low coverage is not automatically unused.

## Required Checks

Run the normal verification and inspect coverage:

```bash
mvn -B clean verify
mvn -B test jacoco:report
mvn -B javadoc:javadoc
mvn -B dependency:list -DincludeScope=compile
```

The compile-scope dependency list must remain `none`.

Coverage reports are generated at:

```text
target/site/jacoco/jacoco.xml
target/site/jacoco/index.html
```

Open `target/site/jacoco/index.html` for human review. Use the XML report for Codecov or Sonar if those services are configured later.

## Advanced Profiles

Use optional profiles as evidence, not as automatic deletion authority:

```bash
mvn -Pspotbugs verify
mvn -Pdependency-check verify
mvn -Pbenchmarks test-compile
```

`spotbugs` generates SpotBugs reports without making normal CI depend on static-analysis findings. `dependency-check` runs OWASP Dependency-Check and may download vulnerability data. `benchmarks` compiles JMH benchmark code but does not run a long benchmark by itself.

`mvn -Prinha-benchmark test-compile` also exists, but full Rinha dataset validation requires `RINHA_DATASET_DIR` and is not required for ordinary cleanup.

No PIT mutation-testing or API-compatibility profile is configured yet. If public API removal is planned, add a japicmp or Revapi baseline against the previous Maven Central release first and keep the initial check non-failing until the baseline is reviewed.

## Cleanup Policy

- Distinguish internal implementation from public API before deletion.
- Never delete public API directly after release without deprecation review.
- Use OpenRewrite, IDE inspections, and static analyzers only as suggestions.
- Verify Javadocs after cleanup so public docs still build.
- Run API compatibility checks before public API removal once a baseline profile exists.
- Run PIT, focused mutation tests, or benchmark comparisons for critical binary parsing, bounds, header, and mapped-read behavior if deletion touches risky logic.
- Document the removal in `CHANGELOG.md` before release.

## Before Release

Before releasing a cleanup change, confirm:

- `mvn -B clean verify` passes.
- `mvn -B javadoc:javadoc` passes.
- `mvn -B dependency:list -DincludeScope=compile` still reports no compile-scope production dependencies.
- JaCoCo XML and HTML reports are generated.
- Optional quality/security/benchmark profiles were run or intentionally skipped with a documented reason.
- Public API removals went through deprecation or compatibility review.
- README, docs, examples, benchmark references, and GitHub Pages references are updated.
