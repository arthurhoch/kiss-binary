package io.github.arthurhoch.kissbinary;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Writes primitives and primitive arrays to an in-memory binary buffer with explicit endianness.
 *
 * <p>Instances are not thread-safe. The writer owns its internal buffer and does not close
 * streams passed to {@link #writeTo(OutputStream)}.</p>
 */
public final class BinaryWriter {

    private static final int DEFAULT_CAPACITY = 64;
    private static final int ARRAY_CHUNK = 8192;

    private final Endianness endianness;
    private final boolean littleEndian;
    private byte[] buffer;
    private int size;

    private BinaryWriter(Endianness endianness) {
        this.endianness = endianness;
        this.littleEndian = endianness == Endianness.LITTLE_ENDIAN;
        this.buffer = new byte[DEFAULT_CAPACITY];
        this.size = 0;
    }

    /**
     * Creates a writer using big-endian byte order.
     *
     * @return a new writer
     */
    public static BinaryWriter create() {
        return create(Endianness.BIG_ENDIAN);
    }

    /**
     * Creates a writer using the specified endianness.
     *
     * @param endianness the byte order
     * @return a new writer
     * @throws NullPointerException if endianness is null
     */
    public static BinaryWriter create(Endianness endianness) {
        Objects.requireNonNull(endianness, "Endianness must not be null");
        return new BinaryWriter(endianness);
    }

    /**
     * Writes a single byte.
     *
     * @param value the byte value
     */
    public void writeByte(byte value) {
        ensureCapacity(Byte.BYTES);
        buffer[size++] = value;
    }

    /**
     * Writes the low 8 bits of an integer as a single byte.
     *
     * @param value the byte value
     */
    public void writeByte(int value) {
        writeByte((byte) value);
    }

    /**
     * Writes a boolean as a single byte: 0 for false, 1 for true.
     *
     * @param value the boolean value
     */
    public void writeBoolean(boolean value) {
        writeByte(value ? 1 : 0);
    }

    /**
     * Writes a char.
     *
     * @param value the char value
     */
    public void writeChar(char value) {
        ensureCapacity(Character.BYTES);
        writeShortBits(value);
    }

    /**
     * Writes a short.
     *
     * @param value the short value
     */
    public void writeShort(short value) {
        ensureCapacity(Short.BYTES);
        writeShortBits(value);
    }

    /**
     * Writes an int.
     *
     * @param value the int value
     */
    public void writeInt(int value) {
        ensureCapacity(Integer.BYTES);
        writeIntBits(value);
    }

    /**
     * Writes a long.
     *
     * @param value the long value
     */
    public void writeLong(long value) {
        ensureCapacity(Long.BYTES);
        writeLongBits(value);
    }

    /**
     * Writes a float.
     *
     * @param value the float value
     */
    public void writeFloat(float value) {
        writeInt(Float.floatToRawIntBits(value));
    }

    /**
     * Writes a double.
     *
     * @param value the double value
     */
    public void writeDouble(double value) {
        writeLong(Double.doubleToRawLongBits(value));
    }

    /**
     * Writes all bytes from the array.
     *
     * @param values the byte array
     * @throws NullPointerException if values is null
     */
    public void writeByteArray(byte[] values) {
        writeBytes(values);
    }

    /**
     * Writes all bytes from the array.
     *
     * @param values the byte array
     * @throws NullPointerException if values is null
     */
    public void writeBytes(byte[] values) {
        Objects.requireNonNull(values, "Byte array must not be null");
        writeBytes(values, 0, values.length);
    }

    /**
     * Writes a range of bytes from the array.
     *
     * @param values the byte array
     * @param offset the starting offset in the array
     * @param length the number of bytes to write
     * @throws NullPointerException if values is null
     * @throws BinaryException if offset or length is invalid
     */
    public void writeBytes(byte[] values, int offset, int length) {
        Objects.requireNonNull(values, "Byte array must not be null");
        if (offset < 0 || length < 0 || offset > values.length - length) {
            throw new BinaryException("Invalid offset/length: offset=" + offset
                    + ", length=" + length + ", arrayLength=" + values.length);
        }
        ensureCapacity(length);
        System.arraycopy(values, offset, buffer, size, length);
        size += length;
    }

    /**
     * Writes a short array.
     *
     * @param values the short array
     * @throws NullPointerException if values is null
     */
    public void writeShortArray(short[] values) {
        Objects.requireNonNull(values, "Array must not be null");
        writeTypedArray(values, values.length, Short.BYTES,
                (buf, arr, off, len) -> buf.asShortBuffer().put(arr, off, len));
    }

    /**
     * Writes an int array.
     *
     * @param values the int array
     * @throws NullPointerException if values is null
     */
    public void writeIntArray(int[] values) {
        Objects.requireNonNull(values, "Array must not be null");
        writeTypedArray(values, values.length, Integer.BYTES,
                (buf, arr, off, len) -> buf.asIntBuffer().put(arr, off, len));
    }

    /**
     * Writes a long array.
     *
     * @param values the long array
     * @throws NullPointerException if values is null
     */
    public void writeLongArray(long[] values) {
        Objects.requireNonNull(values, "Array must not be null");
        writeTypedArray(values, values.length, Long.BYTES,
                (buf, arr, off, len) -> buf.asLongBuffer().put(arr, off, len));
    }

    /**
     * Writes a float array.
     *
     * @param values the float array
     * @throws NullPointerException if values is null
     */
    public void writeFloatArray(float[] values) {
        Objects.requireNonNull(values, "Array must not be null");
        writeTypedArray(values, values.length, Float.BYTES,
                (buf, arr, off, len) -> buf.asFloatBuffer().put(arr, off, len));
    }

    /**
     * Writes a double array.
     *
     * @param values the double array
     * @throws NullPointerException if values is null
     */
    public void writeDoubleArray(double[] values) {
        Objects.requireNonNull(values, "Array must not be null");
        writeTypedArray(values, values.length, Double.BYTES,
                (buf, arr, off, len) -> buf.asDoubleBuffer().put(arr, off, len));
    }

    /**
     * Writes a char array.
     *
     * @param values the char array
     * @throws NullPointerException if values is null
     */
    public void writeCharArray(char[] values) {
        Objects.requireNonNull(values, "Array must not be null");
        writeTypedArray(values, values.length, Character.BYTES,
                (buf, arr, off, len) -> buf.asCharBuffer().put(arr, off, len));
    }

    /**
     * Writes magic bytes from an ASCII string.
     *
     * @param asciiMagic the magic string
     * @throws NullPointerException if asciiMagic is null
     */
    public void writeMagic(String asciiMagic) {
        Objects.requireNonNull(asciiMagic, "Magic string must not be null");
        if (!isAscii(asciiMagic)) {
            writeByteArray(asciiMagic.getBytes(StandardCharsets.US_ASCII));
            return;
        }
        int length = asciiMagic.length();
        ensureCapacity(length);
        for (int i = 0; i < length; i++) {
            buffer[size++] = (byte) asciiMagic.charAt(i);
        }
    }

    /**
     * Writes a version number as a 4-byte int.
     *
     * @param version the version number
     */
    public void writeVersion(int version) {
        writeInt(version);
    }

    /**
     * Writes the current buffer contents to an output stream without closing it.
     *
     * @param out the output stream
     * @throws NullPointerException if out is null
     * @throws BinaryException if writing fails
     */
    public void writeTo(OutputStream out) {
        Objects.requireNonNull(out, "OutputStream must not be null");
        try {
            out.write(buffer, 0, size);
        } catch (IOException e) {
            throw new BinaryException("Write failed", e);
        }
    }

    /**
     * Returns a copy of the written bytes.
     *
     * @return written bytes
     */
    public byte[] toByteArray() {
        return Arrays.copyOf(buffer, size);
    }

    /**
     * Returns the number of bytes written.
     *
     * @return current size in bytes
     */
    public int size() {
        return size;
    }

    /**
     * Returns the number of bytes written.
     *
     * @return current position in bytes
     */
    public int position() {
        return size;
    }

    private void writeShortBits(int value) {
        int pos = size;
        if (littleEndian) {
            buffer[pos++] = (byte) value;
            buffer[pos++] = (byte) (value >>> 8);
        } else {
            buffer[pos++] = (byte) (value >>> 8);
            buffer[pos++] = (byte) value;
        }
        size = pos;
    }

    private void writeIntBits(int value) {
        int pos = size;
        if (littleEndian) {
            buffer[pos++] = (byte) value;
            buffer[pos++] = (byte) (value >>> 8);
            buffer[pos++] = (byte) (value >>> 16);
            buffer[pos++] = (byte) (value >>> 24);
        } else {
            buffer[pos++] = (byte) (value >>> 24);
            buffer[pos++] = (byte) (value >>> 16);
            buffer[pos++] = (byte) (value >>> 8);
            buffer[pos++] = (byte) value;
        }
        size = pos;
    }

    private void writeLongBits(long value) {
        int pos = size;
        if (littleEndian) {
            buffer[pos++] = (byte) value;
            buffer[pos++] = (byte) (value >>> 8);
            buffer[pos++] = (byte) (value >>> 16);
            buffer[pos++] = (byte) (value >>> 24);
            buffer[pos++] = (byte) (value >>> 32);
            buffer[pos++] = (byte) (value >>> 40);
            buffer[pos++] = (byte) (value >>> 48);
            buffer[pos++] = (byte) (value >>> 56);
        } else {
            buffer[pos++] = (byte) (value >>> 56);
            buffer[pos++] = (byte) (value >>> 48);
            buffer[pos++] = (byte) (value >>> 40);
            buffer[pos++] = (byte) (value >>> 32);
            buffer[pos++] = (byte) (value >>> 24);
            buffer[pos++] = (byte) (value >>> 16);
            buffer[pos++] = (byte) (value >>> 8);
            buffer[pos++] = (byte) value;
        }
        size = pos;
    }

    @FunctionalInterface
    private interface ArrayEncoder<T> {
        void encode(ByteBuffer buf, T arr, int offset, int length);
    }

    private <T> void writeTypedArray(T array, int length, int elementSize, ArrayEncoder<T> encoder) {
        int offset = 0;
        while (offset < length) {
            int chunkLength = Math.min(ARRAY_CHUNK, length - offset);
            int byteLength = chunkLength * elementSize;
            ensureCapacity(byteLength);
            ByteBuffer target = ByteBuffer.wrap(buffer, size, byteLength)
                    .slice()
                    .order(endianness.byteOrder());
            encoder.encode(target, array, offset, chunkLength);
            size += byteLength;
            offset += chunkLength;
        }
    }

    private static boolean isAscii(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) > 0x7F) {
                return false;
            }
        }
        return true;
    }

    private void ensureCapacity(int additional) {
        if (additional < 0 || additional > Integer.MAX_VALUE - size) {
            throw new BinaryException("Writer size exceeds maximum byte array size");
        }
        int required = size + additional;
        if (required <= buffer.length) {
            return;
        }
        int newCapacity = buffer.length;
        while (newCapacity < required) {
            int grown = newCapacity + (newCapacity >> 1) + 1;
            newCapacity = Math.max(grown, required);
        }
        buffer = Arrays.copyOf(buffer, newCapacity);
    }
}
