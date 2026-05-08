# Binary Format Design

**Status: Initial implementation complete.**

This page provides user-facing guidance for designing binary formats with KissBinary.

For the full design guide, see [BINARY_FORMAT_GUIDE.md](../BINARY_FORMAT_GUIDE.md).

## Design Process

1. **Define your fields** — what data do you need to store?
2. **Choose endianness** — big-endian (recommended) or little-endian.
3. **Define the header** — magic bytes, version, counts.
4. **Define the data sections** — primitives and arrays in a fixed order.
5. **Document the format** — write down offsets, sizes, and field meanings.

## Recommended Structure

```
Magic (2-4 bytes)
Version (4 bytes when using `writeVersion`, or 1-2 bytes when using compact custom primitive fields)
Counts and dimensions
Data sections
```

## Tips

- Validate magic and version before processing content.
- Write counts before the arrays they describe.
- Choose one endianness for the entire file.
- Include version for forward compatibility.
- Align multi-byte values for performance if needed.
- Keep formats simple and deterministic.

## Common Patterns

### Fixed-Size Records

```
Header: magic (2) + version (4 with `writeVersion`) + recordCount (4) + recordSize (4)
Records: recordCount * recordSize bytes
```

### Variable-Length with Offset Table

```
Header: magic (2) + version (4 with `writeVersion`) + count (4)
Offset table: count * 8 bytes (long offsets)
Data sections: variable length
```

### Key-Value Index

```
Header: magic (2) + version (4 with `writeVersion`) + entryCount (4)
Entries: entryCount * (keySize + valueSize) bytes
```

## Validation

Always validate before allocating:

```java
reader.validateMagic(MAGIC);
reader.validateVersion(1);
int count = reader.readInt();
// validate count before allocating
int[] data = reader.readIntArray(count);
```
