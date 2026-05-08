package io.github.arthurhoch.kissbinary;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Read-only memory-mapped binary reader for position-based access to binary files.
 *
 * <p>Instances are not thread-safe. Each read takes an explicit byte offset and does not
 * advance a cursor. Closing releases the underlying file channel; Java 17 does not provide
 * a standard explicit unmap API for the mapped buffer.</p>
 */
public final class MappedBinaryReader implements AutoCloseable {

    private static final int SMALL_ARRAY_MANUAL_THRESHOLD = 32;

    private final MappedByteBuffer mmap;
    private final FileChannel channel;
    private final long fileSize;
    private final Endianness endianness;

    private MappedBinaryReader(Path path, Endianness endianness) throws IOException {
        this.endianness = endianness;
        this.channel = FileChannel.open(path, StandardOpenOption.READ);
        this.fileSize = channel.size();
        if (fileSize > Integer.MAX_VALUE) {
            try {
                channel.close();
            } catch (IOException ignored) {
                // Preserve the primary error: this implementation maps a single <=2 GB region.
            }
            throw new BinaryException("File too large for memory mapping: " + fileSize
                    + " bytes (max " + Integer.MAX_VALUE + ")");
        }
        this.mmap = fileSize > 0
                ? channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize)
                : null;
        if (mmap != null) {
            mmap.order(endianness.byteOrder());
        }
    }

    /**
     * Opens a memory-mapped file using big-endian byte order.
     *
     * @param path the file path
     * @return a new reader
     * @throws NullPointerException if path is null
     * @throws BinaryException if the file cannot be opened or mapped
     */
    public static MappedBinaryReader from(Path path) {
        return from(path, Endianness.BIG_ENDIAN);
    }

    /**
     * Opens a memory-mapped file using the specified endianness.
     *
     * @param path       the file path
     * @param endianness the byte order
     * @return a new reader
     * @throws NullPointerException if path or endianness is null
     * @throws BinaryException if the file cannot be opened or mapped
     */
    public static MappedBinaryReader from(Path path, Endianness endianness) {
        Objects.requireNonNull(path, "Path must not be null");
        Objects.requireNonNull(endianness, "Endianness must not be null");
        try {
            return new MappedBinaryReader(path, endianness);
        } catch (IOException e) {
            throw new BinaryException("Failed to memory-map file: " + path, e);
        }
    }

    /**
     * Reads a byte at the given offset.
     *
     * @param offset the byte offset
     * @return the byte value
     * @throws BinaryFormatException if offset is out of range
     */
    public byte readByte(long offset) {
        checkBounds(offset, Byte.BYTES);
        return mmap.get((int) offset);
    }

    /**
     * Reads a boolean at the given offset.
     *
     * @param offset the byte offset
     * @return the boolean value
     * @throws BinaryFormatException if offset is out of range or byte is not 0 or 1
     */
    public boolean readBoolean(long offset) {
        byte value = readByte(offset);
        if (value != 0 && value != 1) {
            throw new BinaryFormatException(offset,
                    "Invalid boolean: expected 0 or 1, got " + (value & 0xFF));
        }
        return value == 1;
    }

    /**
     * Reads a char at the given offset.
     *
     * @param offset the byte offset
     * @return the char value
     * @throws BinaryFormatException if offset is out of range
     */
    public char readChar(long offset) {
        checkBounds(offset, Character.BYTES);
        return mmap.getChar((int) offset);
    }

    /**
     * Reads a short at the given offset.
     *
     * @param offset the byte offset
     * @return the short value
     * @throws BinaryFormatException if offset is out of range
     */
    public short readShort(long offset) {
        checkBounds(offset, Short.BYTES);
        return mmap.getShort((int) offset);
    }

    /**
     * Reads an int at the given offset.
     *
     * @param offset the byte offset
     * @return the int value
     * @throws BinaryFormatException if offset is out of range
     */
    public int readInt(long offset) {
        checkBounds(offset, Integer.BYTES);
        return mmap.getInt((int) offset);
    }

    /**
     * Reads a long at the given offset.
     *
     * @param offset the byte offset
     * @return the long value
     * @throws BinaryFormatException if offset is out of range
     */
    public long readLong(long offset) {
        checkBounds(offset, Long.BYTES);
        return mmap.getLong((int) offset);
    }

    /**
     * Reads a float at the given offset.
     *
     * @param offset the byte offset
     * @return the float value
     * @throws BinaryFormatException if offset is out of range
     */
    public float readFloat(long offset) {
        checkBounds(offset, Float.BYTES);
        return mmap.getFloat((int) offset);
    }

    /**
     * Reads a double at the given offset.
     *
     * @param offset the byte offset
     * @return the double value
     * @throws BinaryFormatException if offset is out of range
     */
    public double readDouble(long offset) {
        checkBounds(offset, Double.BYTES);
        return mmap.getDouble((int) offset);
    }

    /**
     * Reads a byte array at the given offset.
     *
     * @param offset the byte offset
     * @param count  the number of bytes
     * @return the byte array
     * @throws BinaryFormatException if count is negative or range is out of bounds
     */
    public byte[] readByteArray(long offset, int count) {
        int byteCount = checkedArrayBounds(offset, count, Byte.BYTES, "byte array");
        byte[] result = new byte[byteCount];
        if (byteCount > 0) {
            mmap.get((int) offset, result, 0, byteCount);
        }
        return result;
    }

    /**
     * Reads bytes from the given offset into a target array range.
     *
     * @param offset       the byte offset in the file
     * @param target       the target array
     * @param targetOffset the target offset
     * @param length       the number of bytes
     * @throws NullPointerException if target is null
     * @throws BinaryException if targetOffset or length is invalid
     * @throws BinaryFormatException if the source range is out of bounds
     */
    public void readBytes(long offset, byte[] target, int targetOffset, int length) {
        Objects.requireNonNull(target, "Target array must not be null");
        if (targetOffset < 0 || length < 0 || targetOffset > target.length - length) {
            throw new BinaryException("Invalid target offset/length: targetOffset=" + targetOffset
                    + ", length=" + length + ", arrayLength=" + target.length);
        }
        checkBounds(offset, length);
        if (length > 0) {
            mmap.get((int) offset, target, targetOffset, length);
        }
    }

    /**
     * Reads a short array at the given offset.
     *
     * @param offset the byte offset
     * @param count  the number of shorts
     * @return the short array
     * @throws BinaryFormatException if count is negative or range is out of bounds
     */
    public short[] readShortArray(long offset, int count) {
        int byteCount = checkedArrayBounds(offset, count, Short.BYTES, "short array");
        short[] result = new short[count];
        if (byteCount > 0) {
            if (count <= SMALL_ARRAY_MANUAL_THRESHOLD) {
                int base = (int) offset;
                for (int i = 0; i < count; i++) {
                    result[i] = mmap.getShort(base + i * Short.BYTES);
                }
            } else {
                slice(offset, byteCount).asShortBuffer().get(result);
            }
        }
        return result;
    }

    /**
     * Reads shorts at the given file offset into the target array.
     *
     * @param fileOffset the byte offset in the file
     * @param target     the target array
     * @throws NullPointerException if target is null
     * @throws BinaryFormatException if the source range is out of bounds
     */
    public void readShortArray(long fileOffset, short[] target) {
        Objects.requireNonNull(target, "Target array must not be null");
        readShortArray(fileOffset, target, 0, target.length);
    }

    /**
     * Reads shorts at the given file offset into a target array range.
     *
     * @param fileOffset   the byte offset in the file
     * @param target       the target array
     * @param targetOffset the target offset
     * @param length       the number of shorts to read
     * @throws NullPointerException if target is null
     * @throws BinaryException if targetOffset or length is invalid
     * @throws BinaryFormatException if the source range is out of bounds
     */
    public void readShortArray(long fileOffset, short[] target, int targetOffset, int length) {
        Objects.requireNonNull(target, "Target array must not be null");
        int byteCount = checkedTargetArrayBounds(fileOffset, target.length, targetOffset, length,
                Short.BYTES, "short array");
        if (byteCount == 0) {
            return;
        }
        if (length <= SMALL_ARRAY_MANUAL_THRESHOLD) {
            int base = (int) fileOffset;
            for (int i = 0; i < length; i++) {
                target[targetOffset + i] = mmap.getShort(base + i * Short.BYTES);
            }
        } else {
            slice(fileOffset, byteCount).asShortBuffer().get(target, targetOffset, length);
        }
    }

    /**
     * Reads an int array at the given offset.
     *
     * @param offset the byte offset
     * @param count  the number of ints
     * @return the int array
     * @throws BinaryFormatException if count is negative or range is out of bounds
     */
    public int[] readIntArray(long offset, int count) {
        int byteCount = checkedArrayBounds(offset, count, Integer.BYTES, "int array");
        int[] result = new int[count];
        if (byteCount > 0) {
            if (count <= SMALL_ARRAY_MANUAL_THRESHOLD) {
                int base = (int) offset;
                for (int i = 0; i < count; i++) {
                    result[i] = mmap.getInt(base + i * Integer.BYTES);
                }
            } else {
                slice(offset, byteCount).asIntBuffer().get(result);
            }
        }
        return result;
    }

    /**
     * Reads ints at the given file offset into the target array.
     *
     * @param fileOffset the byte offset in the file
     * @param target     the target array
     * @throws NullPointerException if target is null
     * @throws BinaryFormatException if the source range is out of bounds
     */
    public void readIntArray(long fileOffset, int[] target) {
        Objects.requireNonNull(target, "Target array must not be null");
        readIntArray(fileOffset, target, 0, target.length);
    }

    /**
     * Reads ints at the given file offset into a target array range.
     *
     * @param fileOffset   the byte offset in the file
     * @param target       the target array
     * @param targetOffset the target offset
     * @param length       the number of ints to read
     * @throws NullPointerException if target is null
     * @throws BinaryException if targetOffset or length is invalid
     * @throws BinaryFormatException if the source range is out of bounds
     */
    public void readIntArray(long fileOffset, int[] target, int targetOffset, int length) {
        Objects.requireNonNull(target, "Target array must not be null");
        int byteCount = checkedTargetArrayBounds(fileOffset, target.length, targetOffset, length,
                Integer.BYTES, "int array");
        if (byteCount == 0) {
            return;
        }
        if (length <= SMALL_ARRAY_MANUAL_THRESHOLD) {
            int base = (int) fileOffset;
            for (int i = 0; i < length; i++) {
                target[targetOffset + i] = mmap.getInt(base + i * Integer.BYTES);
            }
        } else {
            slice(fileOffset, byteCount).asIntBuffer().get(target, targetOffset, length);
        }
    }

    /**
     * Reads a long array at the given offset.
     *
     * @param offset the byte offset
     * @param count  the number of longs
     * @return the long array
     * @throws BinaryFormatException if count is negative or range is out of bounds
     */
    public long[] readLongArray(long offset, int count) {
        int byteCount = checkedArrayBounds(offset, count, Long.BYTES, "long array");
        long[] result = new long[count];
        if (byteCount > 0) {
            if (count <= SMALL_ARRAY_MANUAL_THRESHOLD) {
                int base = (int) offset;
                for (int i = 0; i < count; i++) {
                    result[i] = mmap.getLong(base + i * Long.BYTES);
                }
            } else {
                slice(offset, byteCount).asLongBuffer().get(result);
            }
        }
        return result;
    }

    /**
     * Reads longs at the given file offset into the target array.
     *
     * @param fileOffset the byte offset in the file
     * @param target     the target array
     * @throws NullPointerException if target is null
     * @throws BinaryFormatException if the source range is out of bounds
     */
    public void readLongArray(long fileOffset, long[] target) {
        Objects.requireNonNull(target, "Target array must not be null");
        readLongArray(fileOffset, target, 0, target.length);
    }

    /**
     * Reads longs at the given file offset into a target array range.
     *
     * @param fileOffset   the byte offset in the file
     * @param target       the target array
     * @param targetOffset the target offset
     * @param length       the number of longs to read
     * @throws NullPointerException if target is null
     * @throws BinaryException if targetOffset or length is invalid
     * @throws BinaryFormatException if the source range is out of bounds
     */
    public void readLongArray(long fileOffset, long[] target, int targetOffset, int length) {
        Objects.requireNonNull(target, "Target array must not be null");
        int byteCount = checkedTargetArrayBounds(fileOffset, target.length, targetOffset, length,
                Long.BYTES, "long array");
        if (byteCount == 0) {
            return;
        }
        if (length <= SMALL_ARRAY_MANUAL_THRESHOLD) {
            int base = (int) fileOffset;
            for (int i = 0; i < length; i++) {
                target[targetOffset + i] = mmap.getLong(base + i * Long.BYTES);
            }
        } else {
            slice(fileOffset, byteCount).asLongBuffer().get(target, targetOffset, length);
        }
    }

    /**
     * Reads a float array at the given offset.
     *
     * @param offset the byte offset
     * @param count  the number of floats
     * @return the float array
     * @throws BinaryFormatException if count is negative or range is out of bounds
     */
    public float[] readFloatArray(long offset, int count) {
        int byteCount = checkedArrayBounds(offset, count, Float.BYTES, "float array");
        float[] result = new float[count];
        if (byteCount > 0) {
            if (count <= SMALL_ARRAY_MANUAL_THRESHOLD) {
                int base = (int) offset;
                for (int i = 0; i < count; i++) {
                    result[i] = mmap.getFloat(base + i * Float.BYTES);
                }
            } else {
                slice(offset, byteCount).asFloatBuffer().get(result);
            }
        }
        return result;
    }

    /**
     * Reads floats at the given file offset into the target array.
     *
     * @param fileOffset the byte offset in the file
     * @param target     the target array
     * @throws NullPointerException if target is null
     * @throws BinaryFormatException if the source range is out of bounds
     */
    public void readFloatArray(long fileOffset, float[] target) {
        Objects.requireNonNull(target, "Target array must not be null");
        readFloatArray(fileOffset, target, 0, target.length);
    }

    /**
     * Reads floats at the given file offset into a target array range.
     *
     * @param fileOffset   the byte offset in the file
     * @param target       the target array
     * @param targetOffset the target offset
     * @param length       the number of floats to read
     * @throws NullPointerException if target is null
     * @throws BinaryException if targetOffset or length is invalid
     * @throws BinaryFormatException if the source range is out of bounds
     */
    public void readFloatArray(long fileOffset, float[] target, int targetOffset, int length) {
        Objects.requireNonNull(target, "Target array must not be null");
        int byteCount = checkedTargetArrayBounds(fileOffset, target.length, targetOffset, length,
                Float.BYTES, "float array");
        if (byteCount == 0) {
            return;
        }
        if (length <= SMALL_ARRAY_MANUAL_THRESHOLD) {
            int base = (int) fileOffset;
            for (int i = 0; i < length; i++) {
                target[targetOffset + i] = mmap.getFloat(base + i * Float.BYTES);
            }
        } else {
            slice(fileOffset, byteCount).asFloatBuffer().get(target, targetOffset, length);
        }
    }

    /**
     * Reads a double array at the given offset.
     *
     * @param offset the byte offset
     * @param count  the number of doubles
     * @return the double array
     * @throws BinaryFormatException if count is negative or range is out of bounds
     */
    public double[] readDoubleArray(long offset, int count) {
        int byteCount = checkedArrayBounds(offset, count, Double.BYTES, "double array");
        double[] result = new double[count];
        if (byteCount > 0) {
            if (count <= SMALL_ARRAY_MANUAL_THRESHOLD) {
                int base = (int) offset;
                for (int i = 0; i < count; i++) {
                    result[i] = mmap.getDouble(base + i * Double.BYTES);
                }
            } else {
                slice(offset, byteCount).asDoubleBuffer().get(result);
            }
        }
        return result;
    }

    /**
     * Reads doubles at the given file offset into the target array.
     *
     * @param fileOffset the byte offset in the file
     * @param target     the target array
     * @throws NullPointerException if target is null
     * @throws BinaryFormatException if the source range is out of bounds
     */
    public void readDoubleArray(long fileOffset, double[] target) {
        Objects.requireNonNull(target, "Target array must not be null");
        readDoubleArray(fileOffset, target, 0, target.length);
    }

    /**
     * Reads doubles at the given file offset into a target array range.
     *
     * @param fileOffset   the byte offset in the file
     * @param target       the target array
     * @param targetOffset the target offset
     * @param length       the number of doubles to read
     * @throws NullPointerException if target is null
     * @throws BinaryException if targetOffset or length is invalid
     * @throws BinaryFormatException if the source range is out of bounds
     */
    public void readDoubleArray(long fileOffset, double[] target, int targetOffset, int length) {
        Objects.requireNonNull(target, "Target array must not be null");
        int byteCount = checkedTargetArrayBounds(fileOffset, target.length, targetOffset, length,
                Double.BYTES, "double array");
        if (byteCount == 0) {
            return;
        }
        if (length <= SMALL_ARRAY_MANUAL_THRESHOLD) {
            int base = (int) fileOffset;
            for (int i = 0; i < length; i++) {
                target[targetOffset + i] = mmap.getDouble(base + i * Double.BYTES);
            }
        } else {
            slice(fileOffset, byteCount).asDoubleBuffer().get(target, targetOffset, length);
        }
    }

    /**
     * Reads a char array at the given offset.
     *
     * @param offset the byte offset
     * @param count  the number of chars
     * @return the char array
     * @throws BinaryFormatException if count is negative or range is out of bounds
     */
    public char[] readCharArray(long offset, int count) {
        int byteCount = checkedArrayBounds(offset, count, Character.BYTES, "char array");
        char[] result = new char[count];
        if (byteCount > 0) {
            if (count <= SMALL_ARRAY_MANUAL_THRESHOLD) {
                int base = (int) offset;
                for (int i = 0; i < count; i++) {
                    result[i] = mmap.getChar(base + i * Character.BYTES);
                }
            } else {
                slice(offset, byteCount).asCharBuffer().get(result);
            }
        }
        return result;
    }

    /**
     * Validates magic bytes at the given offset.
     *
     * @param offset   the byte offset
     * @param expected the expected magic bytes
     * @throws NullPointerException if expected is null
     * @throws BinaryFormatException if range is out of bounds or bytes do not match
     */
    public void validateMagic(long offset, byte[] expected) {
        Objects.requireNonNull(expected, "Expected magic must not be null");
        checkBounds(offset, expected.length);
        boolean matches = true;
        int base = (int) offset;
        for (int i = 0; i < expected.length; i++) {
            if (mmap.get(base + i) != expected[i]) {
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
     * Validates magic bytes at offset 0 from an ASCII string.
     *
     * @param asciiMagic the expected magic string
     * @throws NullPointerException if asciiMagic is null
     * @throws BinaryFormatException if range is out of bounds or bytes do not match
     */
    public void expectMagic(String asciiMagic) {
        Objects.requireNonNull(asciiMagic, "Magic string must not be null");
        if (!isAscii(asciiMagic)) {
            validateMagic(0, asciiMagic.getBytes(StandardCharsets.US_ASCII));
            return;
        }
        int length = asciiMagic.length();
        checkBounds(0, length);
        boolean matches = true;
        for (int i = 0; i < length; i++) {
            if (mmap.get(i) != (byte) asciiMagic.charAt(i)) {
                matches = false;
            }
        }
        if (!matches) {
            byte[] actual = copyBytes(0, length);
            throw new BinaryFormatException(0,
                    "Invalid magic: expected " + BinaryFormatException.formatHex(asciiBytes(asciiMagic))
                            + ", actual " + BinaryFormatException.formatHex(actual));
        }
    }

    /**
     * Validates a 4-byte int version at the given offset.
     *
     * @param offset   the byte offset
     * @param expected the expected version
     * @throws BinaryFormatException if range is out of bounds or version does not match
     */
    public void validateVersion(long offset, int expected) {
        int actual = readInt(offset);
        if (actual != expected) {
            throw new BinaryFormatException(offset,
                    "Invalid version: expected " + expected + ", actual " + actual);
        }
    }

    /**
     * Returns the mapped file size in bytes.
     *
     * @return the file size
     */
    public long size() {
        return fileSize;
    }

    @Override
    public void close() {
        try {
            channel.close();
        } catch (IOException e) {
            throw new BinaryException("Close failed", e);
        }
    }

    private int checkedArrayBounds(long offset, int count, int elementSize, String description) {
        if (count < 0) {
            throw new BinaryFormatException(offset,
                    "Array count must be non-negative for " + description + ": got " + count);
        }
        long byteCount = (long) count * elementSize;
        checkBounds(offset, byteCount);
        if (byteCount > Integer.MAX_VALUE) {
            throw new BinaryFormatException(offset,
                    "Array byte count exceeds supported size for " + description
                            + ": required=" + byteCount);
        }
        return (int) byteCount;
    }

    private int checkedTargetArrayBounds(long offset, int targetLength, int targetOffset,
                                         int length, int elementSize, String description) {
        checkTargetRange(targetLength, targetOffset, length);
        long byteCount = (long) length * elementSize;
        if (byteCount > Integer.MAX_VALUE) {
            throw new BinaryFormatException(offset,
                    "Array byte count exceeds supported size for " + description
                            + ": required=" + byteCount);
        }
        checkBounds(offset, byteCount);
        return (int) byteCount;
    }

    private void checkBounds(long offset, long bytes) {
        if (offset < 0 || bytes < 0 || offset > fileSize || bytes > fileSize - offset) {
            throw new BinaryFormatException(offset,
                    "Offset out of range: offset=" + offset + ", required=" + bytes
                            + ", fileSize=" + fileSize);
        }
    }

    private ByteBuffer slice(long offset, int bytes) {
        ByteBuffer duplicate = mmap.duplicate().order(endianness.byteOrder());
        duplicate.position((int) offset);
        duplicate.limit((int) offset + bytes);
        return duplicate.slice().order(endianness.byteOrder());
    }

    private byte[] copyBytes(long offset, int length) {
        byte[] copy = new byte[length];
        if (length > 0) {
            mmap.get((int) offset, copy, 0, length);
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

    private static void checkTargetRange(int arrayLength, int targetOffset, int length) {
        if (targetOffset < 0 || length < 0 || targetOffset > arrayLength - length) {
            throw new BinaryException("Invalid target offset/length: targetOffset=" + targetOffset
                    + ", length=" + length + ", arrayLength=" + arrayLength);
        }
    }
}
