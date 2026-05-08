# KissBinary — Examples

**Status: Initial implementation complete.**

These examples describe current usage.

## Writing a Compact Dataset File

```java
int[] keys = {1, 2, 3, 4, 5};
long[] values = {100L, 200L, 300L, 400L, 500L};

BinaryWriter writer = BinaryWriter.create();
writer.writeByteArray(new byte[]{0x4B, 0x42});  // magic
writer.writeVersion(1);                          // version
writer.writeInt(keys.length);                    // key count
writer.writeIntArray(keys);                      // keys
writer.writeLongArray(values);                   // values

Files.write(Path.of("dataset.bin"), writer.toByteArray());
```

## Reading a Compact Dataset File

```java
byte[] data = Files.readAllBytes(Path.of("dataset.bin"));
BinaryReader reader = BinaryReader.from(data);

reader.validateMagic(new byte[]{0x4B, 0x42});
reader.validateVersion(1);

int count = reader.readInt();
int[] keys = reader.readIntArray(count);
long[] values = reader.readLongArray(count);
```

## Validating a File Header

```java
BinaryReader reader = BinaryReader.from(data);

// Validate magic
reader.validateMagic(new byte[]{0x4D, 0x59, 0x46, 0x4D}); // "MYFM"

// Validate version
reader.validateVersion(2);

// Read header fields
int recordCount = reader.readInt();
long indexOffset = reader.readLong();

// Validate consistency
if (recordCount < 0) {
    throw new IllegalArgumentException("Negative record count");
}
```

## Using Primitive Arrays

```java
// Write mixed primitive arrays
BinaryWriter writer = BinaryWriter.create();
writer.writeInt(timestamps.length);
writer.writeLongArray(timestamps);
writer.writeDoubleArray(measurements);
writer.writeByteArray(flags);
byte[] result = writer.toByteArray();

// Read mixed primitive arrays
BinaryReader reader = BinaryReader.from(result);
int count = reader.readInt();
long[] timestamps = reader.readLongArray(count);
double[] measurements = reader.readDoubleArray(count);
byte[] flags = reader.readByteArray(count);
```

## Using Memory-Mapped Read for Large Static Data

```java
try (MappedBinaryReader reader = MappedBinaryReader.from(Path.of("large-index.bin"))) {
    // Read header at fixed offsets
    reader.validateMagic(0, new byte[]{0x49, 0x44, 0x58}); // "IDX"
    int entryCount = reader.readInt(3);
    long dataOffset = reader.readLong(7);

    // Random access by index
    for (int i = 0; i < entryCount; i++) {
        long entryOffset = dataOffset + (i * 16L);
        long key = reader.readLong(entryOffset);
        long value = reader.readLong(entryOffset + 8);
        // process key/value
    }
}
```

## Building a Binary Index for Fast Startup

```java
// Build phase: write index
BinaryWriter writer = BinaryWriter.create();
writer.writeByteArray(new byte[]{0x4B, 0x49}); // "KI" magic
writer.writeVersion(1);                         // version
writer.writeInt(entries.size());                // entry count

for (Map.Entry<String, Long> entry : entries.entrySet()) {
    // Write fixed-size key (padded to 32 bytes) + offset
    byte[] keyBytes = entry.getKey().getBytes(StandardCharsets.UTF_8);
    byte[] padded = Arrays.copyOf(keyBytes, 32);
    writer.writeByteArray(padded);
    writer.writeLong(entry.getValue());
}

Files.write(Path.of("index.bin"), writer.toByteArray());

// Startup phase: memory-map the index for offset-based lookup
try (MappedBinaryReader index = MappedBinaryReader.from(Path.of("index.bin"))) {
    index.validateMagic(0, new byte[]{0x4B, 0x49});
    index.validateVersion(2, 1);
    int count = index.readInt(6);
    // Direct random access to any entry by offset
    long entryOffset = 10 + (targetIndex * 40L);
    byte[] key = index.readByteArray(entryOffset, 32);
    long offset = index.readLong(entryOffset + 32);
}
```

## Handling Errors

```java
try {
    BinaryReader reader = BinaryReader.from(data);
    reader.validateMagic(new byte[]{0x4B, 0x42});
    int count = reader.readInt();
    int[] values = reader.readIntArray(count);
} catch (BinaryFormatException e) {
    // Error includes offset, expected vs actual, and clear message
    System.err.println("Binary format error: " + e.getMessage());
    // Example output:
    // "At offset 0: Invalid magic: expected [0x4B, 0x42], actual [0x4A, 0x53]"
}
```

## Building a Compact Vector Dataset

This example shows the pattern used in the Rinha dataset benchmark. A full working implementation is in `src/test/java/io/github/arthurhoch/kissbinary/rinha/`.

### Write a vector dataset with labels

```java
int vectorCount = 1000;
int logicalDimensions = 14;
int physicalDimensions = 16; // padded for alignment
int labelWordCount = (vectorCount + 63) / 64;

BinaryWriter writer = BinaryWriter.create(Endianness.LITTLE_ENDIAN);

// Header
writer.writeMagic("KBRN");
writer.writeVersion(1);
writer.writeInt(logicalDimensions);
writer.writeInt(physicalDimensions);
writer.writeInt(vectorCount);
writer.writeInt(labelWordCount);
writer.writeInt(0); // reserved
writer.writeInt(0); // reserved

// Vectors: 14 data dimensions + 2 zero-padded, stored as int16
for (int i = 0; i < vectorCount; i++) {
    for (int d = 0; d < logicalDimensions; d++) {
        writer.writeShort(quantize(vector[d], 10_000));
    }
    for (int d = logicalDimensions; d < physicalDimensions; d++) {
        writer.writeShort((short) 0);
    }
}

// Labels as a bitset
long[] labels = new long[labelWordCount];
// ... set bits for fraud vectors ...
for (long word : labels) {
    writer.writeLong(word);
}

Files.write(Path.of("vectors.kbin"), writer.toByteArray());
```

### Read and validate the vector dataset

```java
byte[] data = Files.readAllBytes(Path.of("vectors.kbin"));
BinaryReader reader = BinaryReader.from(data, Endianness.LITTLE_ENDIAN);

reader.expectMagic("KBRN");
reader.expectVersion(1);
int logicalDims = reader.readInt();
int physicalDims = reader.readInt();
int vectorCount = reader.readInt();
int labelWordCount = reader.readInt();

// Read vector at index i
int HEADER_SIZE = 32;
int vectorByteSize = physicalDims * Short.BYTES;
// Skip to vector i: position = HEADER_SIZE + i * vectorByteSize
```

For memory-mapped random access to large datasets:

```java
try (MappedBinaryReader mmap = MappedBinaryReader.from(Path.of("vectors.kbin"),
        Endianness.LITTLE_ENDIAN)) {
    mmap.expectMagic("KBRN");
    int vectorCount = mmap.readInt(16); // offset 16 in header

    // Random access to vector at index 42
    long offset = 32 + 42L * 16 * 2;
    short[] vec = mmap.readShortArray(offset, 16);
}
```
