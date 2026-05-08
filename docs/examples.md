---
layout: default
---

# Examples

**Status: Initial implementation complete.**

These examples describe current usage.

For the full examples document, see [EXAMPLES.md](../EXAMPLES.md).

## Write a Compact Dataset

```java
BinaryWriter writer = BinaryWriter.create();
writer.writeByteArray(new byte[]{0x4B, 0x42}); // magic
writer.writeVersion(1);                        // version
writer.writeInt(keys.length);                  // count
writer.writeIntArray(keys);
writer.writeLongArray(values);
Files.write(Path.of("data.bin"), writer.toByteArray());
```

## Read a Compact Dataset

```java
BinaryReader reader = BinaryReader.from(Files.readAllBytes(Path.of("data.bin")));
reader.validateMagic(new byte[]{0x4B, 0x42});
reader.validateVersion(1);
int count = reader.readInt();
int[] keys = reader.readIntArray(count);
long[] values = reader.readLongArray(count);
```

## Memory-Map a File

```java
try (MappedBinaryReader reader = MappedBinaryReader.from(Path.of("index.bin"))) {
    int count = reader.readInt(0);
    for (int i = 0; i < count; i++) {
        long offset = 4 + (i * 16L);
        long key = reader.readLong(offset);
        long value = reader.readLong(offset + 8);
    }
}
```

## Handle Errors

```java
try {
    reader.validateMagic(new byte[]{0x4B, 0x42});
} catch (BinaryFormatException e) {
    System.err.println("Format error: " + e.getMessage());
}
```

## See Also

- [EXAMPLES.md](../EXAMPLES.md) — full examples document.
- [getting-started.md](getting-started.md) — getting started guide.
- [api-overview.md](api-overview.md) — API overview.
