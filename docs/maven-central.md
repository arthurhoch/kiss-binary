---
layout: default
---

# Maven Central

KissBinary is published through the Sonatype Central Publisher Portal.

## Coordinates

```xml
<dependency>
    <groupId>io.github.arthurhoch</groupId>
    <artifactId>kiss-binary</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Release Requirements

- `pom.xml` must contain required Maven Central metadata.
- Source and Javadoc JARs must be generated.
- Artifacts must be signed with GPG.
- Production compile-scope dependencies must remain zero.
- Release publishing requires `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `GPG_PRIVATE_KEY`, and `GPG_PASSPHRASE` repository secrets.

## Links

- [Maven Central artifact](https://central.sonatype.com/artifact/io.github.arthurhoch/kiss-binary)
- [Release guide](release.md)
- [Root Maven Central guide](https://github.com/arthurhoch/kiss-binary/blob/main/MAVEN_CENTRAL.md)
