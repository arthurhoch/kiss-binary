---
layout: default
---

# Release Checklist

**Status: Initial implementation complete; not yet released.**

This is the user-facing release checklist. For the full release guide, see [RELEASE.md](../RELEASE.md).

## Before Release

- [ ] All tests pass: `mvn -B verify`.
- [ ] No production dependencies: `mvn dependency:tree -Dscope=compile`.
- [ ] Javadoc generates without errors: `mvn javadoc:javadoc`.
- [ ] Benchmarks captured (if applicable).
- [ ] Documentation reviewed and current.
- [ ] `CHANGELOG.md` updated with release version and date.

## Version and Tag

- [ ] Version in `pom.xml` is correct (no `-SNAPSHOT`).
- [ ] Version follows semantic versioning.
- [ ] Tag matches `v<version>` (e.g., `v0.1.0`).

## Publish

- [ ] Tag pushed to GitHub.
- [ ] GitHub Actions release workflow succeeds.
- [ ] Artifact verified on Maven Central.

## Post-Release

- [ ] `pom.xml` bumped to next `-SNAPSHOT` version.
- [ ] `CHANGELOG.md` updated with release date.

## See Also

- [RELEASE.md](../RELEASE.md) — full release guide.
- [MAVEN_CENTRAL.md](../MAVEN_CENTRAL.md) — Maven Central publishing guide.
