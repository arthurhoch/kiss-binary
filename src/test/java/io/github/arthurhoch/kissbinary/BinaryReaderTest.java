package io.github.arthurhoch.kissbinary;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class BinaryReaderTest {

    @Test
    void fromNullDataThrows() {
        assertThrows(NullPointerException.class, () -> BinaryReader.from((byte[]) null));
    }

    @Test
    void fromNullBufferThrows() {
        assertThrows(NullPointerException.class, () -> BinaryReader.from((ByteBuffer) null));
    }

    @Test
    void fromNullEndiannessThrows() {
        assertThrows(NullPointerException.class, () -> BinaryReader.from(new byte[0], null));
    }

    @Test
    void fromByteBufferReadsRemainingWithoutMutatingOriginalPosition() {
        ByteBuffer source = ByteBuffer.wrap(new byte[]{9, 0, 0, 0, 42});
        source.position(1);

        BinaryReader reader = BinaryReader.from(source);

        assertEquals(42, reader.readInt());
        assertEquals(1, source.position());
    }

    @Test
    void fromByteBufferWithEndianness() {
        ByteBuffer source = ByteBuffer.wrap(new byte[]{1, 0, 0, 0});

        BinaryReader reader = BinaryReader.from(source, Endianness.LITTLE_ENDIAN);

        assertEquals(1, reader.readInt());
    }

    @Test
    void readPrimitives() {
        byte[] data = write(writer -> {
            writer.writeByte((byte) 0x42);
            writer.writeBoolean(true);
            writer.writeBoolean(false);
            writer.writeChar('A');
            writer.writeShort((short) 1000);
            writer.writeInt(42);
            writer.writeLong(123456789L);
            writer.writeFloat(3.14f);
            writer.writeDouble(2.71828);
        });
        BinaryReader reader = BinaryReader.from(data);

        assertEquals((byte) 0x42, reader.readByte());
        assertTrue(reader.readBoolean());
        assertFalse(reader.readBoolean());
        assertEquals('A', reader.readChar());
        assertEquals((short) 1000, reader.readShort());
        assertEquals(42, reader.readInt());
        assertEquals(123456789L, reader.readLong());
        assertEquals(3.14f, reader.readFloat(), 0.001f);
        assertEquals(2.71828, reader.readDouble(), 0.00001);
        assertFalse(reader.hasRemaining());
    }

    @Test
    void readBooleanInvalidThrows() {
        BinaryReader reader = BinaryReader.from(new byte[]{5});

        BinaryFormatException ex = assertThrows(BinaryFormatException.class, reader::readBoolean);

        assertEquals(0, ex.offset());
        assertTrue(ex.getMessage().contains("Invalid boolean"));
    }

    @Test
    void readByteArrayAndBytesAlias() {
        BinaryReader reader = BinaryReader.from(new byte[]{1, 2, 3, 4});

        assertArrayEquals(new byte[]{1, 2}, reader.readByteArray(2));
        assertArrayEquals(new byte[]{3, 4}, reader.readBytes(2));
    }

    @Test
    void readByteArrayNegativeCountThrowsFormatException() {
        BinaryReader reader = BinaryReader.from(new byte[]{1, 2, 3, 4});

        BinaryFormatException ex = assertThrows(BinaryFormatException.class,
                () -> reader.readByteArray(-1));

        assertEquals(0, ex.offset());
        assertTrue(ex.getMessage().contains("non-negative"));
    }

    @Test
    void readFully() {
        BinaryReader reader = BinaryReader.from(new byte[]{10, 20, 30});
        byte[] target = new byte[5];

        reader.readFully(target, 1, 3);

        assertArrayEquals(new byte[]{0, 10, 20, 30, 0}, target);
    }

    @Test
    void readFullyRejectsInvalidArgs() {
        BinaryReader reader = BinaryReader.from(new byte[]{1, 2, 3});

        assertThrows(NullPointerException.class, () -> reader.readFully(null));
        assertThrows(BinaryException.class, () -> reader.readFully(new byte[2], 0, 3));
    }

    @Test
    void readPrimitiveArrays() {
        byte[] data = write(writer -> {
            writer.writeShortArray(new short[]{1, 2});
            writer.writeIntArray(new int[]{3, 4});
            writer.writeLongArray(new long[]{5L, 6L});
            writer.writeFloatArray(new float[]{1.5f, 2.5f});
            writer.writeDoubleArray(new double[]{3.5, 4.5});
            writer.writeCharArray(new char[]{'A', 'B'});
        });
        BinaryReader reader = BinaryReader.from(data);

        assertArrayEquals(new short[]{1, 2}, reader.readShortArray(2));
        assertArrayEquals(new int[]{3, 4}, reader.readIntArray(2));
        assertArrayEquals(new long[]{5L, 6L}, reader.readLongArray(2));
        assertArrayEquals(new float[]{1.5f, 2.5f}, reader.readFloatArray(2));
        assertArrayEquals(new double[]{3.5, 4.5}, reader.readDoubleArray(2));
        assertArrayEquals(new char[]{'A', 'B'}, reader.readCharArray(2));
    }

    @Test
    void readEmptyArray() {
        BinaryReader reader = BinaryReader.from(new byte[0]);

        assertEquals(0, reader.readIntArray(0).length);
    }

    @Test
    void negativeArrayCountThrowsFormatException() {
        BinaryReader reader = BinaryReader.from(new byte[4]);

        BinaryFormatException ex = assertThrows(BinaryFormatException.class,
                () -> reader.readIntArray(-1));

        assertEquals(0, ex.offset());
    }

    @Test
    void arrayCountExceedingRemainingThrowsBeforeAllocating() {
        BinaryReader reader = BinaryReader.from(new byte[4]);

        BinaryFormatException ex = assertThrows(BinaryFormatException.class,
                () -> reader.readByteArray(Integer.MAX_VALUE));

        assertEquals(0, ex.offset());
        assertTrue(ex.getMessage().contains("exceeds remaining bytes"));
    }

    @Test
    void typedArrayCountExceedingRemainingThrowsBeforeAllocating() {
        BinaryReader reader = BinaryReader.from(new byte[4]);

        BinaryFormatException ex = assertThrows(BinaryFormatException.class,
                () -> reader.readIntArray(Integer.MAX_VALUE));

        assertEquals(0, ex.offset());
        assertTrue(ex.getMessage().contains("exceeds remaining bytes"));
    }

    @Test
    void validateMagicSuccessAndMismatch() {
        BinaryReader reader = BinaryReader.from(new byte[]{0x4B, 0x42, 0x01});
        reader.validateMagic(new byte[]{0x4B, 0x42});
        assertEquals(2, reader.position());

        BinaryReader mismatch = BinaryReader.from(new byte[]{0x4A, 0x53});
        BinaryFormatException ex = assertThrows(BinaryFormatException.class,
                () -> mismatch.validateMagic(new byte[]{0x4B, 0x42}));
        assertTrue(ex.getMessage().contains("expected"));
        assertTrue(ex.getMessage().contains("actual"));
        assertEquals(2, mismatch.position());
    }

    @Test
    void expectMagicPreservesAsciiEncodingFallback() {
        BinaryReader reader = BinaryReader.from(new byte[]{'?'});

        reader.expectMagic("\u00E9");

        assertEquals(1, reader.position());
    }

    @Test
    void expectMagicNullThrows() {
        BinaryReader reader = BinaryReader.from(new byte[]{0x4B});

        assertThrows(NullPointerException.class, () -> reader.expectMagic(null));
    }

    @Test
    void validateVersionSuccessAndMismatch() {
        BinaryReader versionReader = BinaryReader.from(write(writer -> writer.writeVersion(4)));
        assertEquals(4, versionReader.readVersion());

        BinaryReader reader = BinaryReader.from(write(writer -> writer.writeVersion(2)));
        reader.validateVersion(2);

        BinaryReader mismatch = BinaryReader.from(write(writer -> writer.writeVersion(3)));
        BinaryFormatException ex = assertThrows(BinaryFormatException.class,
                () -> mismatch.validateVersion(1));
        assertTrue(ex.getMessage().contains("expected 1"));
        assertTrue(ex.getMessage().contains("actual 3"));
    }

    @Test
    void truncatedPrimitiveThrowsFormatException() {
        BinaryReader reader = BinaryReader.from(new byte[]{0, 1});

        BinaryFormatException ex = assertThrows(BinaryFormatException.class, reader::readInt);

        assertEquals(0, ex.offset());
        assertTrue(ex.getMessage().contains("EOF"));
    }

    @Test
    void truncatedShortThrowsFormatException() {
        BinaryReader reader = BinaryReader.from(new byte[]{0});

        assertThrows(BinaryFormatException.class, reader::readShort);
    }

    @Test
    void truncatedLongThrowsFormatException() {
        BinaryReader reader = BinaryReader.from(new byte[]{0, 1, 2, 3});

        assertThrows(BinaryFormatException.class, reader::readLong);
    }

    @Test
    void truncatedFloatThrowsFormatException() {
        BinaryReader reader = BinaryReader.from(new byte[]{0, 1});

        assertThrows(BinaryFormatException.class, reader::readFloat);
    }

    @Test
    void truncatedDoubleThrowsFormatException() {
        BinaryReader reader = BinaryReader.from(new byte[]{0, 1, 2, 3});

        assertThrows(BinaryFormatException.class, reader::readDouble);
    }

    @Test
    void truncatedByteArrayThrowsFormatException() {
        BinaryReader reader = BinaryReader.from(new byte[]{1, 2});

        assertThrows(BinaryFormatException.class, () -> reader.readByteArray(3));
    }

    @Test
    void truncatedTypedArrayThrowsFormatException() {
        BinaryReader reader = BinaryReader.from(new byte[]{0, 0});

        assertThrows(BinaryFormatException.class, () -> reader.readIntArray(1));
    }

    @Test
    void readPrimitiveArraysIntoTargetsMatchReturningMethods() {
        byte[] data = write(writer -> {
            writer.writeShortArray(new short[]{1, 2});
            writer.writeIntArray(new int[]{3, 4});
            writer.writeLongArray(new long[]{5L, 6L});
            writer.writeFloatArray(new float[]{1.5f, 2.5f});
            writer.writeDoubleArray(new double[]{3.5, 4.5});
        });
        BinaryReader returning = BinaryReader.from(data);
        BinaryReader intoTarget = BinaryReader.from(data);

        short[] shorts = new short[2];
        int[] ints = new int[2];
        long[] longs = new long[2];
        float[] floats = new float[2];
        double[] doubles = new double[2];

        intoTarget.readShortArray(shorts);
        intoTarget.readIntArray(ints);
        intoTarget.readLongArray(longs);
        intoTarget.readFloatArray(floats);
        intoTarget.readDoubleArray(doubles);

        assertArrayEquals(returning.readShortArray(2), shorts);
        assertArrayEquals(returning.readIntArray(2), ints);
        assertArrayEquals(returning.readLongArray(2), longs);
        assertArrayEquals(returning.readFloatArray(2), floats);
        assertArrayEquals(returning.readDoubleArray(2), doubles);
        assertFalse(intoTarget.hasRemaining());
    }

    @Test
    void readPrimitiveArraysIntoTargetOffsetAndPartialRange() {
        short[] shorts = new short[]{9, 9, 9, 9};
        BinaryReader.from(write(writer -> writer.writeShortArray(new short[]{1, 2, 3})))
                .readShortArray(shorts, 1, 2);
        assertArrayEquals(new short[]{9, 1, 2, 9}, shorts);

        int[] ints = new int[]{9, 9, 9, 9};
        BinaryReader.from(write(writer -> writer.writeIntArray(new int[]{3, 4, 5})))
                .readIntArray(ints, 1, 2);
        assertArrayEquals(new int[]{9, 3, 4, 9}, ints);

        long[] longs = new long[]{9L, 9L, 9L, 9L};
        BinaryReader.from(write(writer -> writer.writeLongArray(new long[]{5L, 6L, 7L})))
                .readLongArray(longs, 1, 2);
        assertArrayEquals(new long[]{9L, 5L, 6L, 9L}, longs);

        float[] floats = new float[]{9f, 9f, 9f, 9f};
        BinaryReader.from(write(writer -> writer.writeFloatArray(new float[]{1.5f, 2.5f, 3.5f})))
                .readFloatArray(floats, 1, 2);
        assertArrayEquals(new float[]{9f, 1.5f, 2.5f, 9f}, floats);

        double[] doubles = new double[]{9d, 9d, 9d, 9d};
        BinaryReader.from(write(writer -> writer.writeDoubleArray(new double[]{3.5, 4.5, 5.5})))
                .readDoubleArray(doubles, 1, 2);
        assertArrayEquals(new double[]{9d, 3.5, 4.5, 9d}, doubles);
    }

    @Test
    void targetArrayReadsRejectInvalidArguments() {
        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> BinaryReader.from(new byte[0]).readShortArray((short[]) null)),
                () -> assertThrows(NullPointerException.class,
                        () -> BinaryReader.from(new byte[0]).readIntArray((int[]) null)),
                () -> assertThrows(NullPointerException.class,
                        () -> BinaryReader.from(new byte[0]).readLongArray((long[]) null)),
                () -> assertThrows(NullPointerException.class,
                        () -> BinaryReader.from(new byte[0]).readFloatArray((float[]) null)),
                () -> assertThrows(NullPointerException.class,
                        () -> BinaryReader.from(new byte[0]).readDoubleArray((double[]) null)),
                () -> assertThrows(BinaryException.class,
                        () -> BinaryReader.from(new byte[8]).readShortArray(new short[2], -1, 1)),
                () -> assertThrows(BinaryException.class,
                        () -> BinaryReader.from(new byte[8]).readIntArray(new int[2], 0, 3)),
                () -> assertThrows(BinaryException.class,
                        () -> BinaryReader.from(new byte[8]).readLongArray(new long[2], 2, 1)),
                () -> assertThrows(BinaryException.class,
                        () -> BinaryReader.from(new byte[8]).readFloatArray(new float[2], 1, -1)),
                () -> assertThrows(BinaryException.class,
                        () -> BinaryReader.from(new byte[8]).readDoubleArray(new double[2], 1, 2))
        );
    }

    @Test
    void targetArrayReadsThrowOnTruncation() {
        assertAll(
                () -> assertThrows(BinaryFormatException.class,
                        () -> BinaryReader.from(new byte[]{0}).readShortArray(new short[1])),
                () -> assertThrows(BinaryFormatException.class,
                        () -> BinaryReader.from(new byte[]{0, 0}).readIntArray(new int[1])),
                () -> assertThrows(BinaryFormatException.class,
                        () -> BinaryReader.from(new byte[]{0, 0, 0, 0}).readLongArray(new long[1])),
                () -> assertThrows(BinaryFormatException.class,
                        () -> BinaryReader.from(new byte[]{0, 0}).readFloatArray(new float[1])),
                () -> assertThrows(BinaryFormatException.class,
                        () -> BinaryReader.from(new byte[]{0, 0, 0, 0}).readDoubleArray(new double[1]))
        );
    }

    @Test
    void truncatedMagicThrowsFormatException() {
        BinaryReader reader = BinaryReader.from(new byte[]{0x4B});

        BinaryFormatException ex = assertThrows(BinaryFormatException.class,
                () -> reader.validateMagic(new byte[]{0x4B, 0x42}));

        assertEquals(0, ex.offset());
        assertTrue(ex.getMessage().contains("EOF"));
    }

    @Test
    void truncatedVersionThrowsFormatException() {
        BinaryReader reader = BinaryReader.from(new byte[]{0, 0});

        BinaryFormatException ex = assertThrows(BinaryFormatException.class,
                () -> reader.validateVersion(1));

        assertEquals(0, ex.offset());
        assertTrue(ex.getMessage().contains("EOF"));
    }

    @Test
    void positionAndRemainingAdvance() {
        BinaryReader reader = BinaryReader.from(new byte[5]);

        assertEquals(0, reader.position());
        assertEquals(5, reader.remaining());
        assertTrue(reader.hasRemaining());

        reader.readByte();

        assertEquals(1, reader.position());
        assertEquals(4, reader.remaining());
    }

    @Test
    void skipBytesAndSkipFullyAdvancePosition() {
        BinaryReader partial = BinaryReader.from(new byte[]{1, 2, 3, 4});

        assertEquals(2, partial.skipBytes(2));
        assertEquals(2, partial.position());
        assertEquals(2, partial.skipBytes(10));
        assertEquals(4, partial.position());
        assertFalse(partial.hasRemaining());

        BinaryReader full = BinaryReader.from(new byte[]{1, 2, 3, 4});
        full.skipFully(3);
        assertEquals(3, full.position());
        assertEquals(4, full.readByte());
    }

    @Test
    void skipFullyFailsOnTruncatedInput() {
        BinaryReader reader = BinaryReader.from(new byte[]{1, 2});

        BinaryFormatException ex = assertThrows(BinaryFormatException.class,
                () -> reader.skipFully(3));

        assertEquals(0, ex.offset());
        assertEquals(0, reader.position());
    }

    @Test
    void skipRejectsNegativeCounts() {
        BinaryReader reader = BinaryReader.from(new byte[]{1, 2});

        assertThrows(BinaryException.class, () -> reader.skipBytes(-1));
        assertThrows(BinaryException.class, () -> reader.skipFully(-1));
    }

    private static byte[] write(Consumer<BinaryWriter> consumer) {
        BinaryWriter writer = BinaryWriter.create();
        consumer.accept(writer);
        return writer.toByteArray();
    }
}
