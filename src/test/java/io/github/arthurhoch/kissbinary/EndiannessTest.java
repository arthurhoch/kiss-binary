package io.github.arthurhoch.kissbinary;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EndiannessTest {

    @Test
    void bigEndianIntByteOrder() {
        BinaryWriter writer = BinaryWriter.create(Endianness.BIG_ENDIAN);

        writer.writeInt(1);

        assertArrayEquals(new byte[]{0, 0, 0, 1}, writer.toByteArray());
    }

    @Test
    void littleEndianIntByteOrder() {
        BinaryWriter writer = BinaryWriter.create(Endianness.LITTLE_ENDIAN);

        writer.writeInt(1);

        assertArrayEquals(new byte[]{1, 0, 0, 0}, writer.toByteArray());
    }

    @Test
    void bigEndianShortByteOrder() {
        BinaryWriter writer = BinaryWriter.create(Endianness.BIG_ENDIAN);

        writer.writeShort((short) 0x0102);

        assertArrayEquals(new byte[]{0x01, 0x02}, writer.toByteArray());
    }

    @Test
    void littleEndianShortByteOrder() {
        BinaryWriter writer = BinaryWriter.create(Endianness.LITTLE_ENDIAN);

        writer.writeShort((short) 0x0102);

        assertArrayEquals(new byte[]{0x02, 0x01}, writer.toByteArray());
    }

    @Test
    void bigEndianLongByteOrder() {
        BinaryWriter writer = BinaryWriter.create(Endianness.BIG_ENDIAN);

        writer.writeLong(0x0102030405060708L);

        assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, writer.toByteArray());
    }

    @Test
    void littleEndianLongByteOrder() {
        BinaryWriter writer = BinaryWriter.create(Endianness.LITTLE_ENDIAN);

        writer.writeLong(0x0102030405060708L);

        assertArrayEquals(new byte[]{8, 7, 6, 5, 4, 3, 2, 1}, writer.toByteArray());
    }

    @Test
    void writeLEReadBEProducesDifferentValue() {
        BinaryWriter writer = BinaryWriter.create(Endianness.LITTLE_ENDIAN);
        writer.writeInt(1);

        int value = BinaryReader.from(writer.toByteArray(), Endianness.BIG_ENDIAN).readInt();

        assertEquals(0x01000000, value);
        assertNotEquals(1, value);
    }

    @Test
    void writeBEReadLEProducesDifferentValue() {
        BinaryWriter writer = BinaryWriter.create(Endianness.BIG_ENDIAN);
        writer.writeInt(1);

        int value = BinaryReader.from(writer.toByteArray(), Endianness.LITTLE_ENDIAN).readInt();

        assertEquals(0x01000000, value);
        assertNotEquals(1, value);
    }

    @Test
    void primitivesRoundtripLittleEndian() {
        BinaryWriter writer = BinaryWriter.create(Endianness.LITTLE_ENDIAN);
        writer.writeChar('Z');
        writer.writeShort((short) 0x1234);
        writer.writeInt(0x12345678);
        writer.writeLong(0x0102030405060708L);
        writer.writeFloat(3.14f);
        writer.writeDouble(2.71828);

        BinaryReader reader = BinaryReader.from(writer.toByteArray(), Endianness.LITTLE_ENDIAN);

        assertEquals('Z', reader.readChar());
        assertEquals((short) 0x1234, reader.readShort());
        assertEquals(0x12345678, reader.readInt());
        assertEquals(0x0102030405060708L, reader.readLong());
        assertEquals(3.14f, reader.readFloat(), 0.001f);
        assertEquals(2.71828, reader.readDouble(), 0.00001);
    }

    @Test
    void intArrayRoundtripLittleEndian() {
        int[] original = {1, 256, 65536, -1, Integer.MAX_VALUE};
        BinaryWriter writer = BinaryWriter.create(Endianness.LITTLE_ENDIAN);
        writer.writeIntArray(original);

        BinaryReader reader = BinaryReader.from(writer.toByteArray(), Endianness.LITTLE_ENDIAN);

        assertArrayEquals(original, reader.readIntArray(original.length));
    }

    @Test
    void enumMapsToByteOrder() {
        assertEquals(java.nio.ByteOrder.BIG_ENDIAN, Endianness.BIG_ENDIAN.byteOrder());
        assertEquals(java.nio.ByteOrder.LITTLE_ENDIAN, Endianness.LITTLE_ENDIAN.byteOrder());
    }
}
