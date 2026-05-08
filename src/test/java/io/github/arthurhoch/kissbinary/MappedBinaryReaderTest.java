package io.github.arthurhoch.kissbinary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MappedBinaryReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readPrimitivesAtOffset() throws IOException {
        Path file = writeTestFile();

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            assertEquals((byte) 0x4B, reader.readByte(0));
            assertEquals(3, reader.readInt(6));
            assertEquals(10, reader.readInt(10));
            assertEquals(20, reader.readInt(14));
            assertEquals(30, reader.readInt(18));
            assertEquals(100L, reader.readLong(22));
            assertEquals(200L, reader.readLong(30));
            assertEquals(300L, reader.readLong(38));
            assertEquals(3.14, reader.readDouble(46), 0.001);
            assertTrue(reader.readBoolean(54));
            assertEquals('Z', reader.readChar(55));
        }
    }

    @Test
    void readPrimitiveArraysAtOffset() throws IOException {
        Path file = writeTestFile();

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            assertArrayEquals(new int[]{10, 20, 30}, reader.readIntArray(10, 3));
            assertArrayEquals(new long[]{100L, 200L, 300L}, reader.readLongArray(22, 3));
            assertArrayEquals(new byte[]{0x4B, 0x42}, reader.readByteArray(0, 2));
            assertArrayEquals(new char[]{'Z'}, reader.readCharArray(55, 1));
        }
    }

    @Test
    void readShortFloatAndDoubleArrays() throws IOException {
        BinaryWriter writer = BinaryWriter.create();
        writer.writeShortArray(new short[]{1, 2});
        writer.writeFloatArray(new float[]{1.5f, 2.5f});
        writer.writeDoubleArray(new double[]{3.5, 4.5});
        Path file = write(writer.toByteArray());

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            assertEquals((short) 1, reader.readShort(0));
            assertEquals(1.5f, reader.readFloat(4), 0.001f);
            assertEquals(3.5, reader.readDouble(12), 0.001);
            assertArrayEquals(new short[]{1, 2}, reader.readShortArray(0, 2));
            assertArrayEquals(new float[]{1.5f, 2.5f}, reader.readFloatArray(4, 2));
            assertArrayEquals(new double[]{3.5, 4.5}, reader.readDoubleArray(12, 2));
        }
    }

    @Test
    void readBytesIntoTarget() throws IOException {
        Path file = write(new byte[]{10, 20, 30, 40, 50});

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            byte[] target = new byte[5];
            reader.readBytes(1, target, 1, 3);

            assertArrayEquals(new byte[]{0, 20, 30, 40, 0}, target);
        }
    }

    @Test
    void readBytesRejectsInvalidArgs() throws IOException {
        Path file = write(new byte[]{1, 2, 3});

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            assertThrows(NullPointerException.class, () -> reader.readBytes(0, null, 0, 1));
            assertThrows(BinaryException.class, () -> reader.readBytes(0, new byte[2], 0, 3));
            assertThrows(BinaryException.class, () -> reader.readBytes(0, new byte[2], -1, 1));
        }
    }

    @Test
    void invalidMappedBooleanThrowsFormatException() throws IOException {
        Path file = write(new byte[]{5});

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            BinaryFormatException ex = assertThrows(BinaryFormatException.class,
                    () -> reader.readBoolean(0));

            assertEquals(0, ex.offset());
            assertTrue(ex.getMessage().contains("Invalid boolean"));
        }
    }

    @Test
    void validateMagicAndVersion() throws IOException {
        Path file = writeTestFile();

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            reader.validateMagic(0, new byte[]{0x4B, 0x42});
            reader.expectMagic("KB");
            reader.validateVersion(2, 1);
        }
    }

    @Test
    void validateMagicMismatch() throws IOException {
        Path file = writeTestFile();

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            BinaryFormatException ex = assertThrows(BinaryFormatException.class,
                    () -> reader.validateMagic(0, new byte[]{0x4A, 0x53}));

            assertEquals(0, ex.offset());
            assertTrue(ex.getMessage().contains("Invalid magic"));
        }
    }

    @Test
    void expectMagicPreservesAsciiEncodingFallback() throws IOException {
        Path file = write(new byte[]{'?'});

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            reader.expectMagic("\u00E9");
        }
    }

    @Test
    void validateVersionMismatch() throws IOException {
        Path file = writeTestFile();

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            BinaryFormatException ex = assertThrows(BinaryFormatException.class,
                    () -> reader.validateVersion(2, 2));

            assertEquals(2, ex.offset());
            assertTrue(ex.getMessage().contains("Invalid version"));
        }
    }

    @Test
    void size() throws IOException {
        Path file = writeTestFile();

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            assertEquals(57, reader.size());
        }
    }

    @Test
    void outOfBoundsReadThrowsBinaryFormatException() throws IOException {
        Path file = writeTestFile();

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            assertThrows(BinaryFormatException.class, () -> reader.readInt(100));
            assertThrows(BinaryFormatException.class, () -> reader.readLong(50));
            assertThrows(BinaryFormatException.class, () -> reader.readInt(-1));
        }
    }

    @Test
    void hugeOffsetDoesNotOverflowBoundsCheck() throws IOException {
        Path file = writeTestFile();

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            BinaryFormatException ex = assertThrows(BinaryFormatException.class,
                    () -> reader.readInt(Long.MAX_VALUE));

            assertEquals(Long.MAX_VALUE, ex.offset());
        }
    }

    @Test
    void arrayCountExceedingRemainingThrowsBeforeAllocating() throws IOException {
        Path file = write(new byte[4]);

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            assertThrows(BinaryFormatException.class,
                    () -> reader.readByteArray(0, Integer.MAX_VALUE));
            assertThrows(BinaryFormatException.class,
                    () -> reader.readIntArray(0, Integer.MAX_VALUE));
        }
    }

    @Test
    void outOfBoundsMappedArrayReadsThrowFormatException() throws IOException {
        Path file = write(new byte[4]);

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            assertThrows(BinaryFormatException.class, () -> reader.readShortArray(3, 1));
            assertThrows(BinaryFormatException.class, () -> reader.readLongArray(0, 1));
            assertThrows(BinaryFormatException.class, () -> reader.readFloatArray(2, 1));
            assertThrows(BinaryFormatException.class, () -> reader.readDoubleArray(0, 1));
            assertThrows(BinaryFormatException.class, () -> reader.readCharArray(3, 1));
        }
    }

    @Test
    void readPrimitiveArraysIntoTargetsMatchReturningMethods() throws IOException {
        BinaryWriter writer = BinaryWriter.create();
        writer.writeShortArray(new short[]{1, 2});
        writer.writeIntArray(new int[]{3, 4});
        writer.writeLongArray(new long[]{5L, 6L});
        writer.writeFloatArray(new float[]{1.5f, 2.5f});
        writer.writeDoubleArray(new double[]{3.5, 4.5});
        Path file = write(writer.toByteArray());

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            short[] shorts = new short[2];
            int[] ints = new int[2];
            long[] longs = new long[2];
            float[] floats = new float[2];
            double[] doubles = new double[2];

            reader.readShortArray(0, shorts);
            reader.readIntArray(4, ints);
            reader.readLongArray(12, longs);
            reader.readFloatArray(28, floats);
            reader.readDoubleArray(36, doubles);

            assertArrayEquals(reader.readShortArray(0, 2), shorts);
            assertArrayEquals(reader.readIntArray(4, 2), ints);
            assertArrayEquals(reader.readLongArray(12, 2), longs);
            assertArrayEquals(reader.readFloatArray(28, 2), floats);
            assertArrayEquals(reader.readDoubleArray(36, 2), doubles);
        }
    }

    @Test
    void mappedTargetArrayOffsetAndPartialReadsWork() throws IOException {
        BinaryWriter writer = BinaryWriter.create();
        writer.writeShortArray(new short[]{1, 2, 3, 4});
        writer.writeIntArray(new int[]{5, 6, 7, 8});
        writer.writeLongArray(new long[]{9L, 10L, 11L, 12L});
        writer.writeFloatArray(new float[]{1.5f, 2.5f, 3.5f, 4.5f});
        writer.writeDoubleArray(new double[]{5.5, 6.5, 7.5, 8.5});
        Path file = write(writer.toByteArray());

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            short[] shorts = new short[]{-1, -1, -1, -1};
            int[] ints = new int[]{-1, -1, -1, -1};
            long[] longs = new long[]{-1L, -1L, -1L, -1L};
            float[] floats = new float[]{-1f, -1f, -1f, -1f};
            double[] doubles = new double[]{-1d, -1d, -1d, -1d};

            reader.readShortArray(Short.BYTES, shorts, 1, 2);
            reader.readIntArray(8 + Integer.BYTES, ints, 1, 2);
            reader.readLongArray(24 + Long.BYTES, longs, 1, 2);
            reader.readFloatArray(56 + Float.BYTES, floats, 1, 2);
            reader.readDoubleArray(72 + Double.BYTES, doubles, 1, 2);

            assertArrayEquals(new short[]{-1, 2, 3, -1}, shorts);
            assertArrayEquals(new int[]{-1, 6, 7, -1}, ints);
            assertArrayEquals(new long[]{-1L, 10L, 11L, -1L}, longs);
            assertArrayEquals(new float[]{-1f, 2.5f, 3.5f, -1f}, floats);
            assertArrayEquals(new double[]{-1d, 6.5, 7.5, -1d}, doubles);
        }
    }

    @Test
    void mappedTargetArrayReadsRejectInvalidArguments() throws IOException {
        Path file = write(new byte[16]);

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            assertAll(
                    () -> assertThrows(NullPointerException.class,
                            () -> reader.readShortArray(0, (short[]) null)),
                    () -> assertThrows(NullPointerException.class,
                            () -> reader.readIntArray(0, (int[]) null)),
                    () -> assertThrows(NullPointerException.class,
                            () -> reader.readLongArray(0, (long[]) null)),
                    () -> assertThrows(NullPointerException.class,
                            () -> reader.readFloatArray(0, (float[]) null)),
                    () -> assertThrows(NullPointerException.class,
                            () -> reader.readDoubleArray(0, (double[]) null)),
                    () -> assertThrows(BinaryException.class,
                            () -> reader.readShortArray(0, new short[2], -1, 1)),
                    () -> assertThrows(BinaryException.class,
                            () -> reader.readIntArray(0, new int[2], 0, 3)),
                    () -> assertThrows(BinaryException.class,
                            () -> reader.readLongArray(0, new long[2], 2, 1)),
                    () -> assertThrows(BinaryException.class,
                            () -> reader.readFloatArray(0, new float[2], 1, -1)),
                    () -> assertThrows(BinaryException.class,
                            () -> reader.readDoubleArray(0, new double[2], 1, 2))
            );
        }
    }

    @Test
    void mappedTargetArrayReadsThrowOnTruncation() throws IOException {
        Path file = write(new byte[4]);

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            assertAll(
                    () -> assertThrows(BinaryFormatException.class,
                            () -> reader.readShortArray(3, new short[1])),
                    () -> assertThrows(BinaryFormatException.class,
                            () -> reader.readIntArray(2, new int[1])),
                    () -> assertThrows(BinaryFormatException.class,
                            () -> reader.readLongArray(0, new long[1])),
                    () -> assertThrows(BinaryFormatException.class,
                            () -> reader.readFloatArray(2, new float[1])),
                    () -> assertThrows(BinaryFormatException.class,
                            () -> reader.readDoubleArray(0, new double[1]))
            );
        }
    }

    @Test
    void negativeArrayCountThrowsFormatException() throws IOException {
        Path file = write(new byte[4]);

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            BinaryFormatException ex = assertThrows(BinaryFormatException.class,
                    () -> reader.readIntArray(0, -1));

            assertEquals(0, ex.offset());
        }
    }

    @Test
    void zeroLengthArrayAtEndIsAllowed() throws IOException {
        Path file = write(new byte[0]);

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            assertEquals(0, reader.readByteArray(0, 0).length);
            assertEquals(0, reader.readIntArray(0, 0).length);
        }
    }

    @Test
    void nullPathAndEndiannessThrow() {
        assertThrows(NullPointerException.class, () -> MappedBinaryReader.from(null));
        assertThrows(NullPointerException.class, () -> MappedBinaryReader.from(newFile(), null));
    }

    @Test
    void littleEndianRead() throws IOException {
        BinaryWriter writer = BinaryWriter.create(Endianness.LITTLE_ENDIAN);
        writer.writeInt(1);
        Path file = write(writer.toByteArray());

        try (MappedBinaryReader reader = MappedBinaryReader.from(file, Endianness.LITTLE_ENDIAN)) {
            assertEquals(1, reader.readInt(0));
        }
    }

    @Test
    void repeatedRandomAccessDoesNotAdvanceCursor() throws IOException {
        Path file = writeTestFile();

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            assertEquals(3, reader.readInt(6));
            assertEquals(3, reader.readInt(6));
            assertEquals(10, reader.readInt(10));
            assertEquals(100L, reader.readLong(22));
            assertEquals(3, reader.readInt(6));
        }
    }

    @Test
    void closeAndReusePath() throws IOException {
        Path file = writeTestFile();

        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            assertEquals(3, reader.readInt(6));
        }
        try (MappedBinaryReader reader = MappedBinaryReader.from(file)) {
            assertEquals(3, reader.readInt(6));
        }
    }

    private Path writeTestFile() throws IOException {
        BinaryWriter writer = BinaryWriter.create();
        writer.writeMagic("KB");
        writer.writeVersion(1);
        writer.writeInt(3);
        writer.writeIntArray(new int[]{10, 20, 30});
        writer.writeLongArray(new long[]{100L, 200L, 300L});
        writer.writeDouble(3.14);
        writer.writeBoolean(true);
        writer.writeChar('Z');
        return write(writer.toByteArray());
    }

    private Path write(byte[] data) throws IOException {
        Path file = newFile();
        Files.write(file, data);
        return file;
    }

    private Path newFile() {
        return tempDir.resolve("mmap-" + System.nanoTime() + ".bin");
    }
}
