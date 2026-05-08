# KissBinary — Binary Format Guide

**Status: Initial implementation complete.**

This guide explains how to design simple, deterministic binary file formats using KissBinary.

## Philosophy

KissBinary does not define a file format. You do. The library gives you primitives to read and write, and you arrange them however you like.

This guide recommends patterns that work well with the library's API.

## Recommended Binary Format Layout

A typical binary file format has this structure:

```
┌──────────────────────────┐
│ Magic (2-4 bytes)        │  Identify the file type
│ Version                  │  Format version for compatibility
│ Header fields            │  Counts, sizes, offsets
│ Data sections            │  Primitive arrays, records
└──────────────────────────┘
```

The order is intentional: validate first, then read.

### Step 1: Magic Bytes

Choose a fixed byte sequence that identifies your file type. Use 2-4 bytes.

```
Magic: 0x4B 0x42  ("KB" for KissBinary example)
```

Recommendations:

- Use printable ASCII for readability when hex-dumping.
- Keep it short (2-4 bytes).
- Choose something unique to your application.
- Always validate magic before reading any other content.

### Step 2: Version

Include a version number after the magic. `BinaryWriter.writeVersion(int)` and `BinaryReader.validateVersion(int)` use a 4-byte `int`. If your format needs a more compact version field, use `writeByte`/`readByte` or `writeShort`/`readShort` and validate it explicitly.

```
Version: 0x01
```

This lets you evolve the format without breaking old readers.

### Step 3: Counts and Dimensions

After magic and version, write counts and sizes that describe the data sections.

```
Record count: int (4 bytes)
Index count: int (4 bytes)
```

These values tell the reader how much data to expect. Validate them before allocating.

### Step 4: Data Sections

Write data sections in a fixed order. Each section contains primitives or primitive arrays.

```
Records: count * recordSize bytes
Index: indexCount * 8 bytes (long offsets)
```

### Step 5: Offsets (Optional)

For random-access formats, include an offset table or fixed-size records.

```
Record 0: offset 16, size 24
Record 1: offset 40, size 24
Record 2: offset 64, size 24
```

## Example Conceptual File Layout

A compact dataset file with header, counts, and two arrays:

```
Offset  Size  Field         Description
------  ----  -----         -----------
0       2     magic         0x4B 0x42 ("KB")
2       4     version       0x00000001 (int, BE, from writeVersion)
6       4     keyCount      number of keys (int, BE)
10      4     valueCount    number of values (int, BE)
14      4*keyCount  keys    int array
14+4*K  8*valueCount vals  long array
```

Total size: `14 + 4*keyCount + 8*valueCount` bytes.

## Endianness Choices

### Big-Endian (Default)

- Consistent with Java conventions and network byte order.
- Consistent with `ByteBuffer` and `DataOutputStream` defaults.
- Recommended for new formats.
- Easier to debug in hex editors (most-significant byte first).

### Little-Endian

- Consistent with x86/x64 architecture.
- Required when reading existing formats that use LE (e.g., WAV, BMP, many game formats).
- Must be explicitly specified when creating reader/writer.

Choose one endianness for the entire file. Do not mix endianness within a single file format.

## Alignment Considerations

KissBinary does not enforce alignment. However, for performance-sensitive formats:

- Align 4-byte values to 4-byte offsets.
- Align 8-byte values to 8-byte offsets.
- Use padding bytes (e.g., `writer.writeByte((byte) 0)`) if needed.
- Memory-mapped access may benefit from natural alignment.

For most use cases, alignment does not matter because modern JVMs handle unaligned access efficiently.

## Forward Compatibility

To support format evolution:

1. Always include magic and version.
2. When adding fields, increment the version.
3. Readers should check version and handle known versions explicitly.
4. Writers should write the highest version they support.
5. New fields should be appended at the end of the format.
6. Avoid changing existing field sizes or order.

Pattern:

```java
reader.validateMagic(MAGIC);
int version = reader.readVersion(); // or readByte/readShort for compact custom versions
if (version < MIN_VERSION || version > MAX_VERSION) {
    throw new BinaryFormatException("Unsupported version: " + version);
}
// Read fields based on version
```

## Validation Strategy

Always validate before allocating:

1. **Validate magic** — reject unknown file types immediately.
2. **Validate version** — reject unsupported versions immediately.
3. **Validate counts** — reject negative or unreasonably large counts before allocating arrays.
4. **Validate remaining bytes** — ensure enough data exists for declared counts.
5. **Validate individual values** — check ranges where applicable.

Example:

```java
reader.validateMagic(MAGIC);
reader.validateVersion(1);
int count = reader.readInt();
if (count < 0) {
    throw new BinaryFormatException("Negative count: " + count);
}
if (count * RECORD_SIZE > reader.remaining()) {
    throw new BinaryFormatException("Count " + count + " exceeds remaining data");
}
```

## What Not to Do

1. **Do not skip magic validation** for "trusted" files.
2. **Do not allocate before validating** counts and remaining bytes.
3. **Do not mix endianness** within a single file.
4. **Do not use variable-length fields** unless the format specifically requires them.
5. **Do not rely on file size alone** — validate internal consistency.
6. **Do not ignore EOF** during reads.
