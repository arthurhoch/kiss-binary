package io.github.arthurhoch.kissbinary;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class BinaryWriterTest {

    @Test
    void createWithNullEndiannessThrows() {
        assertThrows(NullPointerException.class, () -> BinaryWriter.create(null));
    }

    @Test
    void writeByteAdvancesSize() {
        BinaryWriter writer = BinaryWriter.create();

        writer.writeByte((byte) 0x42);
        writer.writeByte(0xFF);

        assertEquals(2, writer.size());
        assertArrayEquals(new byte[]{0x42, (byte) 0xFF}, writer.toByteArray());
    }

    @Test
    void writeBoolean() {
        BinaryWriter writer = BinaryWriter.create();

        writer.writeBoolean(true);
        writer.writeBoolean(false);

        assertArrayEquals(new byte[]{1, 0}, writer.toByteArray());
    }

    @Test
    void writeChar() {
        BinaryWriter writer = BinaryWriter.create();

        writer.writeChar('A');

        assertArrayEquals(new byte[]{0, 65}, writer.toByteArray());
    }

    @Test
    void writeShort() {
        BinaryWriter writer = BinaryWriter.create();

        writer.writeShort((short) 0x0102);

        assertArrayEquals(new byte[]{0x01, 0x02}, writer.toByteArray());
    }

    @Test
    void writeInt() {
        BinaryWriter writer = BinaryWriter.create();

        writer.writeInt(0x01020304);

        assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04}, writer.toByteArray());
    }

    @Test
    void writeLong() {
        BinaryWriter writer = BinaryWriter.create();

        writer.writeLong(0x0102030405060708L);

        assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, writer.toByteArray());
    }

    @Test
    void writeFloatAndDoubleAdvanceSize() {
        BinaryWriter writer = BinaryWriter.create();

        writer.writeFloat(3.14f);
        writer.writeDouble(2.71828);

        assertEquals(12, writer.size());
        assertEquals(12, writer.position());
    }

    @Test
    void writeByteArrayAndBytesAliases() {
        BinaryWriter writer = BinaryWriter.create();

        writer.writeByteArray(new byte[]{1, 2});
        writer.writeBytes(new byte[]{3});
        writer.writeBytes(new byte[]{3, 4, 5}, 1, 2);

        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, writer.toByteArray());
    }

    @Test
    void writeBytesRejectsInvalidRanges() {
        BinaryWriter writer = BinaryWriter.create();

        assertThrows(NullPointerException.class, () -> writer.writeBytes(null));
        assertThrows(NullPointerException.class, () -> writer.writeByteArray(null));
        assertThrows(BinaryException.class, () -> writer.writeBytes(new byte[]{1, 2}, -1, 1));
        assertThrows(BinaryException.class, () -> writer.writeBytes(new byte[]{1, 2}, 0, 3));
    }

    @Test
    void writePrimitiveArrays() {
        BinaryWriter writer = BinaryWriter.create();

        writer.writeShortArray(new short[]{1, 2});
        writer.writeIntArray(new int[]{3, 4});
        writer.writeLongArray(new long[]{5L});
        writer.writeFloatArray(new float[]{1.5f});
        writer.writeDoubleArray(new double[]{2.5});
        writer.writeCharArray(new char[]{'A', 'B'});

        assertEquals(2 * Short.BYTES
                + 2 * Integer.BYTES
                + Long.BYTES
                + Float.BYTES
                + Double.BYTES
                + 2 * Character.BYTES, writer.size());
    }

    @Test
    void writeEmptyArray() {
        BinaryWriter writer = BinaryWriter.create();

        writer.writeIntArray(new int[0]);

        assertEquals(0, writer.size());
    }

    @Test
    void writeArrayNullThrows() {
        BinaryWriter writer = BinaryWriter.create();

        assertThrows(NullPointerException.class, () -> writer.writeIntArray(null));
    }

    @Test
    void writeMagicAndVersion() {
        BinaryWriter writer = BinaryWriter.create();

        writer.writeMagic("KB");
        writer.writeVersion(1);

        assertArrayEquals(new byte[]{0x4B, 0x42, 0, 0, 0, 1}, writer.toByteArray());
    }

    @Test
    void writeMagicPreservesAsciiEncodingFallback() {
        BinaryWriter writer = BinaryWriter.create();

        writer.writeMagic("\u00E9");

        assertArrayEquals(new byte[]{'?'}, writer.toByteArray());
    }

    @Test
    void writeToDoesNotCloseStream() {
        BinaryWriter writer = BinaryWriter.create();
        writer.writeInt(42);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        writer.writeTo(out);
        out.write(7);

        assertArrayEquals(new byte[]{0, 0, 0, 42, 7}, out.toByteArray());
    }

    @Test
    void writeToNullThrows() {
        BinaryWriter writer = BinaryWriter.create();

        assertThrows(NullPointerException.class, () -> writer.writeTo(null));
    }
}
