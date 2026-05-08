# KissBinary — API Design

**Status: Initial implementation complete; API may still change before v0.1.0.**

This document describes the intended public API shape. The API may change before v0.1.0.

## Design Principles

1. **Memorable**: Class and method names should be guessable.
2. **Explicit**: Endianness is always explicit or configured. No implicit behavior.
3. **Safe**: Bounds-checked by default. EOF produces clear errors.
4. **Small**: The public API should fit on one screen.
5. **Direct**: Read and write primitives. No intermediate objects.

## Public API Package

`io.github.arthurhoch.kissbinary`

## Core Classes

### Endianness

Specifies byte order for multi-byte primitives.

```java
public enum Endianness {
    BIG_ENDIAN,
    LITTLE_ENDIAN
}
```

- Default: `BIG_ENDIAN` (network byte order, consistent with Java conventions).
- Users explicitly choose `LITTLE_ENDIAN` when reading x86-native formats.

### BinaryWriter

Writes primitives and primitive arrays to a byte stream.

```java
public final class BinaryWriter {

    public static BinaryWriter create();
    public static BinaryWriter create(Endianness endianness);

    public void writeByte(byte value);
    public void writeByte(int value);
    public void writeShort(short value);
    public void writeInt(int value);
    public void writeLong(long value);
    public void writeFloat(float value);
    public void writeDouble(double value);
    public void writeChar(char value);
    public void writeBoolean(boolean value);

    public void writeByteArray(byte[] value);
    public void writeBytes(byte[] value);
    public void writeBytes(byte[] value, int offset, int length);
    public void writeShortArray(short[] value);
    public void writeIntArray(int[] value);
    public void writeLongArray(long[] value);
    public void writeFloatArray(float[] value);
    public void writeDoubleArray(double[] value);
    public void writeCharArray(char[] value);

    public void writeMagic(String asciiMagic);
    public void writeVersion(int version);

    public void writeTo(OutputStream out);
    public byte[] toByteArray();
    public int size();
    public int position();
}
```

Resource management: `BinaryWriter` owns an internal `ByteArrayOutputStream`-like buffer. It does not close streams passed to `writeTo()`. The caller manages the `OutputStream` lifecycle.

### BinaryReader

Reads primitives and primitive arrays from a byte buffer.

```java
public final class BinaryReader {

    public static BinaryReader from(byte[] data);
    public static BinaryReader from(byte[] data, Endianness endianness);
    public static BinaryReader from(ByteBuffer buffer);
    public static BinaryReader from(ByteBuffer buffer, Endianness endianness);

    public byte readByte();
    public short readShort();
    public int readInt();
    public long readLong();
    public float readFloat();
    public double readDouble();
    public char readChar();
    public boolean readBoolean();

    public byte[] readByteArray(int count);
    public byte[] readBytes(int length);
    public void readFully(byte[] target);
    public void readFully(byte[] target, int offset, int length);
    public long skipBytes(long byteCount);
    public void skipFully(long byteCount);
    public short[] readShortArray(int count);
    public void readShortArray(short[] target);
    public void readShortArray(short[] target, int offset, int length);
    public int[] readIntArray(int count);
    public void readIntArray(int[] target);
    public void readIntArray(int[] target, int offset, int length);
    public long[] readLongArray(int count);
    public void readLongArray(long[] target);
    public void readLongArray(long[] target, int offset, int length);
    public float[] readFloatArray(int count);
    public void readFloatArray(float[] target);
    public void readFloatArray(float[] target, int offset, int length);
    public double[] readDoubleArray(int count);
    public void readDoubleArray(double[] target);
    public void readDoubleArray(double[] target, int offset, int length);
    public char[] readCharArray(int count);

    public void validateMagic(byte[] expected);
    public void expectMagic(String asciiMagic);
    public int readVersion();
    public void validateVersion(int expected);
    public void expectVersion(int expected);

    public int position();
    public int remaining();
    public boolean hasRemaining();
}
```

Resource management: `BinaryReader` wraps a `ByteBuffer`. It does not own the underlying buffer. The caller manages the `ByteBuffer` and any associated resources.

### MappedBinaryReader

Read-only memory-mapped access to binary files.

```java
public final class MappedBinaryReader implements AutoCloseable {

    public static MappedBinaryReader from(Path file);
    public static MappedBinaryReader from(Path file, Endianness endianness);

    public byte readByte(long offset);
    public short readShort(long offset);
    public int readInt(long offset);
    public long readLong(long offset);
    public float readFloat(long offset);
    public double readDouble(long offset);
    public char readChar(long offset);
    public boolean readBoolean(long offset);

    public byte[] readByteArray(long offset, int count);
    public void readBytes(long offset, byte[] target, int targetOffset, int length);
    public short[] readShortArray(long offset, int count);
    public void readShortArray(long offset, short[] target);
    public void readShortArray(long offset, short[] target, int targetOffset, int length);
    public int[] readIntArray(long offset, int count);
    public void readIntArray(long offset, int[] target);
    public void readIntArray(long offset, int[] target, int targetOffset, int length);
    public long[] readLongArray(long offset, int count);
    public void readLongArray(long offset, long[] target);
    public void readLongArray(long offset, long[] target, int targetOffset, int length);
    public float[] readFloatArray(long offset, int count);
    public void readFloatArray(long offset, float[] target);
    public void readFloatArray(long offset, float[] target, int targetOffset, int length);
    public double[] readDoubleArray(long offset, int count);
    public void readDoubleArray(long offset, double[] target);
    public void readDoubleArray(long offset, double[] target, int targetOffset, int length);
    public char[] readCharArray(long offset, int count);

    public void validateMagic(long offset, byte[] expected);
    public void expectMagic(String asciiMagic);
    public void validateVersion(long offset, int expected);

    public long size();
    @Override
    public void close();
}
```

Resource management: `MappedBinaryReader` owns a `FileChannel` and `MappedByteBuffer`. It implements `AutoCloseable` and should be used with try-with-resources:

```java
try (MappedBinaryReader reader = MappedBinaryReader.from(Path.of("data.bin"))) {
    int count = reader.readInt(0);
    // ...
}
```

### BinaryException

Base exception for all KissBinary errors.

```java
public class BinaryException extends RuntimeException {
    public String getMessage();
}
```

### BinaryFormatException

Exception for malformed, truncated, or unexpected binary data.

```java
public class BinaryFormatException extends BinaryException {
    public long offset();
    public String getMessage(); // includes offset, expected vs actual where applicable
}
```

## Endianness Behavior

- `BinaryWriter.create()` defaults to `BIG_ENDIAN`.
- `BinaryReader.from(data)` defaults to `BIG_ENDIAN`.
- `MappedBinaryReader.from(file)` defaults to `BIG_ENDIAN`.
- Endianness can be specified at creation time and applies to all subsequent operations.
- Endianness cannot be changed after creation.

## Header Validation Pattern

Header validation is a pattern, not a framework. Users call validation methods explicitly:

```java
BinaryReader reader = BinaryReader.from(data);
reader.validateMagic(new byte[]{0x4B, 0x42}); // "KB" for KissBinary
reader.validateVersion(1);
int recordCount = reader.readInt();
// ... read records
```

`validateMagic` and `validateVersion` throw `BinaryFormatException` if the expected value does not match. The exception includes the file offset, the expected value, and the actual value.

## Array Read/Write Pattern

Arrays are written and read with explicit counts. The count is not written automatically — users control the format:

```java
// Write: user decides whether to write the count
writer.writeInt(items.length);         // count
writer.writeIntArray(items);           // data

// Read: user reads count, then data
int count = reader.readInt();
int[] items = reader.readIntArray(count);
```

Hot paths can reuse caller-owned arrays to avoid allocating a new primitive array per read:

```java
short[] vector = new short[16];
reader.readShortArray(vector);
```

For cursor-based readers, `skipFully(byteCount)` advances without materializing skipped bytes. `skipBytes(byteCount)` skips up to the requested number of bytes and returns the number actually skipped.

This keeps the format explicit and under user control.

## Exception Hierarchy

```
RuntimeException
  └── BinaryException
        └── BinaryFormatException
```

- `BinaryException`: base class for all library errors (invalid arguments, unsupported operations).
- `BinaryFormatException`: data-specific errors (EOF, bounds, magic/version mismatch, truncated data).

All exceptions are unchecked. Binary IO errors are typically unrecoverable and should not require `try/catch` boilerplate for normal control flow.

## Safe Defaults

| Default | Rationale |
|---------|-----------|
| Endianness | `BIG_ENDIAN` — consistent with Java conventions and network byte order |
| Bounds checking | Enabled — all reads check remaining bytes first |
| EOF handling | `BinaryFormatException` — never generic `IOException` |
| Array validation | Count must be non-negative and within remaining bytes |
| Magic/version | Explicit validation methods — never automatic |
| Resource management | Caller-owned — library never closes resources it did not create (except `MappedBinaryReader`) |

## What Is Not in the Public API

- No `Serializable` support.
- No reflection-based mapping.
- No schema definitions.
- No annotation-driven configuration.
- No variable-length encoding.
- No checksum/CRC in v0.1.0.
- No compression or encryption.
- No object graph serialization.
- No streaming/async IO.
