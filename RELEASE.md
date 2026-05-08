# KissBinary — Release Guide

**Status: Initial implementation under release review.**

## Release Checklist

Before releasing any version:

### 1. Code and Tests

- [ ] All public methods have tests.
- [ ] All tests pass: `mvn -B verify`.
- [ ] No production dependencies added without approval.
- [ ] No Java 17 incompatibilities.
- [ ] No unused imports, no warnings in compilation.

### 2. Benchmarks

- [ ] JMH benchmarks captured for the release version.
- [ ] No performance regressions compared to previous release (if applicable).
- [ ] Benchmark environment documented (JVM, OS, hardware).

### 3. Documentation

- [ ] README.md reflects the current API.
- [ ] All examples in EXAMPLES.md are verified against current API.
- [ ] PRODUCT_SPEC.md reflects current scope.
- [ ] API_DESIGN.md reflects current public API.
- [ ] CHANGELOG.md updated with all changes under the release version.
- [ ] All architecture docs are current.

### 4. Javadoc

- [ ] All public classes have Javadoc.
- [ ] All public methods have Javadoc.
- [ ] Javadoc generates without errors: `mvn javadoc:javadoc`.
- [ ] No internal classes exposed in public Javadoc.

### 5. Maven Central Metadata

- [ ] `pom.xml` has correct `groupId`: `io.github.arthurhoch`.
- [ ] `pom.xml` has correct `artifactId`: `kiss-binary`.
- [ ] `pom.xml` has correct version (no `-SNAPSHOT` for release).
- [ ] `pom.xml` has `name`, `description`, `url`, `licenses`, `developers`, `scm`.
- [ ] Source JAR plugin configured.
- [ ] Javadoc JAR plugin configured.
- [ ] GPG signing plugin configured in `release` profile.
- [ ] Sonatype Central Portal plugin configured in `release` profile.

### 6. Version and Tag

- [ ] Version in `pom.xml` matches the intended release version.
- [ ] Tag created matching `v<version>` (e.g., `v0.1.0`).
- [ ] Tag pushed to GitHub.

### 7. Post-Release

- [ ] Verify artifact appears on Maven Central.
- [ ] Update `CHANGELOG.md` with release date.
- [ ] Bump version to next `-SNAPSHOT` in `pom.xml`.

## Versioning Rules

- Semantic versioning: MAJOR.MINOR.PATCH.
- `MAJOR`: incompatible API changes.
- `MINOR`: new functionality, backward-compatible.
- `PATCH`: bug fixes, backward-compatible.
- Pre-release versions use `-SNAPSHOT` suffix (e.g., `0.2.0-SNAPSHOT`).

## Tag Naming

Tags must match `v*` pattern:

- `v0.1.0`
- `v0.2.0`
- `v1.0.0`

No other tag formats trigger the release workflow.

## No Dependency Policy

The release must not include any production dependencies. Verify:

```bash
mvn dependency:tree -Dscope=compile
```

The output must show no dependencies other than the JDK.

## Release Workflow

The release process (once CI is configured):

1. Update `CHANGELOG.md` with the release version and date.
2. Update version in `pom.xml` (remove `-SNAPSHOT`).
3. Commit and tag: `git tag v0.1.0`.
4. Push the tag to GitHub.
5. GitHub Actions runs the release workflow:
   - Runs tests.
   - Generates source JAR and Javadoc JAR.
   - Signs artifacts with GPG.
   - Publishes to Sonatype Central Publisher Portal.
6. Verify on Maven Central.
7. Bump `pom.xml` to next `-SNAPSHOT` version.
