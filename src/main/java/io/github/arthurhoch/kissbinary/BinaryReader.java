package io.github.arthurhoch.kissbinary;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Reads primitives and primitive arrays from a byte buffer with explicit endianness.
 *
 * <p>Instances are not thread-safe. The reader has its own cursor and does not mutate
 * the position of a caller-provided {@link ByteBuffer}.</p>
 */
public final class BinaryReader {

    private static final int SMALL_ARRAY_MANUAL_THRESHOLD = 32;

    private final ByteBuffer buffer;

    private BinaryReader(ByteBuffer buffer, Endianness endianness) {
        this.buffer = buffer.slice().order(endianness.byteOrder());
    }

    /**
     * Creates a reader from bytes using big-endian byte order.
     *
     * @param data the bytes to read
     * @return a new reader
     * @throws NullPointerException if data is null
     */
    public static BinaryReader from(byte[] data) {
        return from(data, Endianness.BIG_ENDIAN);
    }

    /**
     * Creates a reader from bytes using the specified endianness.
     *
     * @param data       the bytes to read
     * @param endianness the byte order
     * @return a new reader
     * @throws NullPointerException if data or endianness is null
     */
    public static BinaryReader from(byte[] data, Endianness endianness) {
        Objects.requireNonNull(data, "Data must not be null");
        Objects.requireNonNull(endianness, "Endianness must not be null");
        return new BinaryReader(ByteBuffer.wrap(data), endianness);
    }

    /**
     * Creates a reader from the remaining bytes of a buffer using big-endian byte order.
     *
     * @param buffer the source buffer
     * @return a new reader
     * @throws NullPointerException if buffer is null
     */
    public static BinaryReader from(ByteBuffer buffer) {
        return from(buffer, Endianness.BIG_ENDIAN);
    }

    /**
     * Creates a reader from the remaining bytes of a buffer using the specified endianness.
     *
     * @param buffer     the source buffer
     * @param endianness the byte order
     * @return a new reader
     * @throws NullPointerException if buffer or endianness is null
     */
    public static BinaryReader from(ByteBuffer buffer, Endianness endianness) {
        Objects.requireNonNull(buffer, "ByteBuffer must not be null");
        Objects.requireNonNull(endianness, "Endianness must not be null");
        return new BinaryReader(buffer, endianness);
    }

    /**
     * Reads a byte.
     *
     * @return the byte value
     * @throws BinaryFormatException if EOF is reached
     */
    public byte readByte() {
        require(Byte.BYTES, "byte");
        return buffer.get();
    }

    /**
     * Reads a boolean encoded as 0 or 1.
     *
     * @return the boolean value
     * @throws BinaryFormatException if EOF is reached or the byte is not 0 or 1
     */
    public boolean readBoolean() {
        int offset = position();
        byte value = readByte();
        if (value != 0 && value != 1) {
            throw new BinaryFormatException(offset,
                    "Invalid boolean: expected 0 or 1, got " + (value & 0xFF));
        }
        return value == 1;
    }

    /**
     * Reads a char.
     *
     * @return the char value
     * @throws BinaryFormatException if EOF is reached
     */
    public char readChar() {
        require(Character.BYTES, "char");
        return buffer.getChar();
    }

    /**
     * Reads a short.
     *
     * @return the short value
     * @throws BinaryFormatException if EOF is reached
     */
    public short readShort() {
        require(Short.BYTES, "short");
        return buffer.getShort();
    }

    /**
     * Reads an int.
     *
     * @return the int value
     * @throws BinaryFormatException if EOF is reached
     */
    public int readInt() {
        require(Integer.BYTES, "int");
        return buffer.getInt();
    }

    /**
     * Reads a long.
     *
     * @return the long value
     * @throws BinaryFormatException if EOF is reached
     */
    public long readLong() {
        require(Long.BYTES, "long");
        return buffer.getLong();
    }

    /**
     * Reads a float.
     *
     * @return the float value
     * @throws BinaryFormatException if EOF is reached
     */
    public float readFloat() {
        require(Float.BYTES, "float");
        return buffer.getFloat();
    }

    /**
     * Reads a double.
     *
     * @return the double value
     * @throws BinaryFormatException if EOF is reached
     */
    public double readDouble() {
        require(Double.BYTES, "double");
        return buffer.getDouble();
    }

    /**
     * Reads a byte array.
     *
     * @param count the number of bytes to read
     * @return the bytes
     * @throws BinaryFormatException if count is negative or exceeds remaining bytes
     */
    public byte[] readByteArray(int count) {
        int byteCount = checkedByteCount(count, Byte.BYTES, "byte array");
        byte[] result = new byte[byteCount];
        buffer.get(result);
        return result;
    }

    /**
     * Reads a byte array.
     *
     * @param length the number of bytes to read
     * @return the bytes
     * @throws BinaryFormatException if length is negative or exceeds remaining bytes
     */
    public byte[] readBytes(int length) {
        return readByteArray(length);
    }

    /**
     * Reads bytes into the target array.
     *
     * @param target the target array
     * @throws NullPointerException if target is null
     * @throws BinaryFormatException if EOF is reached
     */
    public void readFully(byte[] target) {
        Objects.requireNonNull(target, "Target array must not be null");
        readFully(target, 0, target.length);
    }

    /**
     * Reads bytes into a target array range.
     *
     * @param target the target array
     * @param offset the target offset
     * @param length the number of bytes to read
     * @throws NullPointerException if target is null
     * @throws BinaryException if offset or length is invalid
     * @throws BinaryFormatException if EOF is reached
     */
    public void readFully(byte[] target, int offset, int length) {
        Objects.requireNonNull(target, "Target array must not be null");
        checkTargetRange(target.length, offset, length);
        require(length, "byte array");
        buffer.get(target, offset, length);
    }

    /**
     * Skips up to the requested number of bytes.
     *
     * <p>This method may skip fewer bytes than requested if the reader reaches EOF.
     * Use {@link #skipFully(long)} when EOF should be treated as malformed input.</p>
     *
     * @param byteCount the requested number of bytes to skip
     * @return the number of bytes actually skipped
     * @throws BinaryException if byteCount is negative
     */
    public long skipBytes(long byteCount) {
        if (byteCount < 0) {
            throw new BinaryException("Skip byte count must be non-negative: got " + byteCount);
        }
        long skipped = Math.min(byteCount, buffer.remaining());
        buffer.position((int) (buffer.position() + skipped));
        return skipped;
    }

    /**
     * Skips exactly the requested number of bytes.
     *
     * @param byteCount the number of bytes to skip
     * @throws BinaryException if byteCount is negative
     * @throws BinaryFormatException if EOF is reached before all bytes are skipped
     */
    public void skipFully(long byteCount) {
        int skip = checkedSkipByteCount(byteCount);
        buffer.position(buffer.position() + skip);
    }

    /**
     * Reads a short array.
     *
     * @param count the number of shorts to read
     * @return the short array
     * @throws BinaryFormatException if count is negative or exceeds remaining bytes
     */
    public short[] readShortArray(int count) {
        int byteCount = checkedByteCount(count, Short.BYTES, "short array");
        short[] result = new short[count];
        if (count <= SMALL_ARRAY_MANUAL_THRESHOLD) {
            buffer.asShortBuffer().get(result);
            buffer.position(buffer.position() + byteCount);
        } else {
            take(byteCount).asShortBuffer().get(result);
        }
        return result;
    }

    /**
     * Reads shorts into the target array.
     *
     * @param target the target array
     * @throws NullPointerException if target is null
     * @throws BinaryFormatException if EOF is reached
     */
    public void readShortArray(short[] target) {
        Objects.requireNonNull(target, "Target array must not be null");
        readShortArray(target, 0, target.length);
    }

    /**
     * Reads shorts into a target array range.
     *
     * @param target the target array
     * @param offset the target offset
     * @param length the number of shorts to read
     * @throws NullPointerException if target is null
     * @throws BinaryException if offset or length is invalid
     * @throws BinaryFormatException if EOF is reached
     */
    public void readShortArray(short[] target, int offset, int length) {
        Objects.requireNonNull(target, "Target array must not be null");
        checkTargetRange(target.length, offset, length);
        int byteCount = checkedTargetByteCount(length, Short.BYTES, "short array");
        if (byteCount == 0) {
            return;
        }
        if (length <= SMALL_ARRAY_MANUAL_THRESHOLD) {
            buffer.asShortBuffer().get(target, offset, length);
            buffer.position(buffer.position() + byteCount);
        } else {
            take(byteCount).asShortBuffer().get(target, offset, length);
        }
    }

    /**
     * Reads an int array.
     *
     * @param count the number of ints to read
     * @return the int array
     * @throws BinaryFormatException if count is negative or exceeds remaining bytes
     */
    public int[] readIntArray(int count) {
        int byteCount = checkedByteCount(count, Integer.BYTES, "int array");
        int[] result = new int[count];
        take(byteCount).asIntBuffer().get(result);
        return result;
    }

    /**
     * Reads ints into the target array.
     *
     * @param target the target array
     * @throws NullPointerException if target is null
     * @throws BinaryFormatException if EOF is reached
     */
    public void readIntArray(int[] target) {
        Objects.requireNonNull(target, "Target array must not be null");
        readIntArray(target, 0, target.length);
    }

    /**
     * Reads ints into a target array range.
     *
     * @param target the target array
     * @param offset the target offset
     * @param length the number of ints to read
     * @throws NullPointerException if target is null
     * @throws BinaryException if offset or length is invalid
     * @throws BinaryFormatException if EOF is reached
     */
    public void readIntArray(int[] target, int offset, int length) {
        Objects.requireNonNull(target, "Target array must not be null");
        checkTargetRange(target.length, offset, length);
        int byteCount = checkedTargetByteCount(length, Integer.BYTES, "int array");
        if (byteCount > 0) {
            take(byteCount).asIntBuffer().get(target, offset, length);
        }
    }

    /**
     * Reads a long array.
     *
     * @param count the number of longs to read
     * @return the long array
     * @throws BinaryFormatException if count is negative or exceeds remaining bytes
     */
    public long[] readLongArray(int count) {
        int byteCount = checkedByteCount(count, Long.BYTES, "long array");
        long[] result = new long[count];
        take(byteCount).asLongBuffer().get(result);
        return result;
    }

    /**
     * Reads longs into the target array.
     *
     * @param target the target array
     * @throws NullPointerException if target is null
     * @throws BinaryFormatException if EOF is reached
     */
    public void readLongArray(long[] target) {
        Objects.requireNonNull(target, "Target array must not be null");
        readLongArray(target, 0, target.length);
    }

    /**
     * Reads longs into a target array range.
     *
     * @param target the target array
     * @param offset the target offset
     * @param length the number of longs to read
     * @throws NullPointerException if target is null
     * @throws BinaryException if offset or length is invalid
     * @throws BinaryFormatException if EOF is reached
     */
    public void readLongArray(long[] target, int offset, int length) {
        Objects.requireNonNull(target, "Target array must not be null");
        checkTargetRange(target.length, offset, length);
        int byteCount = checkedTargetByteCount(length, Long.BYTES, "long array");
        if (byteCount > 0) {
            take(byteCount).asLongBuffer().get(target, offset, length);
        }
    }

    /**
     * Reads a float array.
     *
     * @param count the number of floats to read
     * @return the float array
     * @throws BinaryFormatException if count is negative or exceeds remaining bytes
     */
    public float[] readFloatArray(int count) {
        int byteCount = checkedByteCount(count, Float.BYTES, "float array");
        float[] result = new float[count];
        take(byteCount).asFloatBuffer().get(result);
        return result;
    }

    /**
     * Reads floats into the target array.
     *
     * @param target the target array
     * @throws NullPointerException if target is null
     * @throws BinaryFormatException if EOF is reached
     */
    public void readFloatArray(float[] target) {
        Objects.requireNonNull(target, "Target array must not be null");
        readFloatArray(target, 0, target.length);
    }

    /**
     * Reads floats into a target array range.
     *
     * @param target the target array
     * @param offset the target offset
     * @param length the number of floats to read
     * @throws NullPointerException if target is null
     * @throws BinaryException if offset or length is invalid
     * @throws BinaryFormatException if EOF is reached
     */
    public void readFloatArray(float[] target, int offset, int length) {
        Objects.requireNonNull(target, "Target array must not be null");
        checkTargetRange(target.length, offset, length);
        int byteCount = checkedTargetByteCount(length, Float.BYTES, "float array");
        if (byteCount > 0) {
            take(byteCount).asFloatBuffer().get(target, offset, length);
        }
    }

    /**
     * Reads a double array.
     *
     * @param count the number of doubles to read
     * @return the double array
     * @throws BinaryFormatException if count is negative or exceeds remaining bytes
     */
    public double[] readDoubleArray(int count) {
        int byteCount = checkedByteCount(count, Double.BYTES, "double array");
        double[] result = new double[count];
        take(byteCount).asDoubleBuffer().get(result);
        return result;
    }

    /**
     * Reads doubles into the target array.
     *
     * @param target the target array
     * @throws NullPointerException if target is null
     * @throws BinaryFormatException if EOF is reached
     */
    public void readDoubleArray(double[] target) {
        Objects.requireNonNull(target, "Target array must not be null");
        readDoubleArray(target, 0, target.length);
    }

    /**
     * Reads doubles into a target array range.
     *
     * @param target the target array
     * @param offset the target offset
     * @param length the number of doubles to read
     * @throws NullPointerException if target is null
     * @throws BinaryException if offset or length is invalid
     * @throws BinaryFormatException if EOF is reached
     */
    public void readDoubleArray(double[] target, int offset, int length) {
        Objects.requireNonNull(target, "Target array must not be null");
        checkTargetRange(target.length, offset, length);
        int byteCount = checkedTargetByteCount(length, Double.BYTES, "double array");
        if (byteCount > 0) {
            take(byteCount).asDoubleBuffer().get(target, offset, length);
        }
    }

    /**
     * Reads a char array.
     *
     * @param count the number of chars to read
     * @return the char array
     * @throws BinaryFormatException if count is negative or exceeds remaining bytes
     */
    public char[] readCharArray(int count) {
        int byteCount = checkedByteCount(count, Character.BYTES, "char array");
        char[] result = new char[count];
        take(byteCount).asCharBuffer().get(result);
        return result;
    }

    /**
     * Reads and validates magic bytes.
     *
     * @param expected the expected magic bytes
     * @throws NullPointerException if expected is null
     * @throws BinaryFormatException if EOF is reached or bytes do not match
     */
    public void validateMagic(byte[] expected) {
        Objects.requireNonNull(expected, "Expected magic must not be null");
        int offset = position();
        require(expected.length, "magic");
        boolean matches = true;
        for (byte expectedByte : expected) {
            if (buffer.get() != expectedByte) {
                matches = false;
            }
        }
        if (!matches) {
            byte[] actual = copyBytes(offset, expected.length);
            throw new BinaryFormatException(offset,
                    "Invalid magic: expected " + BinaryFormatException.formatHex(expected)
                            + ", actual " + BinaryFormatException.formatHex(actual));
        }
    }

    /**
     * Reads and validates magic bytes from an ASCII string.
     *
     * @param asciiMagic the expected magic string
     * @throws NullPointerException if asciiMagic is null
     * @throws BinaryFormatException if EOF is reached or bytes do not match
     */
    public void expectMagic(String asciiMagic) {
        Objects.requireNonNull(asciiMagic, "Magic string must not be null");
        if (!isAscii(asciiMagic)) {
            validateMagic(asciiMagic.getBytes(StandardCharsets.US_ASCII));
            return;
        }
        int offset = position();
        int length = asciiMagic.length();
        require(length, "magic");
        boolean matches = true;
        for (int i = 0; i < length; i++) {
            if (buffer.get() != (byte) asciiMagic.charAt(i)) {
                matches = false;
            }
        }
        if (!matches) {
            byte[] actual = copyBytes(offset, length);
            throw new BinaryFormatException(offset,
                    "Invalid magic: expected " + BinaryFormatException.formatHex(asciiBytes(asciiMagic))
                            + ", actual " + BinaryFormatException.formatHex(actual));
        }
    }

    /**
     * Reads a version number.
     *
     * @return the version
     */
    public int readVersion() {
        return readInt();
    }

    /**
     * Reads and validates a version number.
     *
     * @param expected the expected version
     * @throws BinaryFormatException if EOF is reached or the version does not match
     */
    public void validateVersion(int expected) {
        int offset = position();
        int actual = readInt();
        if (actual != expected) {
            throw new BinaryFormatException(offset,
                    "Invalid version: expected " + expected + ", actual " + actual);
        }
    }

    /**
     * Reads and validates a version number.
     *
     * @param expected the expected version
     * @throws BinaryFormatException if EOF is reached or the version does not match
     */
    public void expectVersion(int expected) {
        validateVersion(expected);
    }

    /**
     * Returns the current read position in bytes.
     *
     * @return the position
     */
    public int position() {
        return buffer.position();
    }

    /**
     * Returns the number of remaining bytes.
     *
     * @return remaining bytes
     */
    public int remaining() {
        return buffer.remaining();
    }

    /**
     * Returns whether unread bytes remain.
     *
     * @return true if bytes remain
     */
    public boolean hasRemaining() {
        return buffer.hasRemaining();
    }

    private void require(int bytes, String description) {
        if (bytes < 0 || buffer.remaining() < bytes) {
            throw new BinaryFormatException(position(),
                    "Unexpected EOF: required " + bytes + " bytes for " + description
                            + ", but " + Math.max(0, buffer.remaining()) + " bytes remaining");
        }
    }

    private int checkedByteCount(int count, int elementSize, String description) {
        if (count < 0) {
            throw new BinaryFormatException(position(),
                    "Array count must be non-negative for " + description + ": got " + count);
        }
        long byteCount = (long) count * elementSize;
        if (byteCount > buffer.remaining()) {
            throw new BinaryFormatException(position(),
                    "Array count exceeds remaining bytes for " + description
                            + ": count=" + count + ", required=" + byteCount
                            + " bytes, available=" + buffer.remaining());
        }
        if (byteCount > Integer.MAX_VALUE) {
            throw new BinaryFormatException(position(),
                    "Array byte count exceeds supported size for " + description
                            + ": required=" + byteCount);
        }
        return (int) byteCount;
    }

    private int checkedTargetByteCount(int count, int elementSize, String description) {
        long byteCount = (long) count * elementSize;
        if (byteCount > Integer.MAX_VALUE) {
            throw new BinaryFormatException(position(),
                    "Array byte count exceeds supported size for " + description
                            + ": required=" + byteCount);
        }
        if (byteCount > buffer.remaining()) {
            throw new BinaryFormatException(position(),
                    "Array count exceeds remaining bytes for " + description
                            + ": count=" + count + ", required=" + byteCount
                            + " bytes, available=" + buffer.remaining());
        }
        return (int) byteCount;
    }

    private int checkedSkipByteCount(long byteCount) {
        if (byteCount < 0) {
            throw new BinaryException("Skip byte count must be non-negative: got " + byteCount);
        }
        if (byteCount > buffer.remaining()) {
            throw new BinaryFormatException(position(),
                    "Unexpected EOF: required " + byteCount + " bytes for skip"
                            + ", but " + buffer.remaining() + " bytes remaining");
        }
        return (int) byteCount;
    }

    private static void checkTargetRange(int arrayLength, int offset, int length) {
        if (offset < 0 || length < 0 || offset > arrayLength - length) {
            throw new BinaryException("Invalid offset/length: offset=" + offset
                    + ", length=" + length + ", arrayLength=" + arrayLength);
        }
    }

    private ByteBuffer take(int byteCount) {
        ByteBuffer slice = buffer.slice().order(buffer.order());
        slice.limit(byteCount);
        buffer.position(buffer.position() + byteCount);
        return slice;
    }

    private byte[] copyBytes(int offset, int length) {
        byte[] copy = new byte[length];
        for (int i = 0; i < length; i++) {
            copy[i] = buffer.get(offset + i);
        }
        return copy;
    }

    private static byte[] asciiBytes(String value) {
        byte[] bytes = new byte[value.length()];
        for (int i = 0; i < value.length(); i++) {
            bytes[i] = (byte) value.charAt(i);
        }
        return bytes;
    }

    private static boolean isAscii(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) > 0x7F) {
                return false;
            }
        }
        return true;
    }
}
