# KissBinary

A tiny, zero-dependency Java 17+ binary IO library for reading and writing primitive binary formats.

Zero external dependencies. No framework. No object serialization. No schema engine.

## Status

**Initial implementation complete.** Local tests and Maven build pass. JMH benchmark results are available under [benchmark-results/](benchmark-results/). Version 0.1.0 is published to Maven Central. See [CHANGELOG.md](CHANGELOG.md) for details.

## What It Is

KissBinary reads and writes explicit binary data: primitives, primitive arrays, and binary headers with magic/version validation. It gives you direct control over byte layout, endianness, and structure without hiding anything behind a framework.

It exists for cases where you need to read or write compact binary data - indexes, caches, datasets, static data, snapshots - and you want a small, predictable library that does exactly what you tell it to.

## What It Is Not

- Not a Java serialization framework. No `ObjectOutputStream`. No `Serializable`.
- Not Kryo, Protobuf, FlatBuffers, or any schema-driven serialization system.
- Not a database, storage engine, or file system abstraction.
- Not a compression library.
- Not an encryption library.
- Not a reflection-based object mapper.
- Not an annotation-driven framework.
- Not a replacement for `java.io.DataInputStream` / `DataOutputStream` (though it solves similar problems with safer defaults and richer errors).

## Philosophy

- **KISS**: Keep It Simple, Stupid.
- **Zero dependencies**: No external libraries required.
- **Native JDK**: Built only with Java 17+ standard APIs.
- **Explicit binary formats**: You define the layout. The library reads and writes primitives.
- **Predictable performance**: No reflection, no object serialization, no hidden allocation.
- **Rich errors**: Include file offset, expected vs actual values, clear messages.
- **Safe defaults**: Bounds-checked, EOF-safe, explicit endianness.

## Quick Examples

### Write

```java
import io.github.arthurhoch.kissbinary.*;
import java.nio.file.Path;
import java.nio.file.Files;

BinaryWriter writer = BinaryWriter.create();
writer.writeMagic("KB");
writer.writeVersion(1);
writer.writeInt(42);
writer.writeLong(1000L);
writer.writeDouble(3.14);
writer.writeByteArray(new byte[]{0x01, 0x02, 0x03});
writer.writeIntArray(new int[]{10, 20, 30});

Files.write(Path.of("data.bin"), writer.toByteArray());
```

### Read

```java
byte[] data = Files.readAllBytes(Path.of("data.bin"));
BinaryReader reader = BinaryReader.from(data);

reader.expectMagic("KB");
reader.expectVersion(1);
int value = reader.readInt();         // 42
long big = reader.readLong();          // 1000L
double pi = reader.readDouble();       // 3.14
byte[] arr = reader.readByteArray(3);  // [1, 2, 3]
int[] nums = reader.readIntArray(3);   // [10, 20, 30]
```

### Header Validation

```java
reader.expectMagic("KB");
reader.expectVersion(1);
int count = reader.readInt();
```

### Memory-Mapped Read

```java
try (MappedBinaryReader mmap = MappedBinaryReader.from(Path.of("index.bin"))) {
    mmap.expectMagic("KB");
    int count = mmap.readInt(6);
    long offset = mmap.readLong(10);
}
```

### Endianness

```java
BinaryWriter writer = BinaryWriter.create(Endianness.LITTLE_ENDIAN);
BinaryReader reader = BinaryReader.from(data, Endianness.LITTLE_ENDIAN);
```

## Maven

```xml
<dependency>
    <groupId>io.github.arthurhoch</groupId>
    <artifactId>kiss-binary</artifactId>
    <version>0.1.0</version>
</dependency>
```

Available from Maven Central.

## When to Use KissBinary

- Reading and writing custom binary file formats.
- Building compact binary indexes for startup data loading.
- Storing primitive arrays in a deterministic binary layout.
- Validating file headers with magic bytes and version numbers.
- Memory-mapped read access for large static datasets.
- Applications that need direct control over byte layout.
- Embedding static binary data in applications.
- Building binary caches, snapshots, or compact datasets.

## When Not to Use KissBinary

- If you need Java object serialization, use `ObjectOutputStream` or a serialization framework.
- If you need schema-driven serialization, use Protobuf, FlatBuffers, or Avro.
- If you need JSON, use [kiss-json](https://github.com/arthurhoch/kiss-json) or another JSON library.
- If you need HTTP transport, use [kiss-requests](https://github.com/arthurhoch/kiss-requests) or another HTTP library.
- If you need an HTTP server, use [kiss-server](https://github.com/arthurhoch/kiss-server) or another server library.
- If you need a database, use a database.
- If you need compression, use a compression library.
- If you need encryption, use an encryption library.

## Ecosystem

KissBinary is a sibling of:

- [kiss-requests](https://github.com/arthurhoch/kiss-requests) — tiny HTTP client library
- [kiss-json](https://github.com/arthurhoch/kiss-json) — tiny JSON library
- [kiss-server](https://github.com/arthurhoch/kiss-server) — tiny HTTP server library

Each library is independent. They do not depend on each other. They compose naturally:

```java
// Read binary index at startup
MappedBinaryReader index = MappedBinaryReader.from(Path.of("cache.idx"));

// Serve data via HTTP
server.get("/data/{id}", ctx -> {
    long offset = lookupOffset(index, id);
    byte[] payload = readPayload(index, offset);
    return ctx.bytes(payload);
});

// Write JSON responses
server.get("/status", ctx -> ctx.text("{\"status\":\"ok\"}"));
```

## Real-World Validation

KissBinary includes a performance validation using the Rinha de Backend 2026 reference dataset (3,000,000 labeled vectors). This proves kiss-binary can write, read, validate, and memory-map a ~96 MB compact binary file derived from a real dataset.

See [docs/rinha-dataset-benchmark.md](docs/rinha-dataset-benchmark.md) for details.

## v0.1.0 Scope

- `BinaryWriter` — write primitives and primitive arrays to an in-memory byte buffer
- `BinaryReader` — read primitives and primitive arrays from `byte[]` or `ByteBuffer`
- `MappedBinaryReader` — memory-mapped read-only access
- Little-endian and big-endian support
- Header validation: magic bytes, version, counts
- Bounds checking and safe EOF handling
- `BinaryException` / `BinaryFormatException`
- Maven Central publishing preparation

## Non-Goals

- No object serialization framework
- No schema engine or IDL
- No reflection-based mapping
- No annotation-driven configuration
- No checksum/CRC in v0.1.0
- No compression support
- No encryption support
- No network IO
- No Spring, Quarkus, or framework integrations
- No Lombok, annotation processing, or code generation

## Documentation

- [Documentation site](https://arthurhoch.github.io/kiss-binary/)
- [CAVEMAN.md](CAVEMAN.md) — compact project summary
- [Product Specification](PRODUCT_SPEC.md)
- [Implementation Plan](IMPLEMENTATION_PLAN.md)
- [API Design](API_DESIGN.md)
- [Performance](PERFORMANCE.md)
- [Error Handling](ERROR_HANDLING.md)
- [Binary Format Guide](BINARY_FORMAT_GUIDE.md)
- [Examples](EXAMPLES.md)
- [Roadmap](ROADMAP.md)
- [Release Guide](RELEASE.md)
- [Maven Central Publishing](MAVEN_CENTRAL.md)
- [Architecture](.github/architecture/index.md)

## License

Apache License 2.0. Copyright 2026 Arthur Hoch. See [LICENSE.txt](LICENSE.txt).
