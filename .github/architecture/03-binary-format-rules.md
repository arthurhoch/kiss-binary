# 03 — Binary Format Rules

This document defines the recommended binary format layout and validation rules for formats built with KissBinary.

## Recommended Layout

```
┌──────────────────────┐
│ Magic (2-4 bytes)    │  Fixed byte sequence identifying the file type
│ Version             │  Format version for forward compatibility
│ Header fields        │  Counts, sizes, offsets, dimensions
│ Data sections        │  Primitive arrays, fixed-size records
└──────────────────────┘
```

## Magic Bytes

- Use 2-4 bytes of fixed data at the start of the file.
- Choose printable ASCII for readability when hex-dumping.
- Must be validated before any other processing.
- KissBinary provides `validateMagic(byte[])` for this purpose.

## Version

- Use `writeVersion`/`validateVersion` for a 4-byte int version field, or use explicit byte/short primitive methods when the format requires a compact custom version field.
- Unsigned byte (0-255) or unsigned short (0-65535) is sufficient for many compact custom formats.
- Increment when the format changes incompatibly.
- Readers should check version and reject unknown versions explicitly.

## Counts and Dimensions

- Counts and sizes must be written before the data they describe.
- All counts must be non-negative.
- All counts must be validated against remaining bytes before allocation.
- Example: write `int count`, then `count` elements.

## Offsets

- For random-access formats, include an offset table or use fixed-size records.
- Offsets must be validated against file size before use.
- Use `long` (8 bytes) for offsets when files may exceed 2 GB.

## Primitive Arrays

- Arrays are written and read with explicit count.
- KissBinary does not write the count automatically — the user controls the format.
- Bulk operations use `ByteBuffer` view buffers for efficiency.
- Array count must be validated before allocation.

## Endianness

- Choose one endianness for the entire file.
- Big-endian is recommended for new formats (consistent with Java conventions).
- Little-endian is required when reading existing x86-native formats.
- Do not mix endianness within a single file.
- Document the chosen endianness in the format specification.

## Alignment

- KissBinary does not enforce alignment.
- For performance-sensitive formats, align multi-byte values to their natural boundaries.
- Use padding bytes if needed.
- Memory-mapped reads may benefit from natural alignment.

## Forward Compatibility

- Always include magic and version.
- Append new fields at the end of the format.
- Do not change existing field sizes or order.
- Readers should handle known versions explicitly and reject unknown versions.

## Validation Before Allocation

This is a critical security and correctness rule:

1. Validate magic before processing content.
2. Validate version before interpreting format details.
3. Validate counts are non-negative.
4. Validate counts against remaining bytes.
5. Only then allocate arrays or buffers.

Never allocate based on untrusted data without validation.

## Validation Pattern

```java
reader.validateMagic(MAGIC);
reader.validateVersion(VERSION);
int count = reader.readInt();
if (count < 0) {
    throw new BinaryFormatException("Negative count at offset X: " + count);
}
if (count * ELEMENT_SIZE > reader.remaining()) {
    throw new BinaryFormatException("Count exceeds remaining data");
}
int[] data = reader.readIntArray(count);
```
