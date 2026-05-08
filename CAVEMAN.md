# KissBinary — Caveman Summary

## What this is

Java 17+ binary IO library. Zero dependencies. Reads and writes primitives, arrays, headers. No object serialization. No reflection. No schema engine.

## Main mental model

Write:

```java
BinaryWriter writer = BinaryWriter.create();
writer.writeInt(42);
writer.writeLong(1000L);
writer.writeDouble(3.14);
writer.writeIntArray(new int[]{1, 2, 3});
byte[] data = writer.toByteArray();
```

Read:

```java
BinaryReader reader = BinaryReader.from(data);
int value = reader.readInt();
long big = reader.readLong();
double pi = reader.readDouble();
int[] arr = reader.readIntArray(3);
```

## v0.1.0 must support

- BinaryWriter: write primitives, arrays, to byte[] or OutputStream
- BinaryReader: read primitives, arrays, from byte[] or ByteBuffer
- MappedBinaryReader: mmap read-only access
- Little-endian and big-endian
- Header validation: magic bytes, version, counts
- Bounds checking
- Safe EOF handling
- BinaryFormatException with offset, expected vs actual

## KISS rules

- Keep API tiny.
- No dependencies.
- No reflection.
- No object serialization.
- No schema engine.
- No annotations.
- No framework.
- No code generation.
- No Lombok.
- Do the simple thing.
- Measure before claiming performance.
- Validate before allocating.

## What this is not

Not Kryo. Not Protobuf. Not FlatBuffers. Not Java Serialization. Not a database. Not a schema engine. Not an ORM. Not a compression library. Not an encryption library.

## Public API shape

- BinaryWriter — write primitives and arrays
- BinaryReader — read primitives and arrays
- MappedBinaryReader — mmap read-only access
- Endianness — LITTLE_ENDIAN, BIG_ENDIAN
- BinaryFormatException — rich error with offset and context

## Error rule

All binary read failures throw BinaryFormatException. Include file offset where available. Include expected vs actual for magic/version. No vague RuntimeException. No silent truncation.

## Performance rule

Read and write primitives directly. No intermediate objects. No reflection. No boxing where avoidable. Bounds-check before reads. Benchmark with JMH before claiming performance.

## Security rule

Do not trust file headers. Validate magic, version, and sizes before allocating memory. Cap array sizes on read. Reject negative counts.

## Before coding

Read these files in order:

1. CAVEMAN.md (this file — compact summary)
2. AGENTS.md (authoritative rules)
3. PRODUCT_SPEC.md (full spec)
4. IMPLEMENTATION_PLAN.md (phases)
5. API_DESIGN.md (API shape)
6. PERFORMANCE.md (performance goals)
7. .github/architecture/index.md (architecture rules)

CAVEMAN.md is a summary only. If it conflicts with detailed docs, the detailed docs win.
