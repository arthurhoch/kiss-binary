package io.github.arthurhoch.kissbinary.rinha;

import io.github.arthurhoch.kissbinary.BinaryException;
import io.github.arthurhoch.kissbinary.Endianness;

final class RinhaBinaryFormat {

    static final String MAGIC = "KBRN";
    static final int VERSION = 1;
    static final int LOGICAL_DIMENSIONS = 14;
    static final int PHYSICAL_DIMENSIONS = 16;
    static final int HEADER_SIZE = 32;
    static final int DEFAULT_QUANTIZE_SCALE = 10_000;
    static final Endianness ENDIANNESS = Endianness.LITTLE_ENDIAN;

    private RinhaBinaryFormat() {}

    static int labelWordCount(int vectorCount) {
        return (vectorCount + 63) / 64;
    }

    static long vectorDataOffset() {
        return HEADER_SIZE;
    }

    static long vectorOffset(int vectorIndex) {
        return HEADER_SIZE + (long) vectorIndex * PHYSICAL_DIMENSIONS * Short.BYTES;
    }

    static long labelDataOffset(int vectorCount) {
        return HEADER_SIZE + (long) vectorCount * PHYSICAL_DIMENSIONS * Short.BYTES;
    }

    static long expectedFileSize(int vectorCount) {
        return labelDataOffset(vectorCount) + (long) labelWordCount(vectorCount) * Long.BYTES;
    }

    static short quantizeToInt16(double value, int scale) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new BinaryException("Cannot quantize NaN or Infinity to int16");
        }
        long scaled = Math.round(value * scale);
        if (scaled < Short.MIN_VALUE) return Short.MIN_VALUE;
        if (scaled > Short.MAX_VALUE) return Short.MAX_VALUE;
        return (short) scaled;
    }

    static boolean getLabel(long[] labelWords, int vectorIndex) {
        int wordIndex = vectorIndex / 64;
        int bitIndex = vectorIndex % 64;
        return (labelWords[wordIndex] & (1L << bitIndex)) != 0;
    }

    static void setLabel(long[] labelWords, int vectorIndex, boolean fraud) {
        int wordIndex = vectorIndex / 64;
        int bitIndex = vectorIndex % 64;
        if (fraud) {
            labelWords[wordIndex] |= (1L << bitIndex);
        }
    }
}
