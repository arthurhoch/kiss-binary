package io.github.arthurhoch.kissbinary.benchmarks.rinha;

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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class RinhaBinaryBenchmark {

    private static final Endianness ENDIANNESS = Endianness.LITTLE_ENDIAN;
    private static final ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
    private static final int LOGICAL_DIMS = 14;
    private static final int PHYSICAL_DIMS = 16;
    private static final int HEADER_SIZE = 32;
    private static final int DEFAULT_LABEL_WORD_CAPACITY = (10_000 + 63) / 64;

    @State(Scope.Benchmark)
    public static class KbinState {
        @Param({"10000"})
        public int vectorCount;

        public byte[] data;
        public Path kbinFile;
        public MappedBinaryReader mmapReader;
        public int labelWordCount;
        public long labelOffset;
        public int[] randomIndices;
        public ByteBuffer heapBuffer;

        @Setup(Level.Trial)
        public void setup() throws Exception {
            Path dir = Files.createTempDirectory("rinha-jmh-");
            kbinFile = dir.resolve("bench.kbin");

            BinaryWriter writer = BinaryWriter.create(ENDIANNESS);
            writer.writeMagic("KBRN");
            writer.writeVersion(1);
            writer.writeInt(LOGICAL_DIMS);
            writer.writeInt(PHYSICAL_DIMS);
            writer.writeInt(vectorCount);
            labelWordCount = (vectorCount + 63) / 64;
            writer.writeInt(labelWordCount);
            writer.writeInt(0);
            writer.writeInt(0);

            Random rng = new Random(42);
            for (int i = 0; i < vectorCount; i++) {
                for (int d = 0; d < LOGICAL_DIMS; d++) {
                    writer.writeShort((short) rng.nextInt(Short.MAX_VALUE));
                }
                for (int d = LOGICAL_DIMS; d < PHYSICAL_DIMS; d++) {
                    writer.writeShort((short) 0);
                }
            }

            long[] labels = new long[labelWordCount];
            for (int i = 0; i < vectorCount; i++) {
                if (rng.nextDouble() < 0.05) {
                    labels[i / 64] |= (1L << (i % 64));
                }
            }
            for (long word : labels) {
                writer.writeLong(word);
            }

            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(kbinFile.toFile()))) {
                writer.writeTo(out);
            }

            data = Files.readAllBytes(kbinFile);
            labelOffset = HEADER_SIZE + (long) vectorCount * PHYSICAL_DIMS * Short.BYTES;
            heapBuffer = ByteBuffer.wrap(data).order(BYTE_ORDER);
            mmapReader = MappedBinaryReader.from(kbinFile, ENDIANNESS);

            randomIndices = new int[Math.min(vectorCount, 10000)];
            int val = 0;
            for (int i = 0; i < randomIndices.length; i++) {
                val = (val + 4093) % vectorCount;
                randomIndices[i] = val;
            }
        }

        @TearDown(Level.Trial)
        public void tearDown() throws Exception {
            if (mmapReader != null) mmapReader.close();
            if (kbinFile != null) {
                Files.deleteIfExists(kbinFile);
                Files.deleteIfExists(kbinFile.getParent());
            }
        }
    }

    @State(Scope.Thread)
    public static class ThreadState {
        public int cursor;
        public final short[] vector = new short[PHYSICAL_DIMS];
        public long[] labels = new long[DEFAULT_LABEL_WORD_CAPACITY];

        public long[] labelTarget(int wordCount) {
            if (labels.length < wordCount) {
                labels = new long[wordCount];
            }
            return labels;
        }
    }

    @Benchmark
    public long sequentialRead_BinaryReader(KbinState state) {
        BinaryReader reader = BinaryReader.from(state.data, ENDIANNESS);
        reader.readByteArray(HEADER_SIZE);
        long checksum = 0;
        for (int i = 0; i < state.vectorCount; i++) {
            short[] vec = reader.readShortArray(PHYSICAL_DIMS);
            for (short s : vec) checksum += s;
        }
        return checksum;
    }

    @Benchmark
    public long sequentialRead_MappedBinaryReader(KbinState state) {
        long checksum = 0;
        for (int i = 0; i < state.vectorCount; i++) {
            long offset = HEADER_SIZE + (long) i * PHYSICAL_DIMS * Short.BYTES;
            short[] vec = state.mmapReader.readShortArray(offset, PHYSICAL_DIMS);
            for (short s : vec) checksum += s;
        }
        return checksum;
    }

    @Benchmark
    public long sequentialRead_MappedBinaryReader_reusedArray(KbinState state, ThreadState ts) {
        long checksum = 0;
        short[] vec = ts.vector;
        for (int i = 0; i < state.vectorCount; i++) {
            long offset = HEADER_SIZE + (long) i * PHYSICAL_DIMS * Short.BYTES;
            state.mmapReader.readShortArray(offset, vec);
            for (short s : vec) checksum += s;
        }
        return checksum;
    }

    @Benchmark
    public long sequentialRead_HeapByteBuffer(KbinState state) {
        ByteBuffer buf = state.heapBuffer.duplicate().order(BYTE_ORDER);
        buf.position(HEADER_SIZE);
        long checksum = 0;
        short[] temp = new short[PHYSICAL_DIMS];
        for (int i = 0; i < state.vectorCount; i++) {
            buf.asShortBuffer().get(temp);
            buf.position(buf.position() + PHYSICAL_DIMS * Short.BYTES);
            for (short s : temp) checksum += s;
        }
        return checksum;
    }

    @Benchmark
    public long randomAccess_MappedBinaryReader(KbinState state, ThreadState ts) {
        int idx = state.randomIndices[ts.cursor];
        ts.cursor++;
        if (ts.cursor >= state.randomIndices.length) ts.cursor = 0;
        long offset = HEADER_SIZE + (long) idx * PHYSICAL_DIMS * Short.BYTES;
        short[] vec = state.mmapReader.readShortArray(offset, PHYSICAL_DIMS);
        long checksum = 0;
        for (short s : vec) checksum += s;
        return checksum;
    }

    @Benchmark
    public long randomAccess_MappedBinaryReader_reusedArray(KbinState state, ThreadState ts) {
        int idx = state.randomIndices[ts.cursor];
        ts.cursor++;
        if (ts.cursor >= state.randomIndices.length) ts.cursor = 0;
        long offset = HEADER_SIZE + (long) idx * PHYSICAL_DIMS * Short.BYTES;
        short[] vec = ts.vector;
        state.mmapReader.readShortArray(offset, vec);
        long checksum = 0;
        for (short s : vec) checksum += s;
        return checksum;
    }

    @Benchmark
    public long randomAccess_HeapByteBuffer(KbinState state, ThreadState ts) {
        int idx = state.randomIndices[ts.cursor];
        ts.cursor++;
        if (ts.cursor >= state.randomIndices.length) ts.cursor = 0;
        int offset = HEADER_SIZE + idx * PHYSICAL_DIMS * Short.BYTES;
        long checksum = 0;
        ByteBuffer buf = state.heapBuffer.duplicate().order(BYTE_ORDER);
        for (int d = 0; d < PHYSICAL_DIMS; d++) {
            checksum += buf.getShort(offset + d * Short.BYTES);
        }
        return checksum;
    }

    @Benchmark
    public int headerValidation_KissBinary(KbinState state, Blackhole bh) {
        BinaryReader reader = BinaryReader.from(state.data, ENDIANNESS);
        reader.expectMagic("KBRN");
        reader.expectVersion(1);
        bh.consume(reader.readInt());
        return reader.position();
    }

    @Benchmark
    public int headerValidation_ByteBuffer(KbinState state, Blackhole bh) {
        ByteBuffer buf = ByteBuffer.wrap(state.data).order(BYTE_ORDER);
        boolean magicOk = buf.get() == 'K' && buf.get() == 'B' && buf.get() == 'R' && buf.get() == 'N';
        boolean versionOk = buf.getInt() == 1;
        bh.consume(magicOk && versionOk);
        return buf.getInt();
    }

    @Benchmark
    public int labelBitsetScan_KissBinary(KbinState state) {
        BinaryReader reader = BinaryReader.from(state.data, ENDIANNESS);
        reader.readByteArray((int) state.labelOffset);
        long[] labels = reader.readLongArray(state.labelWordCount);
        int count = 0;
        for (long word : labels) {
            count += Long.bitCount(word);
        }
        return count;
    }

    @Benchmark
    public int labelBitsetScan_KissBinary_skipFullyReusedArray(KbinState state, ThreadState ts) {
        BinaryReader reader = BinaryReader.from(state.data, ENDIANNESS);
        reader.skipFully(state.labelOffset);
        long[] labels = ts.labelTarget(state.labelWordCount);
        reader.readLongArray(labels, 0, state.labelWordCount);
        int count = 0;
        for (int i = 0; i < state.labelWordCount; i++) {
            count += Long.bitCount(labels[i]);
        }
        return count;
    }

    @Benchmark
    public int labelBitsetScan_MappedBinaryReader_reusedArray(KbinState state, ThreadState ts) {
        long[] labels = ts.labelTarget(state.labelWordCount);
        state.mmapReader.readLongArray(state.labelOffset, labels, 0, state.labelWordCount);
        int count = 0;
        for (int i = 0; i < state.labelWordCount; i++) {
            count += Long.bitCount(labels[i]);
        }
        return count;
    }

    @Benchmark
    public int labelBitsetScan_ByteBuffer(KbinState state) {
        ByteBuffer buf = state.heapBuffer.duplicate().order(BYTE_ORDER);
        buf.position((int) state.labelOffset);
        int count = 0;
        for (int i = 0; i < state.labelWordCount; i++) {
            count += Long.bitCount(buf.getLong());
        }
        return count;
    }
}
