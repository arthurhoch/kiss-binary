# 01 — Product Boundaries

## What KissBinary Is

KissBinary is a tiny, zero-dependency Java 17+ binary IO library. It reads and writes explicit binary data: primitives, primitive arrays, and binary headers with magic/version validation.

It gives users direct control over byte layout with predictable performance and rich error messages.

## What KissBinary Is Not

KissBinary is explicitly **not** any of the following:

- **Not Kryo.** No object graph serialization, no class registration, no reflective field mapping.
- **Not Protobuf.** No schema language, no code generation, no `.proto` files, no wire format compatibility.
- **Not FlatBuffers.** No schema compiler, no zero-copy object model, no IDL.
- **Not Avro.** No schema registry, no code generation, no container format.
- **Not Java Serialization.** No `ObjectOutputStream`, no `Serializable`, no `ObjectInputStream`.
- **Not a database.** No query engine, no indexing, no transaction support.
- **Not a schema engine.** No IDL, no schema definitions, no format evolution framework.
- **Not an ORM.** No object-relational mapping, no SQL, no database abstraction.
- **Not a compression library.** Use `java.util.zip` or similar for compression.
- **Not an encryption library.** Use `javax.crypto` or similar for encryption.
- **Not a network library.** No HTTP, no sockets, no RPC.

## Ecosystem Boundaries

KissBinary is a sibling of kiss-requests, kiss-json, and kiss-server. Each library is independent and does not depend on the others.

| Library | Purpose | KissBinary Relationship |
|---------|---------|------------------------|
| kiss-requests | HTTP client | No dependency. Applications may use kiss-requests to download binary data, then use KissBinary to parse it. |
| kiss-json | JSON library | No dependency. Applications may use kiss-json for JSON config and KissBinary for binary data. |
| kiss-server | HTTP server | No dependency. Applications may use kiss-server to serve binary data read by KissBinary. |

The application is responsible for composing these libraries. KissBinary does not know about HTTP, JSON, or server frameworks.

## Boundary Between KissBinary and Application Code

KissBinary provides:

- Primitive read/write operations.
- Array read/write operations.
- Header validation methods.
- Memory-mapped read access.

The application is responsible for:

- Defining the binary format (field order, sizes, endianness).
- Managing files and resources.
- Implementing domain-specific logic on top of primitive data.
- Choosing between in-memory and memory-mapped access.
- Handling application-level errors.

## What Must Not Leak Across Boundaries

- KissBinary must not depend on kiss-requests, kiss-json, or kiss-server.
- KissBinary must not depend on any external library.
- KissBinary must not impose a file format on the user.
- KissBinary must not serialize Java objects.
- KissBinary must not use reflection for data mapping.
