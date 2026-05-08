# KissBinary JMH Results

Raw results are saved in `benchmark-results/jmh-results.json`.

This run used JMH throughput mode with 5 warmup iterations, 5 measurement iterations, 2 forks, 200 ms per iteration, and the JMH GC profiler. See `benchmark-results/environment.md` for the machine, JVM, and exact command.

JMH is the OpenJDK Java Microbenchmark Harness for JVM benchmarks: <https://openjdk.org/projects/code-tools/jmh/>.

## Summary

- The benchmarks ran successfully and produced 80 primary benchmark results.
- In this local run, kiss-binary read benchmarks showed high throughput relative to `DataInputStream`.
- In this local run, raw heap `ByteBuffer` was ahead of kiss-binary in several primitive write, header validation, random-offset, and sequential scan scenarios.
- The evidence does not support a broad claim that kiss-binary has "extremely high performance".
- Allocation numbers below are the JMH GC profiler's `gc.alloc.rate.norm` values for this run.

## Primitive Writes

| Benchmark | Implementation | Score ops/s | Error | Alloc B/op |
|---|---|---:|---:|---:|
| writeInt | kiss | 41.05M | 4.16M | 104.001 |
| writeInt | dataOutputStream | 58.39M | 7.67M | 72.001 |
| writeInt | byteBufferHeap | 106.73M | 17.84M | 24.000 |
| writeInt | byteBufferDirect | 2.40M | 1.00M | 136.012 |
| writeLong | kiss | 32.66M | 8.14M | 104.001 |
| writeLong | dataOutputStream | 56.87M | 9.52M | 72.001 |
| writeLong | byteBufferHeap | 102.71M | 36.55M | 24.000 |
| writeLong | byteBufferDirect | 2.63M | 1.38M | 136.012 |
| writeDouble | kiss | 48.80M | 5.37M | 104.001 |
| writeDouble | dataOutputStream | 63.06M | 16.86M | 72.001 |
| writeDouble | byteBufferHeap | 100.23M | 19.30M | 24.000 |
| writeDouble | byteBufferDirect | 2.43M | 1.04M | 136.013 |
| mixed record | kiss | 41.19M | 4.17M | 144.001 |
| mixed record | dataOutputStream | 34.27M | 6.40M | 104.001 |
| mixed record | byteBufferHeap | 192.77M | 30.72M | 40.000 |
| mixed record | byteBufferDirect | 2.31M | 986.35K | 136.012 |

## Primitive Reads

| Benchmark | Implementation | Score ops/s | Error | Alloc B/op |
|---|---|---:|---:|---:|
| readInt | kiss | 338.55M | 20.41M | <0.001 |
| readInt | dataInputStream | 158.76M | 12.06M | 56.000 |
| readInt | byteBufferHeap | 396.66M | 35.87M | <0.001 |
| readInt | byteBufferDirect | 210.97M | 16.74M | <0.001 |
| readLong | kiss | 308.11M | 36.59M | <0.001 |
| readLong | dataInputStream | 126.94M | 18.07M | 56.000 |
| readLong | byteBufferHeap | 364.02M | 36.35M | <0.001 |
| readLong | byteBufferDirect | 202.75M | 15.60M | <0.001 |
| readDouble | kiss | 319.72M | 29.77M | <0.001 |
| readDouble | dataInputStream | 137.68M | 16.87M | 56.000 |
| readDouble | byteBufferHeap | 399.88M | 22.86M | <0.001 |
| readDouble | byteBufferDirect | 205.92M | 12.08M | <0.001 |
| mixed record | kiss | 237.26M | 22.44M | <0.001 |
| mixed record | dataInputStream | 92.76M | 9.21M | 56.000 |
| mixed record | byteBufferHeap | 305.83M | 16.90M | <0.001 |
| mixed record | byteBufferDirect | 181.48M | 11.05M | <0.001 |

## Array Writes

Array length: 1024 elements.

| Array | Implementation | Score ops/s | Alloc B/op |
|---|---|---:|---:|
| short | kiss | 3.07M | 4,320.0 |
| short | byteBufferHeap | 3.97M | 2,176.0 |
| short | byteBufferDirect | 1.18M | 136.038 |
| int | kiss | 1.99M | 8,416.0 |
| int | byteBufferHeap | 3.12M | 4,224.0 |
| int | byteBufferDirect | 712.63K | 165.243 |
| long | kiss | 966.89K | 16,608.0 |
| long | byteBufferHeap | 1.79M | 8,320.0 |
| long | byteBufferDirect | 735.31K | 136.965 |
| float | kiss | 1.96M | 8,528.0 |
| float | byteBufferHeap | 3.11M | 4,224.0 |
| float | byteBufferDirect | 866.64K | 137.273 |
| double | kiss | 980.22K | 16,720.0 |
| double | byteBufferHeap | 1.73M | 8,320.0 |
| double | byteBufferDirect | 677.18K | 137.106 |

## Array Reads

Array length: 1024 elements.

| Array | Implementation | Score ops/s | Alloc B/op |
|---|---|---:|---:|
| short | kiss | 4.28M | 2,064.0 |
| short | byteBufferHeap | 3.87M | 2,176.0 |
| short | byteBufferDirect | 3.86M | 2,120.0 |
| int | kiss | 3.25M | 4,296.0 |
| int | byteBufferHeap | 2.96M | 4,224.0 |
| int | byteBufferDirect | 3.07M | 4,168.0 |
| long | kiss | 1.61M | 8,392.0 |
| long | byteBufferHeap | 1.70M | 8,320.0 |
| long | byteBufferDirect | 1.75M | 8,264.0 |
| float | kiss | 3.27M | 4,296.0 |
| float | byteBufferHeap | 3.09M | 4,224.0 |
| float | byteBufferDirect | 3.13M | 4,168.0 |
| double | kiss | 1.67M | 8,392.0 |
| double | byteBufferHeap | 1.72M | 8,320.0 |
| double | byteBufferDirect | 1.86M | 8,236.0 |

## Header Validation

| Benchmark | Implementation | Score ops/s | Error | Alloc B/op |
|---|---|---:|---:|---:|
| expectMagic | kiss | 141.83M | 15.52M | 48.000 |
| expectMagic | byteBufferHeap | 320.01M | 10.75M | <0.001 |
| expectVersion | kiss | 264.84M | 41.11M | <0.001 |
| expectVersion | byteBufferHeap | 332.47M | 23.40M | <0.001 |

## Mapped Random Offset Reads

Records: 8192 fixed-size records containing `int`, `long`, and `double`.

| Primitive | Implementation | Score ops/s | Alloc B/op |
|---|---|---:|---:|
| int | mappedBinaryReader | 110.18M | <0.001 |
| long | mappedBinaryReader | 113.68M | <0.001 |
| double | mappedBinaryReader | 113.41M | <0.001 |
| int | byteBufferHeap | 125.56M | <0.001 |
| long | byteBufferHeap | 115.67M | <0.001 |
| double | byteBufferHeap | 109.41M | <0.001 |
| int | byteBufferDirect | 162.38M | <0.001 |
| long | byteBufferDirect | 155.75M | <0.001 |
| double | byteBufferDirect | 152.20M | <0.001 |

## Sequential Scan

Records: 8192 fixed-size records containing `int`, `long`, and `double`.

| Implementation | Score ops/s | Error | Alloc B/op |
|---|---:|---:|---:|
| binaryReader | 45.57K | 2.43K | 112.751 |
| mappedBinaryReader | 69.46K | 7.19K | 0.494 |
| byteBufferHeap | 83.98K | 8.27K | 56.408 |
| byteBufferDirect | 122.99K | 13.01K | 64.281 |
| dataInputStream | 23.59K | 1.77K | 57.455 |

## Limitations

- This is one local machine/JVM run, not a cross-platform result.
- Benchmarks used JDK 21 while the project compiles to Java 17 bytecode.
- Primitive write benchmarks measure one writer/buffer creation per operation, so they measure small-message construction rather than a reused writer.
- Direct `ByteBuffer` write benchmarks allocate a direct buffer per operation, which is intentionally shown but is not representative of pooled direct buffers.
- File IO noise is minimized by keeping most benchmarks in memory; mmap benchmarks use temporary files to exercise `MappedBinaryReader`.
- The benchmark jar built successfully, but this workspace needed an isolated classpath run because generated JMH classes under `target/classes` were inconsistent during jar execution.
