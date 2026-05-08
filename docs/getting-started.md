---
layout: default
---

# Getting Started

**Status: Initial implementation complete.**

KissBinary is not yet published. This page describes the current usage flow.

## Installation

Once published to Maven Central:

```xml
<dependency>
    <groupId>io.github.arthurhoch</groupId>
    <artifactId>kiss-binary</artifactId>
    <version>0.1.0</version>
</dependency>
```

No transitive dependencies. Just the JDK.

## Usage

### Write binary data

```java
BinaryWriter writer = BinaryWriter.create();
writer.writeInt(42);
writer.writeLong(1000L);
writer.writeDouble(3.14);
byte[] data = writer.toByteArray();
```

### Read binary data

```java
BinaryReader reader = BinaryReader.from(data);
int value = reader.readInt();
long big = reader.readLong();
double pi = reader.readDouble();
```

### Validate a file header

```java
reader.validateMagic(new byte[]{0x4B, 0x42});
reader.validateVersion(1);
int count = reader.readInt();
```

### Use little-endian

```java
BinaryWriter writer = BinaryWriter.create(Endianness.LITTLE_ENDIAN);
BinaryReader reader = BinaryReader.from(data, Endianness.LITTLE_ENDIAN);
```

### Memory-map a file

```java
try (MappedBinaryReader reader = MappedBinaryReader.from(Path.of("data.bin"))) {
    int count = reader.readInt(0);
}
```

## Next Steps

- [API Overview](api-overview.md)
- [Binary Format Design](binary-format-design.md)
- [Examples](examples.md)
- [Performance Notes](performance-notes.md)
