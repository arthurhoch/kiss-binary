# 07 — Release Readiness

This document defines the readiness gates that must be satisfied before any version is released.

## Readiness Gates

All gates must pass before a release is tagged.

### 1. Documentation Complete

- [ ] `README.md` reflects the current API.
- [ ] `PRODUCT_SPEC.md` reflects current scope.
- [ ] `API_DESIGN.md` reflects current public API.
- [ ] `IMPLEMENTATION_PLAN.md` reflects current progress.
- [ ] `EXAMPLES.md` examples are verified against current API.
- [ ] `ERROR_HANDLING.md` reflects current error model.
- [ ] `BINARY_FORMAT_GUIDE.md` is current.
- [ ] `PERFORMANCE.md` reflects benchmark results (if benchmarks exist).
- [ ] `ROADMAP.md` is current.
- [ ] `CHANGELOG.md` is updated with all changes for this version.
- [ ] Architecture docs in `.github/architecture/` are current.
- [ ] User docs in `docs/` are current.

### 2. API Reviewed

- [ ] Public API matches `API_DESIGN.md`.
- [ ] No public methods without Javadoc.
- [ ] No public methods without tests.
- [ ] No internal classes exposed as public API.
- [ ] API is small enough to memorize.

### 3. Tests Passing

- [ ] All tests pass: `mvn -B verify`.
- [ ] All test categories covered (see `.github/architecture/06-testing-and-benchmarking.md`).
- [ ] No disabled or skipped tests without documented reason.
- [ ] No flaky tests.

### 4. Javadoc Generated

- [ ] Javadoc generates without errors: `mvn javadoc:javadoc`.
- [ ] All public classes have class-level Javadoc.
- [ ] All public methods have Javadoc.
- [ ] No internal classes appear in public Javadoc.

### 5. Benchmarks Captured

- [ ] JMH benchmarks run for the release version.
- [ ] Results documented with environment details.
- [ ] No performance regressions compared to previous release (if applicable).
- [ ] Baseline comparisons included.

### 6. No Compile Dependencies

- [ ] `mvn dependency:tree -Dscope=compile` shows zero dependencies.
- [ ] No transitive dependencies introduced.
- [ ] No test or build dependencies leaking into compile scope.

### 7. Maven Central Metadata Ready

- [ ] `groupId` is `io.github.arthurhoch`.
- [ ] `artifactId` is `kiss-binary`.
- [ ] Version is correct (no `-SNAPSHOT` for release).
- [ ] `pom.xml` has `name`, `description`, `url`, `licenses`, `developers`, `scm`.
- [ ] Source JAR plugin configured.
- [ ] Javadoc JAR plugin configured.
- [ ] GPG signing configured in `release` profile.
- [ ] Sonatype Central Portal plugin configured in `release` profile.

### 8. Release Version Set

- [ ] Version in `pom.xml` matches intended release version.
- [ ] `CHANGELOG.md` has a section for the release version with date.
- [ ] Tag matches `v<version>` pattern.

## Release Process

1. Complete all readiness gates.
2. Update `CHANGELOG.md` with release version and date.
3. Set version in `pom.xml` (remove `-SNAPSHOT`).
4. Commit changes.
5. Tag: `git tag v0.1.0`.
6. Push tag to GitHub.
7. GitHub Actions release workflow runs.
8. Verify artifact on Maven Central.
9. Bump `pom.xml` to next `-SNAPSHOT` version.

## Emergency Fixes

For patch releases (e.g., `v0.1.1`):

1. Fix the bug.
2. Add a test for the bug.
3. Update `CHANGELOG.md`.
4. Complete readiness gates (abbreviated: tests, docs, version).
5. Tag and release.
