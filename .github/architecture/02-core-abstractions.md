# 02 — Core Abstractions

This document defines the planned public API types and explains why each exists.

## Planned Abstractions

### BinaryWriter

**Purpose**: Write primitives and primitive arrays to a byte stream.

**Why it exists**: Java's `DataOutputStream` is always big-endian, has no array bulk operations, and throws generic `IOException`. `BinaryWriter` provides explicit endianness, bulk array writes, and rich errors.

**Key design decisions**:
- Owns an internal growable byte buffer.
- Does not close streams passed to `writeTo()`.
- Endianness is set at creation and cannot change.
- No object serialization. Only primitives and primitive arrays.

### BinaryReader

**Purpose**: Read primitives and primitive arrays from a byte buffer.

**Why it exists**: Java's `DataInputStream` is always big-endian, has no bounds checking, and throws generic `EOFException` without context. `BinaryReader` provides explicit endianness, bounds checking, and `BinaryFormatException` with file offset and context.

**Key design decisions**:
- Wraps a `ByteBuffer`. Does not own the underlying buffer.
- Bounds-checks before every read.
- EOF throws `BinaryFormatException` with offset and remaining bytes.
- Endianness is set at creation and cannot change.
- Has a cursor (position) that advances on each read.

### MappedBinaryReader

**Purpose**: Read-only memory-mapped access to binary files.

**Why it exists**: For large static files, memory mapping avoids copying data into heap buffers. Position-based reads (`readInt(offset)`) allow random access without cursor management.

**Key design decisions**:
- Owns a `FileChannel` and `MappedByteBuffer`. Implements `AutoCloseable`.
- Position-based reads: `readInt(offset)` takes an explicit offset, does not advance a cursor.
- Read-only. No write operations.
- Suitable for large files that are read many times.
- Caller should use try-with-resources.

### Endianness

**Purpose**: Specify byte order for multi-byte primitives.

**Why it exists**: Binary formats use different byte orders. KissBinary supports both big-endian (Java/network default) and little-endian (x86 native). Endianness must always be explicit.

**Key design decisions**:
- Enum: `BIG_ENDIAN`, `LITTLE_ENDIAN`.
- Set at reader/writer creation time.
- Cannot be changed after creation.
- Default: `BIG_ENDIAN`.

### BinaryHeader (Conceptual)

**Purpose**: A validation pattern, not a class. Users call `validateMagic()` and `validateVersion()` on `BinaryReader`.

**Why it exists**: Most binary files start with a header containing magic bytes and a version number. KissBinary provides validation methods for this pattern without imposing a specific header format.

**Key design decisions**:
- Not a separate class in v0.1.0.
- Validation methods on `BinaryReader` and `MappedBinaryReader`.
- Throws `BinaryFormatException` with expected vs actual values.

### BinaryException

**Purpose**: Base exception for all KissBinary errors.

**Why it exists**: Provides a common catch point for all library errors. Used for invalid arguments, unsupported operations, and configuration errors.

### BinaryFormatException

**Purpose**: Exception for malformed, truncated, or unexpected binary data.

**Why it exists**: Binary IO errors need context — what went wrong, where, and what was expected. `BinaryFormatException` carries this context without requiring checked exceptions.

**Key design decisions**:
- Extends `BinaryException`.
- Includes file offset where the error was detected.
- Includes expected vs actual values for validation errors.
- Unchecked: binary format errors are typically unrecoverable.

## What Must Not Be Abstracted

1. **No format abstraction**: The user defines the format. KissBinary does not impose a layout.
2. **No type system**: No type descriptors, no type IDs, no type registries.
3. **No object mapping**: No reflection, no field mapping, no annotation scanning.
4. **No streaming abstraction**: No async IO, no reactive streams, no event-based parsing.
5. **No schema engine**: No IDL, no schema definitions, no format evolution framework.
6. **No plugin system**: No custom serializers, no codec registry, no extension points.

The abstraction boundary is deliberately thin: primitives go in, primitives come out. The user controls everything else.
