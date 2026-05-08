# kiss-binary 0.1.0 Release Report

Date: 2026-05-08

## 1. Release Summary

- Released artifact: `io.github.arthurhoch:kiss-binary:0.1.0`
- Git tag: `v0.1.0`
- GitHub repository: <https://github.com/arthurhoch/kiss-binary>
- GitHub release: <https://github.com/arthurhoch/kiss-binary/releases/tag/v0.1.0>
- Publishing path: manual GitHub Actions workflow `.github/workflows/release.yml`
- Next development version: next `-SNAPSHOT`

## 2. Commands Run

Initial checks:

```bash
pwd
git status --short
git branch --show-current
git remote -v
gh auth status
java -version
mvn -v
git fetch origin
git pull --ff-only origin main
gh secret list --repo arthurhoch/kiss-binary
rg --hidden -n -i '(password=|secret=|token=|ghp_|github_pat_|GPG_PRIVATE_KEY|MAVEN_CENTRAL_PASSWORD|SONATYPE|CENTRAL_TOKEN)' -g '!target/**' -g '!benchmark-results/jmh-classes/**' -g '!.git/**' .
```

Local verification before release preparation:

```bash
mvn -B clean verify
mvn -B javadoc:javadoc
mvn -B dependency:list -DincludeScope=compile
mvn -B -Pbenchmarks clean package
java -jar target/benchmarks.jar -l
```

Release preparation and verification:

```bash
mvn -q -DforceStdout help:evaluate -Dexpression=project.version
git diff --check
mvn -B clean verify
mvn -B javadoc:javadoc
mvn -B dependency:list -DincludeScope=compile
mvn -B -Pbenchmarks clean package
java -jar target/benchmarks.jar -wi 5 -i 5 -f 1 -w 200ms -r 200ms -prof gc -rf json -rff target/release-0.1.0-jmh-results.json
git add pom.xml CHANGELOG.md
git commit -m "Release kiss-binary 0.1.0"
git tag -a v0.1.0 -m "Release kiss-binary 0.1.0"
git push origin main
git push origin v0.1.0
```

Publishing and release:

```bash
gh workflow run release.yml --repo arthurhoch/kiss-binary --ref main -f release_version=0.1.0
gh run watch --repo arthurhoch/kiss-binary 25552730692 --exit-status
curl -fsSI https://repo1.maven.org/maven2/io/github/arthurhoch/kiss-binary/0.1.0/kiss-binary-0.1.0.pom
curl -fsSI https://repo.maven.apache.org/maven2/io/github/arthurhoch/kiss-binary/0.1.0/kiss-binary-0.1.0.pom
gh release create v0.1.0 --repo arthurhoch/kiss-binary --title "kiss-binary 0.1.0" --notes-file RELEASE_NOTES_0.1.0.md
```

Post-release:

```bash
mvn -B clean verify
git status --short
git log --oneline --decorate -5
git tag --list "v0.1.0"
mvn -q -DforceStdout help:evaluate -Dexpression=project.version
gh run list --repo arthurhoch/kiss-binary --limit=5
gh run watch --repo arthurhoch/kiss-binary 25553119499 --exit-status
gh run watch --repo arthurhoch/kiss-binary 25553119506 --exit-status
```

## 3. Local Verification Results

- `mvn -B clean verify`: passed before release preparation on `0.1.0-SNAPSHOT`.
- `mvn -B javadoc:javadoc`: passed before release preparation.
- `mvn -B dependency:list -DincludeScope=compile`: passed with no compile-scope dependencies.
- `mvn -B -Pbenchmarks clean package`: passed and built the JMH benchmark jar.
- `java -jar target/benchmarks.jar -l`: passed.
- After changing `pom.xml` to `0.1.0`, `mvn -B clean verify` passed with 120 tests, 0 failures, 0 errors, and 6 skipped full-dataset tests.
- After changing `pom.xml` to `0.1.0`, `mvn -B javadoc:javadoc` passed.
- After changing `pom.xml` to `0.1.0`, `mvn -B dependency:list -DincludeScope=compile` passed with no compile-scope dependencies.
- After changing `pom.xml` to `0.1.0`, `mvn -B -Pbenchmarks clean package` passed.
- The full JMH command completed and wrote `target/release-0.1.0-jmh-results.json`.
- After bumping to the next development snapshot, `mvn -B clean verify` passed with 120 tests, 0 failures, 0 errors, and 6 skipped full-dataset tests.
- After pushing the next-snapshot commit, GitHub Actions CI passed on run `25553119499`.
- After pushing the next-snapshot commit, CodeQL passed on run `25553119506`.

## 4. Tag Information

- Tag: `v0.1.0`
- Tag type: annotated
- Tag message: `Release kiss-binary 0.1.0`
- Tagged commit: `b8fd1dfd499542f73ed09cce67f70b4f4a3f65ea`
- Tagged commit subject: `Release kiss-binary 0.1.0`
- Tag pushed to origin: yes

## 5. GitHub Workflow Run

- Workflow: `Maven Central Release`
- Run URL: <https://github.com/arthurhoch/kiss-binary/actions/runs/25552730692>
- Status: completed
- Conclusion: success
- Started: 2026-05-08T11:19:47Z
- Completed: 2026-05-08T11:27:21Z

The workflow verified the release version, ran `mvn -B clean verify`, generated Javadocs, checked compile-scope dependencies, imported the configured GPG key, and ran `mvn -B -Prelease -Dgpg.passphrase="$GPG_PASSPHRASE" deploy`.

## 6. Maven Central Publish Status

Publication succeeded through the GitHub Actions release workflow.

The artifact POM was publicly reachable from Maven Central during verification:

- <https://repo1.maven.org/maven2/io/github/arthurhoch/kiss-binary/0.1.0/kiss-binary-0.1.0.pom>
- <https://repo.maven.apache.org/maven2/io/github/arthurhoch/kiss-binary/0.1.0/kiss-binary-0.1.0.pom>

Coordinates:

```xml
<dependency>
    <groupId>io.github.arthurhoch</groupId>
    <artifactId>kiss-binary</artifactId>
    <version>0.1.0</version>
</dependency>
```

## 7. GitHub Release

- URL: <https://github.com/arthurhoch/kiss-binary/releases/tag/v0.1.0>
- Title: `kiss-binary 0.1.0`
- Draft: no
- Prerelease: no
- Release notes file: `RELEASE_NOTES_0.1.0.md`

## 8. Next Snapshot Version

The project was bumped to the next development snapshot after successful publication and release creation.

## 9. Manual Follow-Up Needed

- Monitor Maven Central search/indexing if desired. Direct repository access was already verified, but Central search UI indexing can lag.
- Future releases should ideally go through a pull request or an explicitly documented admin bypass. This release push reported bypassed branch-rule violations because direct pushes to `main` normally require PR/status checks.

## 10. Anything Not Completed

- No Maven Central local publish command was run.
- No secrets were printed or written to files.
- No additional version tags were created beyond `v0.1.0`.
- No Maven Central release was attempted before local verification, GitHub secrets validation, and successful workflow execution.

## 11. Final Repository State

- Working tree after the next-snapshot push: clean.
- Current branch: `main`.
- Next-snapshot commit: `0dc2440 Prepare next development iteration`.
- Current project version after release: next `-SNAPSHOT`.
- `v0.1.0` remains present locally and on origin.
- CodeQL emitted one non-blocking annotation: it could not build an overlay-base database with manual build mode and fell back to a normal full database.
