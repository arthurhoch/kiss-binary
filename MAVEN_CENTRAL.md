# KissBinary — Maven Central Publishing Guide

**Status: Version `0.1.0` is published on Maven Central.**

Do not publish future releases until code, tests, documentation, benchmarks, release workflow, credentials, and release tag are complete.

## Maven Coordinates

- **groupId**: `io.github.arthurhoch`
- **artifactId**: `kiss-binary`
- **Latest stable version**: `0.1.0`

```xml
<dependency>
    <groupId>io.github.arthurhoch</groupId>
    <artifactId>kiss-binary</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Sonatype Central Portal Setup

KissBinary uses the [Sonatype Central Publisher Portal](https://central.sonatype.com/) for Maven Central publishing.

### Prerequisites

1. Sonatype Central account linked to `io.github.arthurhoch` namespace.
2. Namespace verified via DNS TXT record or GitHub repository verification.
3. User token generated from Sonatype Central Portal.

### Required Secrets (GitHub Repository Secrets)

| Secret | Purpose |
|--------|---------|
| `MAVEN_CENTRAL_USERNAME` | Sonatype Central Portal user token username |
| `MAVEN_CENTRAL_PASSWORD` | Sonatype Central Portal user token password |
| `GPG_PRIVATE_KEY` | GPG private key for artifact signing |
| `GPG_PASSPHRASE` | Passphrase for the GPG private key |

The normal build (`mvn -B verify`) must never require these secrets.

## GPG Signing

All published artifacts must be signed with GPG.

### Setup

1. Generate a GPG key pair:

```bash
gpg --full-generate-key
```

2. Export the private key:

```bash
gpg --armor --export-secret-keys YOUR_KEY_ID > private-key.asc
```

3. Store `private-key.asc` content as `GPG_PRIVATE_KEY` secret.
4. Store the passphrase as `GPG_PASSPHRASE` secret.

### Verification

Verify signing works locally:

```bash
mvn -Prelease verify
```

## Release Profile

The `release` Maven profile in `pom.xml` must configure:

1. Source JAR generation (`maven-source-plugin`).
2. Javadoc JAR generation (`maven-javadoc-plugin`).
3. GPG signing (`maven-gpg-plugin`).
4. Sonatype Central Portal publishing (`central-publishing-maven-plugin` or `sonatype-central-plugin`).

The profile is activated only during release:

```bash
mvn -Prelease deploy
```

Normal builds do not activate this profile:

```bash
mvn -B verify
```

## pom.xml Requirements

The `pom.xml` must include:

```xml
<groupId>io.github.arthurhoch</groupId>
<artifactId>kiss-binary</artifactId>
<version>0.1.0</version>
<packaging>jar</packaging>

<name>KissBinary</name>
<description>A tiny, zero-dependency Java 17+ binary IO library.</description>
<url>https://github.com/arthurhoch/kiss-binary</url>

<licenses>
    <license>
        <name>Apache License 2.0</name>
        <url>https://www.apache.org/licenses/LICENSE-2.0</url>
    </license>
</licenses>

<developers>
    <developer>
        <name>Arthur Hoch</name>
        <email>arthurhoch@users.noreply.github.com</email>
    </developer>
</developers>

<scm>
    <url>https://github.com/arthurhoch/kiss-binary</url>
    <connection>scm:git:git://github.com/arthurhoch/kiss-binary.git</connection>
</scm>
```

## Publishing Process

1. Ensure all tests pass.
2. Ensure all documentation is current.
3. Ensure `pom.xml` version is correct (no `-SNAPSHOT` for release).
4. Ensure `CHANGELOG.md` is updated.
5. Tag the release: `git tag v0.1.0`.
6. Push the tag to GitHub.
7. GitHub Actions release workflow runs:
   - Checks out the tagged commit.
   - Runs `mvn -B verify`.
   - Runs `mvn -Prelease deploy`.
   - Artifacts are signed, published to Sonatype Central Portal.
8. Verify artifact on Maven Central (may take a few minutes to propagate).

## Do Not

- Do not publish future releases until implementation and tests are complete.
- Do not publish from a local machine. Use GitHub Actions only.
- Do not commit secrets to the repository.
- Do not skip GPG signing.
- Do not publish `-SNAPSHOT` versions.
- Do not re-publish the same version. Use a new version number.
