package io.github.arthurhoch.kissbinary;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoundtripTest {

    @Test
    void primitiveRoundtrip() {
        BinaryWriter writer = BinaryWriter.create();
        writer.writeByte((byte) 0x42);
        writer.writeBoolean(true);
        writer.writeBoolean(false);
        writer.writeChar('K');
        writer.writeShort(Short.MAX_VALUE);
        writer.writeShort(Short.MIN_VALUE);
        writer.writeInt(Integer.MAX_VALUE);
        writer.writeInt(Integer.MIN_VALUE);
        writer.writeLong(Long.MAX_VALUE);
        writer.writeLong(Long.MIN_VALUE);
        writer.writeFloat(3.14f);
        writer.writeFloat(-1.0f);
        writer.writeDouble(2.71828);
        writer.writeDouble(-1.0);

        BinaryReader reader = BinaryReader.from(writer.toByteArray());

        assertEquals((byte) 0x42, reader.readByte());
        assertTrue(reader.readBoolean());
        assertFalse(reader.readBoolean());
        assertEquals('K', reader.readChar());
        assertEquals(Short.MAX_VALUE, reader.readShort());
        assertEquals(Short.MIN_VALUE, reader.readShort());
        assertEquals(Integer.MAX_VALUE, reader.readInt());
        assertEquals(Integer.MIN_VALUE, reader.readInt());
        assertEquals(Long.MAX_VALUE, reader.readLong());
        assertEquals(Long.MIN_VALUE, reader.readLong());
        assertEquals(3.14f, reader.readFloat(), 0.001f);
        assertEquals(-1.0f, reader.readFloat());
        assertEquals(2.71828, reader.readDouble(), 0.00001);
        assertEquals(-1.0, reader.readDouble());
    }

    @Test
    void byteArrayRoundtrip() {
        byte[] original = {10, 20, 30, 40, 50};
        BinaryWriter writer = BinaryWriter.create();
        writer.writeByteArray(original);

        BinaryReader reader = BinaryReader.from(writer.toByteArray());

        assertArrayEquals(original, reader.readByteArray(original.length));
    }

    @Test
    void partialBytesRoundtrip() {
        byte[] original = {10, 20, 30, 40, 50};
        BinaryWriter writer = BinaryWriter.create();
        writer.writeBytes(original, 1, 3);

        BinaryReader reader = BinaryReader.from(writer.toByteArray());

        assertArrayEquals(new byte[]{20, 30, 40}, reader.readByteArray(3));
    }

    @Test
    void primitiveArrayRoundtrip() {
        short[] shorts = {0, Short.MAX_VALUE, Short.MIN_VALUE, 500};
        int[] ints = {1, 100, Integer.MAX_VALUE, Integer.MIN_VALUE, 0, -42};
        long[] longs = {0L, Long.MAX_VALUE, Long.MIN_VALUE, 999L};
        float[] floats = {0.0f, 1.5f, -2.5f, Float.MAX_VALUE};
        double[] doubles = {0.0, 1.5, -2.5, Double.MAX_VALUE};
        char[] chars = {'A', 'B', '\u1234'};

        BinaryWriter writer = BinaryWriter.create();
        writer.writeShortArray(shorts);
        writer.writeIntArray(ints);
        writer.writeLongArray(longs);
        writer.writeFloatArray(floats);
        writer.writeDoubleArray(doubles);
        writer.writeCharArray(chars);

        BinaryReader reader = BinaryReader.from(writer.toByteArray());

        assertArrayEquals(shorts, reader.readShortArray(shorts.length));
        assertArrayEquals(ints, reader.readIntArray(ints.length));
        assertArrayEquals(longs, reader.readLongArray(longs.length));
        assertArrayEquals(floats, reader.readFloatArray(floats.length));
        assertArrayEquals(doubles, reader.readDoubleArray(doubles.length));
        assertArrayEquals(chars, reader.readCharArray(chars.length));
    }

    @Test
    void largeIntArrayRoundtrip() {
        int[] original = new int[10000];
        for (int i = 0; i < original.length; i++) {
            original[i] = i * 7;
        }
        BinaryWriter writer = BinaryWriter.create();
        writer.writeIntArray(original);

        BinaryReader reader = BinaryReader.from(writer.toByteArray());

        assertArrayEquals(original, reader.readIntArray(original.length));
    }

    @Test
    void headerAndDataRoundtrip() {
        int[] keys = {1, 2, 3, 4, 5};
        long[] values = {100L, 200L, 300L, 400L, 500L};

        BinaryWriter writer = BinaryWriter.create();
        writer.writeMagic("KB");
        writer.writeVersion(1);
        writer.writeInt(keys.length);
        writer.writeIntArray(keys);
        writer.writeLongArray(values);

        BinaryReader reader = BinaryReader.from(writer.toByteArray());
        reader.expectMagic("KB");
        reader.expectVersion(1);
        int count = reader.readInt();

        assertEquals(5, count);
        assertArrayEquals(keys, reader.readIntArray(count));
        assertArrayEquals(values, reader.readLongArray(count));
    }

    @Test
    void sequentialReadsMatchWriteOrder() {
        BinaryWriter writer = BinaryWriter.create();
        writer.writeMagic("TST");
        writer.writeInt(3);
        for (int i = 0; i < 3; i++) {
            writer.writeInt(i * 10);
            writer.writeDouble(i + 0.5);
        }

        BinaryReader reader = BinaryReader.from(writer.toByteArray());
        reader.expectMagic("TST");
        int count = reader.readInt();

        assertEquals(3, count);
        for (int i = 0; i < count; i++) {
            assertEquals(i * 10, reader.readInt());
            assertEquals(i + 0.5, reader.readDouble(), 0.001);
        }
    }
}
