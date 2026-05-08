# KissBinary — Product Specification

**Status: Initial implementation complete; not yet released.**

## Project Name

KissBinary

## Mission

Make reading and writing primitive binary data in Java so simple that you can do it from memory, with direct control over byte layout, predictable performance, and rich errors when something goes wrong.

## Problem Statement

Java developers who need to read or write binary file formats have limited options:

1. **`DataInputStream` / `DataOutputStream`**: JDK standard, but always big-endian, no bounds checking, no header validation, and generic `IOException` on EOF with no context about what was being read or where.

2. **`ByteBuffer`**: Flexible but verbose. Users must manage position, limit, flip, rewind, and endianness manually. Error messages are unhelpful.

3. **Serialization frameworks** (Kryo, Protobuf, FlatBuffers, Avro): Powerful but heavy. They bring schemas, code generation, reflection, annotations, and complexity that is unnecessary when all you need is to read and write some `int`s, `long`s, and `byte[]` arrays in a known layout.

None of these make the common case trivial: read a header, validate magic and version, read some primitives and arrays, get clear errors when the file is truncated or malformed.

KissBinary solves this by providing a tiny, memorable API that:

- Reads and writes primitives and primitive arrays directly.
- Validates headers with magic bytes, version numbers, and counts.
- Includes file offset and expected vs actual values in error messages.
- Supports both little-endian and big-endian from day one.
- Offers memory-mapped read access for large static files.
- Has zero external dependencies.

## Target Users

1. Java developers building custom binary file formats.
2. Infrastructure tool authors who need compact binary data.
3. Game and simulation developers who need compact binary data access with predictable layout.
4. Developers building binary indexes, caches, or static datasets.
5. Participants in coding competitions (e.g., Rinha de Backend) who need compact data storage.
6. Developers who cannot or will not add external dependencies for binary IO.
7. Developers who want direct control over byte layout without schema overhead.

## Core Use Cases

1. **Compact dataset files**: Write primitive data to a file, read it back deterministically.
2. **Binary indexes**: Build lookup tables with fixed-size entries for O(1) access.
3. **Static runtime data**: Load read-only data at startup via memory mapping.
4. **Header validation**: Validate magic bytes and version before processing file content.
5. **Primitive array storage**: Store and retrieve `int[]`, `long[]`, `double[]`, etc.
6. **Binary caches**: Serialize application state as primitives to a compact binary format.
7. **Binary snapshots**: Dump and restore application data quickly.

## v0.1.0 Scope

### Core API

1. **BinaryWriter**: Write primitives and primitive arrays. Output to `byte[]` or `OutputStream`.
2. **BinaryReader**: Read primitives and primitive arrays. Input from `byte[]` or `ByteBuffer`.
3. **MappedBinaryReader**: Read-only memory-mapped access via `FileChannel` / `MappedByteBuffer`.

### Data Types

All Java primitive types:
- `byte`, `short`, `int`, `long`, `float`, `double`, `char`, `boolean`
- Primitive arrays: `byte[]`, `short[]`, `int[]`, `long[]`, `float[]`, `double[]`, `char[]`

### Endianness

- Explicit endianness per reader/writer instance.
- Default: big-endian (network byte order, consistent with Java conventions).
- Little-endian support for x86-native formats.

### Header Validation

- Magic byte validation.
- Version number validation.
- Count/size field validation.

### Error Handling

- `BinaryFormatException` for malformed, truncated, or unexpected data.
- File offset in error messages where applicable.
- Expected vs actual values for magic and version mismatches.

### Infrastructure

- Maven project, Java 17, zero production dependencies.
- JUnit 5 tests.
- Maven Central publishing configuration.
- Documentation.

## v0.1.0 Non-Goals

1. No Java object serialization.
2. No schema engine, IDL, or code generation.
3. No reflection-based mapping.
4. No annotation-driven configuration.
5. No checksum/CRC (future consideration).
6. No compression.
7. No encryption.
8. No network IO.
9. No HTTP, JSON, or XML handling.
10. No Spring, Quarkus, or framework integrations.
11. No Lombok, annotation processing, or code generation.
12. No variable-length encoding (VarInt, etc.) in v0.1.0.

## Future Scope

- Checksum/CRC validation for file integrity.
- Variable-length integer encoding (VarInt, ZigZag).
- String encoding/decoding with explicit charset.
- Structured header definitions.
- Off-heap / direct buffer support.
- Append-only writer mode.
- Streaming reader for very large files.

Each future feature must justify its inclusion against the KISS principle.

## Performance Expectations

- Primitive reads and writes must be direct `ByteBuffer` operations — no intermediate objects, no boxing.
- Array reads and writes must use bulk `ByteBuffer` operations where possible.
- Bounds checking must happen before reads, not after.
- No reflection, no object serialization, no hidden allocation on hot paths.
- Memory-mapped reads must be competitive with direct `ByteBuffer` access.
- JMH benchmarks must be captured before v0.1.0 release.

Do not claim specific performance numbers until benchmarks exist.

## Safety Expectations

- All reads must check remaining bytes before reading.
- EOF must produce a clear `BinaryFormatException`, not a generic `IOException`.
- Magic and version validation must happen before processing file content.
- Array sizes from untrusted sources must be validated before allocation.
- Negative counts must be rejected.
- No silent truncation.

## Compatibility Expectations

- Java 17 source and target. No preview features.
- Zero production dependencies.
- No native code. Pure Java.
- Android-compatible by design (pending validation).
- GraalVM Native Image friendly by design (pending validation).

## What Must Remain Simple

1. The public API must fit on one screen.
2. `BinaryWriter.create()` and `BinaryReader.from(data)` must work with safe defaults.
3. Endianness must be explicit, never implicit.
4. Error messages must be readable by humans.
5. The library must never require the user to understand its internals.
