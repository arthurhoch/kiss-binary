# API Overview

**Status: Initial implementation complete.**

This page describes the current public API areas.

## API Areas

### BinaryWriter

Write primitives and primitive arrays to an in-memory byte buffer.

- Create with `BinaryWriter.create()` (big-endian default) or `BinaryWriter.create(Endianness)`.
- Write methods: `writeByte`, `writeShort`, `writeInt`, `writeLong`, `writeFloat`, `writeDouble`, `writeChar`, `writeBoolean`.
- Array methods: `writeByteArray`, `writeShortArray`, `writeIntArray`, `writeLongArray`, `writeFloatArray`, `writeDoubleArray`, `writeCharArray`.
- Output: `toByteArray()` returns `byte[]`, `writeTo(OutputStream)` writes to a stream.
- Size: `size()` returns the number of bytes written.

### BinaryReader

Read primitives and primitive arrays from a byte buffer.

- Create with `BinaryReader.from(byte[])` or `BinaryReader.from(ByteBuffer)`.
- Optional endianness: `BinaryReader.from(data, Endianness)`.
- Read methods: `readByte`, `readShort`, `readInt`, `readLong`, `readFloat`, `readDouble`, `readChar`, `readBoolean`.
- Array methods: `readByteArray(count)`, `readShortArray(count)`, etc.
- Validation: `validateMagic(byte[])`, `validateVersion(int)`.
- Position: `position()`, `remaining()`, `hasRemaining()`.

### MappedBinaryReader

Read-only memory-mapped access to binary files.

- Create with `MappedBinaryReader.from(Path)` or `MappedBinaryReader.from(Path, Endianness)`.
- Position-based reads: `readInt(offset)`, `readLong(offset)`, etc.
- Array reads at offset: `readIntArray(offset, count)`, etc.
- Validation: `validateMagic(offset, byte[])`, `validateVersion(offset, int)`.
- Size: `size()` returns the mapped file size.
- Implements `AutoCloseable` — use with try-with-resources.

### Endianness

Enum with two values:

- `BIG_ENDIAN` — default, consistent with Java and network byte order.
- `LITTLE_ENDIAN` — for x86-native formats.

Set at creation time. Cannot be changed.

### Exceptions

- `BinaryException` — base exception for library errors.
- `BinaryFormatException` — data errors (EOF, bounds, magic/version mismatch).

All exceptions are unchecked. Error messages include file offset and context.

## Design Principles

- **Memorable**: Method names match Java primitive type names.
- **Explicit**: Endianness is always specified or defaulted. No implicit behavior.
- **Safe**: Bounds-checked by default. EOF produces clear errors.
- **Small**: Fewer than 10 public types total.
