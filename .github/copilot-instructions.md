# GitHub Copilot Instructions

Use [CAVEMAN.md](../CAVEMAN.md) as the compact project summary. Do not treat it as a replacement for detailed rules.

## Library Purpose

KissBinary is a tiny, zero-dependency Java 17+ binary IO library. It reads and writes explicit binary data: primitives, primitive arrays, and binary headers with magic/version validation.

It is not a framework. It is not a serialization engine. It is not a schema tool.

## Public API Direction

The central mental model:

```java
BinaryWriter writer = BinaryWriter.create();
writer.writeInt(42);
writer.writeLong(1000L);
byte[] data = writer.toByteArray();

BinaryReader reader = BinaryReader.from(data);
int value = reader.readInt();
long big = reader.readLong();
```

Memory-mapped read access:

```java
try (MappedBinaryReader reader = MappedBinaryReader.from(Path.of("data.bin"))) {
    int count = reader.readInt(0);
}
```

Header validation:

```java
reader.validateMagic(new byte[]{0x4B, 0x42});
reader.validateVersion(1);
```

## Package Structure

- Base package: `io.github.arthurhoch.kissbinary`
- Do not introduce extra public subpackages unless they simplify the API.

## KISS Constraints

- Zero production dependencies.
- Java 17+ compatibility.
- No object serialization (`ObjectOutputStream`, `Serializable`).
- No reflection-based mapping.
- No schema engine, IDL, or code generation.
- No annotations, no annotation processing.
- No framework patterns.
- No Lombok.
- The user defines the binary layout. The library reads and writes primitives.

## Testing Expectations

- JUnit 5 for all tests.
- No internet access required.
- Tests must be deterministic.
- Every public method must have at least one test.
- Cover: primitive roundtrip, endianness, arrays, headers, bounds, EOF, malformed input, mmap.

## Documentation Expectations

- README.md for quick start.
- docs/ for user-facing documentation.
- Every public API must have at least one copyable example.
- Update docs when public behavior changes.
- English only.
- Do not claim features work if they are not implemented.

## Forbidden Additions

- No production dependencies.
- No Lombok, no annotation processing, no code generation.
- No Spring, Quarkus, or framework integrations.
- No object serialization, reflection-based mapping, or schema engines.
- No compression or encryption.
- No network IO.

## Architecture Docs

Read `.github/architecture/index.md` before making changes. Architecture docs are authoritative for implementation decisions.

## Preferred Java Style

- Java 17 source and target.
- Prefer simple classes and records over complex hierarchies.
- Prefer explicit names over clever abstractions.
- No reflection-based magic.
- Every exception must be informative with context.
- When in doubt, choose the simpler solution.
