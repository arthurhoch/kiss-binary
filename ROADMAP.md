# KissBinary — Roadmap

**Status: Initial v0.1.0 implementation under release review.**

## v0.1.0

Core binary IO:

- `BinaryWriter` — write primitives and primitive arrays
- `BinaryReader` — read primitives and primitive arrays
- `MappedBinaryReader` — memory-mapped read-only access
- `Endianness` — big-endian (default) and little-endian
- Header validation — magic bytes, version, counts
- `BinaryException` / `BinaryFormatException` — rich errors with offset and context
- Bounds checking and safe EOF handling
- JUnit 5 test suite
- JMH benchmarks
- Maven Central publishing preparation

## v0.2.0

Potential additions (not committed):

- String read/write with explicit charset and length prefix
- Variable-length integer encoding (VarInt, ZigZag)
- Checksum/CRC support for file integrity
- Structured header helper class
- Append-only writer mode

Each v0.2.0 feature must justify its inclusion against the KISS principle and must include benchmarks before acceptance.

## Future Ideas

- Off-heap / direct buffer support for zero-copy reads
- Streaming reader for very large files (cursor-based with windowing)
- Binary format migration utilities
- Compact date/time encoding (epoch millis, epoch seconds)
- Bit-level operations for packed fields

None of these are committed. All require design review, benchmarks, and human approval.

## What Will Not Be Added

These are explicitly out of scope for KissBinary:

- Java object serialization (`ObjectOutputStream`, `Serializable`)
- Schema engine, IDL, or code generation
- Reflection-based object mapping
- Annotation-driven configuration
- Compression (use a compression library)
- Encryption (use an encryption library)
- Network IO (use kiss-requests or another HTTP library)
- HTTP server (use kiss-server or another server library)
- JSON/XML handling (use kiss-json or another library)
- Database abstraction
- ORM functionality
- Spring, Quarkus, or framework integrations
- Lombok, annotation processing, or code generation

## What Requires Benchmarks Before Inclusion

- Any new data type or encoding that affects hot-path performance
- Any change to the internal buffer strategy
- Any abstraction that adds method call overhead
- Memory-mapped IO vs file IO trade-offs
- Off-heap buffer access patterns
- Any claim that a new feature improves measured performance
