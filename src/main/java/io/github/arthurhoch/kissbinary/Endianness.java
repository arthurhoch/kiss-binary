package io.github.arthurhoch.kissbinary;

import java.nio.ByteOrder;

/**
 * Byte order for multi-byte primitive reads and writes.
 *
 * <p>Use {@link #BIG_ENDIAN} for new formats (consistent with Java and network byte order).
 * Use {@link #LITTLE_ENDIAN} when reading existing x86-native formats.</p>
 */
public enum Endianness {

    /** Most-significant byte first (Java/network default). */
    BIG_ENDIAN(ByteOrder.BIG_ENDIAN),

    /** Least-significant byte first (x86 native). */
    LITTLE_ENDIAN(ByteOrder.LITTLE_ENDIAN);

    private final ByteOrder byteOrder;

    Endianness(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
    }

    /**
     * Returns the corresponding {@link ByteOrder} for use with {@link java.nio.ByteBuffer}.
     *
     * @return the Java ByteOrder
     */
    ByteOrder byteOrder() {
        return byteOrder;
    }
}
