# 04 — Performance Rules

This document defines the performance rules for KissBinary implementation.

## Hot Path Principles

The hot path is any method that reads or writes a primitive or array. These rules apply to all hot path code.

### No Allocation on Hot Path

- Primitive read/write methods must not allocate objects.
- Array read methods allocate the result array (unavoidable) but must use bulk fill.
- No intermediate wrapper objects, no boxing, no temporary buffers.

### No Reflection

- Reflection adds per-access overhead that is unpredictable and hard for the JIT to optimize.
- KissBinary reads and writes primitives directly via `ByteBuffer`.
- No reflective field access, no method handle invocation on hot paths.

### No Object Serialization

- Object serialization involves reflection, intermediate object graphs, and hidden allocation.
- KissBinary only handles primitives and primitive arrays.
- No `ObjectOutputStream`, no `Serializable`, no object graph traversal.

### No Hidden Buffering

- `BinaryWriter` owns one internal buffer. No double-buffering.
- `BinaryReader` wraps a `ByteBuffer`. No intermediate copy.
- `MappedBinaryReader` wraps a `MappedByteBuffer`. No copy at all.
- Users who pass `OutputStream` to `writeTo()` manage buffering themselves.

### Bounds Check Before Operation

- One comparison per read: `remaining() >= bytesRequired`.
- Bounds check happens before the data access, not after.
- The cost is one branch prediction, which modern CPUs handle efficiently.

## Array Operations

Array operations must use `ByteBuffer` bulk methods:

- `ByteBuffer.get(byte[])` for byte arrays.
- `ByteBuffer.asIntBuffer().get(int[])` for int arrays.
- Equivalent view buffer operations for other primitive types.

These leverage JVM-optimized buffer operations and avoid per-element loops.

## File IO vs Memory-Mapped IO

### BinaryWriter / BinaryReader (In-Memory)

- All operations happen on `byte[]` or `ByteBuffer` in memory.
- No file IO in the read/write path.
- Users manage file IO: load `byte[]` from file, create reader, write `byte[]` to file.

### MappedBinaryReader (Memory-Mapped)

- Uses `FileChannel.map()` for read-only memory mapping.
- No data copy from kernel to user space after initial mmap.
- Position-based reads: `readInt(offset)`.
- Suitable for large static files with random access patterns.

Performance expectation: mmap should be competitive with direct `ByteBuffer` for random access. Sequential access may not benefit.

## Benchmark Before Claiming

1. No performance claim without JMH benchmark results.
2. All benchmark results must include JVM version, OS, hardware, warmup, measurement, and raw output.
3. Compare against `DataInputStream` / `DataOutputStream` and raw `ByteBuffer` as baselines.
4. Label measured hot paths separately from other paths.
5. Local directional results only, not universal production guarantees.

## What to Measure

- Primitive read/write throughput (ops/sec).
- Array read/write throughput (bytes/sec).
- Endianness comparison (LE vs BE).
- Memory-mapped read throughput and latency.
- Allocation rate (bytes per operation).
- Bounds check overhead (with vs without).
