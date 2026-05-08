package io.github.arthurhoch.kissbinary.benchmarks;

import io.github.arthurhoch.kissbinary.BinaryReader;
import io.github.arthurhoch.kissbinary.BinaryWriter;
import io.github.arthurhoch.kissbinary.Endianness;
import io.github.arthurhoch.kissbinary.MappedBinaryReader;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class KissBinaryBenchmark {

    private static final Endianness ENDIANNESS = Endianness.BIG_ENDIAN;
    private static final ByteOrder BYTE_ORDER = ByteOrder.BIG_ENDIAN;
    private static final int ARRAY_LENGTH = 1024;
    private static final int SHORT_VECTOR_LENGTH = 16;
    private static final int RECORD_COUNT = 8192;
    private static final int RECORD_SIZE = Integer.BYTES + Long.BYTES + Double.BYTES;

    @Benchmark
    public void primitiveWriteInt(PrimitiveWriteState state, Blackhole blackhole) throws IOException {
        writeSinglePrimitive(state.impl, state.intValue, 0L, 0.0d, PrimitiveKind.INT, blackhole);
    }

    @Benchmark
    public void primitiveWriteLong(PrimitiveWriteState state, Blackhole blackhole) throws IOException {
        writeSinglePrimitive(state.impl, 0, state.longValue, 0.0d, PrimitiveKind.LONG, blackhole);
    }

    @Benchmark
    public void primitiveWriteDouble(PrimitiveWriteState state, Blackhole blackhole) throws IOException {
        writeSinglePrimitive(state.impl, 0, 0L, state.doubleValue, PrimitiveKind.DOUBLE, blackhole);
    }

    @Benchmark
    public void primitiveWriteMixedRecord(PrimitiveWriteState state, Blackhole blackhole) throws IOException {
        switch (state.impl) {
            case "kiss" -> {
                BinaryWriter writer = BinaryWriter.create(ENDIANNESS);
                writer.writeInt(state.intValue);
                writer.writeLong(state.longValue);
                writer.writeDouble(state.doubleValue);
                blackhole.consume(writer.toByteArray());
            }
            case "dataOutputStream" -> {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream(RECORD_SIZE);
                DataOutputStream out = new DataOutputStream(bytes);
                out.writeInt(state.intValue);
                out.writeLong(state.longValue);
                out.writeDouble(state.doubleValue);
                blackhole.consume(bytes.toByteArray());
            }
            case "byteBufferHeap" -> {
                ByteBuffer buffer = ByteBuffer.allocate(RECORD_SIZE).order(BYTE_ORDER);
                buffer.putInt(state.intValue);
                buffer.putLong(state.longValue);
                buffer.putDouble(state.doubleValue);
                blackhole.consume(buffer.array());
            }
            case "byteBufferDirect" -> {
                ByteBuffer buffer = ByteBuffer.allocateDirect(RECORD_SIZE).order(BYTE_ORDER);
                buffer.putInt(state.intValue);
                buffer.putLong(state.longValue);
                buffer.putDouble(state.doubleValue);
                blackhole.consume(buffer);
            }
            default -> throw new IllegalStateException("Unknown implementation: " + state.impl);
        }
    }

    @Benchmark
    public int primitiveReadInt(PrimitiveReadState state) throws IOException {
        return switch (state.impl) {
            case "kiss" -> BinaryReader.from(state.intData, ENDIANNESS).readInt();
            case "dataInputStream" -> new DataInputStream(new ByteArrayInputStream(state.intData)).readInt();
            case "byteBufferHeap" -> ByteBuffer.wrap(state.intData).order(BYTE_ORDER).getInt();
            case "byteBufferDirect" -> state.directIntData.duplicate().order(BYTE_ORDER).getInt();
            default -> throw new IllegalStateException("Unknown implementation: " + state.impl);
        };
    }

    @Benchmark
    public long primitiveReadLong(PrimitiveReadState state) throws IOException {
        return switch (state.impl) {
            case "kiss" -> BinaryReader.from(state.longData, ENDIANNESS).readLong();
            case "dataInputStream" -> new DataInputStream(new ByteArrayInputStream(state.longData)).readLong();
            case "byteBufferHeap" -> ByteBuffer.wrap(state.longData).order(BYTE_ORDER).getLong();
            case "byteBufferDirect" -> state.directLongData.duplicate().order(BYTE_ORDER).getLong();
            default -> throw new IllegalStateException("Unknown implementation: " + state.impl);
        };
    }

    @Benchmark
    public double primitiveReadDouble(PrimitiveReadState state) throws IOException {
        return switch (state.impl) {
            case "kiss" -> BinaryReader.from(state.doubleData, ENDIANNESS).readDouble();
            case "dataInputStream" -> new DataInputStream(new ByteArrayInputStream(state.doubleData)).readDouble();
            case "byteBufferHeap" -> ByteBuffer.wrap(state.doubleData).order(BYTE_ORDER).getDouble();
            case "byteBufferDirect" -> state.directDoubleData.duplicate().order(BYTE_ORDER).getDouble();
            default -> throw new IllegalStateException("Unknown implementation: " + state.impl);
        };
    }

    @Benchmark
    public long primitiveReadMixedRecord(PrimitiveReadState state) throws IOException {
        return switch (state.impl) {
            case "kiss" -> {
                BinaryReader reader = BinaryReader.from(state.mixedData, ENDIANNESS);
                yield checksum(reader.readInt(), reader.readLong(), reader.readDouble());
            }
            case "dataInputStream" -> {
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(state.mixedData));
                yield checksum(in.readInt(), in.readLong(), in.readDouble());
            }
            case "byteBufferHeap" -> {
                ByteBuffer buffer = ByteBuffer.wrap(state.mixedData).order(BYTE_ORDER);
                yield checksum(buffer.getInt(), buffer.getLong(), buffer.getDouble());
            }
            case "byteBufferDirect" -> {
                ByteBuffer buffer = state.directMixedData.duplicate().order(BYTE_ORDER);
                yield checksum(buffer.getInt(), buffer.getLong(), buffer.getDouble());
            }
            default -> throw new IllegalStateException("Unknown implementation: " + state.impl);
        };
    }

    @Benchmark
    public void arrayWrite(ArrayWriteState state, Blackhole blackhole) {
        switch (state.impl) {
            case "kiss" -> writeArrayWithKissBinary(state.arrayType, state, blackhole);
            case "byteBufferHeap" -> writeArrayWithByteBuffer(state.arrayType, false, state, blackhole);
            case "byteBufferDirect" -> writeArrayWithByteBuffer(state.arrayType, true, state, blackhole);
            default -> throw new IllegalStateException("Unknown implementation: " + state.impl);
        }
    }

    @Benchmark
    public void arrayRead(ArrayReadState state, Blackhole blackhole) {
        switch (state.impl) {
            case "kiss" -> readArrayWithKissBinary(state.arrayType, state, blackhole);
            case "byteBufferHeap" -> readArrayWithByteBuffer(state.arrayType, false, state, blackhole);
            case "byteBufferDirect" -> readArrayWithByteBuffer(state.arrayType, true, state, blackhole);
            default -> throw new IllegalStateException("Unknown implementation: " + state.impl);
        }
    }

    @Benchmark
    public long binaryReaderReadShortArrayReturning(ShortArrayReadState state) {
        short[] values = BinaryReader.from(state.shortData, ENDIANNESS)
                .readShortArray(SHORT_VECTOR_LENGTH);
        return sum(values, SHORT_VECTOR_LENGTH);
    }

    @Benchmark
    public long binaryReaderReadShortArrayReused(ShortArrayReadState state) {
        BinaryReader reader = BinaryReader.from(state.shortData, ENDIANNESS);
        reader.readShortArray(state.shortTarget);
        return sum(state.shortTarget, SHORT_VECTOR_LENGTH);
    }

    @Benchmark
    public long mappedBinaryReaderReadShortArrayReturning(ShortArrayReadState state) {
        short[] values = state.mappedReader.readShortArray(0, SHORT_VECTOR_LENGTH);
        return sum(values, SHORT_VECTOR_LENGTH);
    }

    @Benchmark
    public long mappedBinaryReaderReadShortArrayReused(ShortArrayReadState state) {
        state.mappedReader.readShortArray(0, state.shortTarget);
        return sum(state.shortTarget, SHORT_VECTOR_LENGTH);
    }

    @Benchmark
    public void headerValidateMagic(HeaderState state, Blackhole blackhole) {
        switch (state.impl) {
            case "kiss" -> {
                BinaryReader reader = BinaryReader.from(state.magicData, ENDIANNESS);
                reader.expectMagic("KB");
                blackhole.consume(reader.position());
            }
            case "byteBufferHeap" -> {
                ByteBuffer buffer = ByteBuffer.wrap(state.magicData).order(BYTE_ORDER);
                boolean ok = buffer.get() == 'K' && buffer.get() == 'B';
                blackhole.consume(ok);
            }
            default -> throw new IllegalStateException("Unknown implementation: " + state.impl);
        }
    }

    @Benchmark
    public void headerValidateVersion(HeaderState state, Blackhole blackhole) {
        switch (state.impl) {
            case "kiss" -> {
                BinaryReader reader = BinaryReader.from(state.versionData, ENDIANNESS);
                reader.expectVersion(1);
                blackhole.consume(reader.position());
            }
            case "byteBufferHeap" -> {
                ByteBuffer buffer = ByteBuffer.wrap(state.versionData).order(BYTE_ORDER);
                blackhole.consume(buffer.getInt() == 1);
            }
            default -> throw new IllegalStateException("Unknown implementation: " + state.impl);
        }
    }

    @Benchmark
    public long randomOffsetRead(MappedState state) {
        int record = state.nextRecord();
        int offset = record * RECORD_SIZE;
        return switch (state.impl) {
            case "mappedBinaryReader" -> switch (state.primitive) {
                case "int" -> state.mappedReader.readInt(offset);
                case "long" -> state.mappedReader.readLong(offset + Integer.BYTES);
                case "double" -> Double.doubleToRawLongBits(
                        state.mappedReader.readDouble(offset + Integer.BYTES + Long.BYTES));
                default -> throw new IllegalStateException("Unknown primitive: " + state.primitive);
            };
            case "byteBufferHeap" -> switch (state.primitive) {
                case "int" -> state.heapBuffer.getInt(offset);
                case "long" -> state.heapBuffer.getLong(offset + Integer.BYTES);
                case "double" -> Double.doubleToRawLongBits(
                        state.heapBuffer.getDouble(offset + Integer.BYTES + Long.BYTES));
                default -> throw new IllegalStateException("Unknown primitive: " + state.primitive);
            };
            case "byteBufferDirect" -> switch (state.primitive) {
                case "int" -> state.directBuffer.getInt(offset);
                case "long" -> state.directBuffer.getLong(offset + Integer.BYTES);
                case "double" -> Double.doubleToRawLongBits(
                        state.directBuffer.getDouble(offset + Integer.BYTES + Long.BYTES));
                default -> throw new IllegalStateException("Unknown primitive: " + state.primitive);
            };
            default -> throw new IllegalStateException("Unknown implementation: " + state.impl);
        };
    }

    @Benchmark
    public long sequentialScan(SequentialState state) throws IOException {
        return switch (state.impl) {
            case "binaryReader" -> {
                BinaryReader reader = BinaryReader.from(state.data, ENDIANNESS);
                long checksum = 0L;
                for (int i = 0; i < RECORD_COUNT; i++) {
                    checksum += checksum(reader.readInt(), reader.readLong(), reader.readDouble());
                }
                yield checksum;
            }
            case "mappedBinaryReader" -> {
                long checksum = 0L;
                for (int i = 0; i < RECORD_COUNT; i++) {
                    int offset = i * RECORD_SIZE;
                    checksum += checksum(state.mappedReader.readInt(offset),
                            state.mappedReader.readLong(offset + Integer.BYTES),
                            state.mappedReader.readDouble(offset + Integer.BYTES + Long.BYTES));
                }
                yield checksum;
            }
            case "byteBufferHeap" -> {
                ByteBuffer buffer = ByteBuffer.wrap(state.data).order(BYTE_ORDER);
                long checksum = 0L;
                for (int i = 0; i < RECORD_COUNT; i++) {
                    checksum += checksum(buffer.getInt(), buffer.getLong(), buffer.getDouble());
                }
                yield checksum;
            }
            case "byteBufferDirect" -> {
                ByteBuffer buffer = state.directBuffer.duplicate().order(BYTE_ORDER);
                long checksum = 0L;
                for (int i = 0; i < RECORD_COUNT; i++) {
                    checksum += checksum(buffer.getInt(), buffer.getLong(), buffer.getDouble());
                }
                yield checksum;
            }
            case "dataInputStream" -> {
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(state.data));
                long checksum = 0L;
                for (int i = 0; i < RECORD_COUNT; i++) {
                    checksum += checksum(in.readInt(), in.readLong(), in.readDouble());
                }
                yield checksum;
            }
            default -> throw new IllegalStateException("Unknown implementation: " + state.impl);
        };
    }

    private static void writeSinglePrimitive(String impl, int intValue, long longValue,
                                             double doubleValue, PrimitiveKind kind,
                                             Blackhole blackhole) throws IOException {
        switch (impl) {
            case "kiss" -> {
                BinaryWriter writer = BinaryWriter.create(ENDIANNESS);
                switch (kind) {
                    case INT -> writer.writeInt(intValue);
                    case LONG -> writer.writeLong(longValue);
                    case DOUBLE -> writer.writeDouble(doubleValue);
                }
                blackhole.consume(writer.toByteArray());
            }
            case "dataOutputStream" -> {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream(kind.bytes);
                DataOutputStream out = new DataOutputStream(bytes);
                switch (kind) {
                    case INT -> out.writeInt(intValue);
                    case LONG -> out.writeLong(longValue);
                    case DOUBLE -> out.writeDouble(doubleValue);
                }
                blackhole.consume(bytes.toByteArray());
            }
            case "byteBufferHeap" -> {
                ByteBuffer buffer = ByteBuffer.allocate(kind.bytes).order(BYTE_ORDER);
                putPrimitive(buffer, intValue, longValue, doubleValue, kind);
                blackhole.consume(buffer.array());
            }
            case "byteBufferDirect" -> {
                ByteBuffer buffer = ByteBuffer.allocateDirect(kind.bytes).order(BYTE_ORDER);
                putPrimitive(buffer, intValue, longValue, doubleValue, kind);
                blackhole.consume(buffer);
            }
            default -> throw new IllegalStateException("Unknown implementation: " + impl);
        }
    }

    private static void putPrimitive(ByteBuffer buffer, int intValue, long longValue,
                                     double doubleValue, PrimitiveKind kind) {
        switch (kind) {
            case INT -> buffer.putInt(intValue);
            case LONG -> buffer.putLong(longValue);
            case DOUBLE -> buffer.putDouble(doubleValue);
        }
    }

    private static void writeArrayWithKissBinary(String arrayType, ArrayWriteState state,
                                                 Blackhole blackhole) {
        BinaryWriter writer = BinaryWriter.create(ENDIANNESS);
        switch (arrayType) {
            case "short" -> writer.writeShortArray(state.shorts);
            case "int" -> writer.writeIntArray(state.ints);
            case "long" -> writer.writeLongArray(state.longs);
            case "float" -> writer.writeFloatArray(state.floats);
            case "double" -> writer.writeDoubleArray(state.doubles);
            default -> throw new IllegalStateException("Unknown array type: " + arrayType);
        }
        blackhole.consume(writer.toByteArray());
    }

    private static void writeArrayWithByteBuffer(String arrayType, boolean direct,
                                                 ArrayWriteState state, Blackhole blackhole) {
        int bytes = arrayByteCount(arrayType);
        ByteBuffer buffer = direct
                ? ByteBuffer.allocateDirect(bytes).order(BYTE_ORDER)
                : ByteBuffer.allocate(bytes).order(BYTE_ORDER);
        switch (arrayType) {
            case "short" -> {
                var view = buffer.asShortBuffer();
                view.put(state.shorts);
                blackhole.consume(view.position());
            }
            case "int" -> {
                var view = buffer.asIntBuffer();
                view.put(state.ints);
                blackhole.consume(view.position());
            }
            case "long" -> {
                var view = buffer.asLongBuffer();
                view.put(state.longs);
                blackhole.consume(view.position());
            }
            case "float" -> {
                var view = buffer.asFloatBuffer();
                view.put(state.floats);
                blackhole.consume(view.position());
            }
            case "double" -> {
                var view = buffer.asDoubleBuffer();
                view.put(state.doubles);
                blackhole.consume(view.position());
            }
            default -> throw new IllegalStateException("Unknown array type: " + arrayType);
        }
        blackhole.consume(buffer);
    }

    private static void readArrayWithKissBinary(String arrayType, ArrayReadState state,
                                                Blackhole blackhole) {
        switch (arrayType) {
            case "short" -> blackhole.consume(BinaryReader.from(state.shortData, ENDIANNESS)
                    .readShortArray(ARRAY_LENGTH));
            case "int" -> blackhole.consume(BinaryReader.from(state.intData, ENDIANNESS)
                    .readIntArray(ARRAY_LENGTH));
            case "long" -> blackhole.consume(BinaryReader.from(state.longData, ENDIANNESS)
                    .readLongArray(ARRAY_LENGTH));
            case "float" -> blackhole.consume(BinaryReader.from(state.floatData, ENDIANNESS)
                    .readFloatArray(ARRAY_LENGTH));
            case "double" -> blackhole.consume(BinaryReader.from(state.doubleData, ENDIANNESS)
                    .readDoubleArray(ARRAY_LENGTH));
            default -> throw new IllegalStateException("Unknown array type: " + arrayType);
        }
    }

    private static void readArrayWithByteBuffer(String arrayType, boolean direct,
                                                ArrayReadState state, Blackhole blackhole) {
        ByteBuffer buffer = direct ? state.directData(arrayType).duplicate() : state.heapData(arrayType);
        buffer.order(BYTE_ORDER);
        switch (arrayType) {
            case "short" -> {
                short[] result = new short[ARRAY_LENGTH];
                buffer.asShortBuffer().get(result);
                blackhole.consume(result);
            }
            case "int" -> {
                int[] result = new int[ARRAY_LENGTH];
                buffer.asIntBuffer().get(result);
                blackhole.consume(result);
            }
            case "long" -> {
                long[] result = new long[ARRAY_LENGTH];
                buffer.asLongBuffer().get(result);
                blackhole.consume(result);
            }
            case "float" -> {
                float[] result = new float[ARRAY_LENGTH];
                buffer.asFloatBuffer().get(result);
                blackhole.consume(result);
            }
            case "double" -> {
                double[] result = new double[ARRAY_LENGTH];
                buffer.asDoubleBuffer().get(result);
                blackhole.consume(result);
            }
            default -> throw new IllegalStateException("Unknown array type: " + arrayType);
        }
    }

    private static int arrayByteCount(String arrayType) {
        return switch (arrayType) {
            case "short" -> ARRAY_LENGTH * Short.BYTES;
            case "int", "float" -> ARRAY_LENGTH * Integer.BYTES;
            case "long", "double" -> ARRAY_LENGTH * Long.BYTES;
            default -> throw new IllegalStateException("Unknown array type: " + arrayType);
        };
    }

    private static long checksum(int intValue, long longValue, double doubleValue) {
        return intValue * 31L + longValue * 17L + Double.doubleToRawLongBits(doubleValue);
    }

    private static long sum(short[] values, int length) {
        long total = 0L;
        for (int i = 0; i < length; i++) {
            total += values[i];
        }
        return total;
    }

    private static byte[] recordData() {
        BinaryWriter writer = BinaryWriter.create(ENDIANNESS);
        for (int i = 0; i < RECORD_COUNT; i++) {
            writer.writeInt(i);
            writer.writeLong(i * 1_000_000_003L);
            writer.writeDouble(i + 0.25d);
        }
        return writer.toByteArray();
    }

    private static ByteBuffer directCopy(byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(data.length).order(BYTE_ORDER);
        buffer.put(data);
        buffer.flip();
        return buffer;
    }

    private static ByteBuffer heapCopy(byte[] data) {
        return ByteBuffer.wrap(data).order(BYTE_ORDER);
    }

    private enum PrimitiveKind {
        INT(Integer.BYTES),
        LONG(Long.BYTES),
        DOUBLE(Double.BYTES);

        private final int bytes;

        PrimitiveKind(int bytes) {
            this.bytes = bytes;
        }
    }

    @State(Scope.Thread)
    public static class PrimitiveWriteState {
        @Param({"kiss", "dataOutputStream", "byteBufferHeap", "byteBufferDirect"})
        public String impl;

        public int intValue = 0x12345678;
        public long longValue = 0x123456789ABCDEFL;
        public double doubleValue = 12345.6789d;
    }

    @State(Scope.Thread)
    public static class PrimitiveReadState {
        @Param({"kiss", "dataInputStream", "byteBufferHeap", "byteBufferDirect"})
        public String impl;

        public byte[] intData;
        public byte[] longData;
        public byte[] doubleData;
        public byte[] mixedData;
        public ByteBuffer directIntData;
        public ByteBuffer directLongData;
        public ByteBuffer directDoubleData;
        public ByteBuffer directMixedData;

        @Setup(Level.Trial)
        public void setup() {
            BinaryWriter intWriter = BinaryWriter.create(ENDIANNESS);
            intWriter.writeInt(0x12345678);
            intData = intWriter.toByteArray();

            BinaryWriter longWriter = BinaryWriter.create(ENDIANNESS);
            longWriter.writeLong(0x123456789ABCDEFL);
            longData = longWriter.toByteArray();

            BinaryWriter doubleWriter = BinaryWriter.create(ENDIANNESS);
            doubleWriter.writeDouble(12345.6789d);
            doubleData = doubleWriter.toByteArray();

            BinaryWriter mixedWriter = BinaryWriter.create(ENDIANNESS);
            mixedWriter.writeInt(0x12345678);
            mixedWriter.writeLong(0x123456789ABCDEFL);
            mixedWriter.writeDouble(12345.6789d);
            mixedData = mixedWriter.toByteArray();

            directIntData = directCopy(intData);
            directLongData = directCopy(longData);
            directDoubleData = directCopy(doubleData);
            directMixedData = directCopy(mixedData);
        }
    }

    @State(Scope.Thread)
    public static class ArrayWriteState {
        @Param({"kiss", "byteBufferHeap", "byteBufferDirect"})
        public String impl;

        @Param({"short", "int", "long", "float", "double"})
        public String arrayType;

        public short[] shorts;
        public int[] ints;
        public long[] longs;
        public float[] floats;
        public double[] doubles;

        @Setup(Level.Trial)
        public void setup() {
            shorts = new short[ARRAY_LENGTH];
            ints = new int[ARRAY_LENGTH];
            longs = new long[ARRAY_LENGTH];
            floats = new float[ARRAY_LENGTH];
            doubles = new double[ARRAY_LENGTH];
            for (int i = 0; i < ARRAY_LENGTH; i++) {
                shorts[i] = (short) i;
                ints[i] = i * 31;
                longs[i] = i * 1_000_000_003L;
                floats[i] = i + 0.5f;
                doubles[i] = i + 0.25d;
            }
        }
    }

    @State(Scope.Thread)
    public static class ArrayReadState extends ArrayWriteState {
        public byte[] shortData;
        public byte[] intData;
        public byte[] longData;
        public byte[] floatData;
        public byte[] doubleData;
        public ByteBuffer directShortData;
        public ByteBuffer directIntData;
        public ByteBuffer directLongData;
        public ByteBuffer directFloatData;
        public ByteBuffer directDoubleData;

        @Override
        @Setup(Level.Trial)
        public void setup() {
            super.setup();
            BinaryWriter writer = BinaryWriter.create(ENDIANNESS);
            writer.writeShortArray(shorts);
            shortData = writer.toByteArray();

            writer = BinaryWriter.create(ENDIANNESS);
            writer.writeIntArray(ints);
            intData = writer.toByteArray();

            writer = BinaryWriter.create(ENDIANNESS);
            writer.writeLongArray(longs);
            longData = writer.toByteArray();

            writer = BinaryWriter.create(ENDIANNESS);
            writer.writeFloatArray(floats);
            floatData = writer.toByteArray();

            writer = BinaryWriter.create(ENDIANNESS);
            writer.writeDoubleArray(doubles);
            doubleData = writer.toByteArray();

            directShortData = directCopy(shortData);
            directIntData = directCopy(intData);
            directLongData = directCopy(longData);
            directFloatData = directCopy(floatData);
            directDoubleData = directCopy(doubleData);
        }

        ByteBuffer heapData(String arrayType) {
            return switch (arrayType) {
                case "short" -> heapCopy(shortData);
                case "int" -> heapCopy(intData);
                case "long" -> heapCopy(longData);
                case "float" -> heapCopy(floatData);
                case "double" -> heapCopy(doubleData);
                default -> throw new IllegalStateException("Unknown array type: " + arrayType);
            };
        }

        ByteBuffer directData(String arrayType) {
            return switch (arrayType) {
                case "short" -> directShortData;
                case "int" -> directIntData;
                case "long" -> directLongData;
                case "float" -> directFloatData;
                case "double" -> directDoubleData;
                default -> throw new IllegalStateException("Unknown array type: " + arrayType);
            };
        }
    }

    @State(Scope.Thread)
    public static class ShortArrayReadState {
        public short[] shortTarget;
        public byte[] shortData;
        public MappedBinaryReader mappedReader;
        public Path tempFile;

        @Setup(Level.Trial)
        public void setup() throws IOException {
            short[] values = new short[SHORT_VECTOR_LENGTH];
            for (int i = 0; i < values.length; i++) {
                values[i] = (short) (i * 13);
            }
            shortTarget = new short[SHORT_VECTOR_LENGTH];

            BinaryWriter writer = BinaryWriter.create(ENDIANNESS);
            writer.writeShortArray(values);
            shortData = writer.toByteArray();

            tempFile = Files.createTempFile("kiss-binary-short-array-jmh-", ".bin");
            Files.write(tempFile, shortData);
            mappedReader = MappedBinaryReader.from(tempFile, ENDIANNESS);
        }

        @TearDown(Level.Trial)
        public void tearDown() throws IOException {
            if (mappedReader != null) {
                mappedReader.close();
            }
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    @State(Scope.Thread)
    public static class HeaderState {
        @Param({"kiss", "byteBufferHeap"})
        public String impl;

        public byte[] magicData;
        public byte[] versionData;

        @Setup(Level.Trial)
        public void setup() {
            BinaryWriter writer = BinaryWriter.create(ENDIANNESS);
            writer.writeMagic("KB");
            magicData = writer.toByteArray();

            writer = BinaryWriter.create(ENDIANNESS);
            writer.writeVersion(1);
            versionData = writer.toByteArray();
        }
    }

    @State(Scope.Thread)
    public static class MappedState {
        @Param({"mappedBinaryReader", "byteBufferHeap", "byteBufferDirect"})
        public String impl;

        @Param({"int", "long", "double"})
        public String primitive;

        public byte[] data;
        public ByteBuffer heapBuffer;
        public ByteBuffer directBuffer;
        public MappedBinaryReader mappedReader;
        public Path tempFile;
        public int[] randomRecords;
        public int cursor;

        @Setup(Level.Trial)
        public void setup() throws IOException {
            data = recordData();
            heapBuffer = heapCopy(data);
            directBuffer = directCopy(data);
            tempFile = Files.createTempFile("kiss-binary-jmh-", ".bin");
            Files.write(tempFile, data);
            mappedReader = MappedBinaryReader.from(tempFile, ENDIANNESS);
            randomRecords = new int[RECORD_COUNT];
            int value = 0;
            for (int i = 0; i < randomRecords.length; i++) {
                value = (value + 4093) & (RECORD_COUNT - 1);
                randomRecords[i] = value;
            }
        }

        public int nextRecord() {
            int value = randomRecords[cursor];
            cursor++;
            if (cursor == randomRecords.length) {
                cursor = 0;
            }
            return value;
        }

        @TearDown(Level.Trial)
        public void tearDown() throws IOException {
            if (mappedReader != null) {
                mappedReader.close();
            }
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    @State(Scope.Thread)
    public static class SequentialState {
        @Param({"binaryReader", "mappedBinaryReader", "byteBufferHeap", "byteBufferDirect", "dataInputStream"})
        public String impl;

        public byte[] data;
        public ByteBuffer directBuffer;
        public MappedBinaryReader mappedReader;
        public Path tempFile;

        @Setup(Level.Trial)
        public void setup() throws IOException {
            data = recordData();
            directBuffer = directCopy(data);
            tempFile = Files.createTempFile("kiss-binary-jmh-seq-", ".bin");
            Files.write(tempFile, data);
            mappedReader = MappedBinaryReader.from(tempFile, ENDIANNESS);
        }

        @TearDown(Level.Trial)
        public void tearDown() throws IOException {
            if (mappedReader != null) {
                mappedReader.close();
            }
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }
}
