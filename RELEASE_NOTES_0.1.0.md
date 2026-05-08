# kiss-binary 0.1.0

Initial Maven Central release of KissBinary.

## Highlights

- Zero-dependency Java 17+ binary IO for explicit primitive formats.
- `BinaryWriter`, `BinaryReader`, and `MappedBinaryReader`.
- Big-endian and little-endian support.
- Primitive and primitive-array read/write APIs.
- Caller-provided primitive array read APIs for allocation-sensitive hot paths.
- Header helpers for magic and version validation.
- Rich malformed/truncated input errors via `BinaryFormatException`.
- JMH benchmark profile and Rinha-shaped benchmark coverage.

## Maven

```xml
<dependency>
    <groupId>io.github.arthurhoch</groupId>
    <artifactId>kiss-binary</artifactId>
    <version>0.1.0</version>
</dependency>
```

