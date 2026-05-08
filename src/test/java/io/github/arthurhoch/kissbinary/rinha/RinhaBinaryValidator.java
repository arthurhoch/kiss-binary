package io.github.arthurhoch.kissbinary.rinha;

import io.github.arthurhoch.kissbinary.BinaryReader;
import io.github.arthurhoch.kissbinary.BinaryFormatException;
import io.github.arthurhoch.kissbinary.MappedBinaryReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class RinhaBinaryValidator {

    private RinhaBinaryValidator() {}

    static HeaderInfo validateHeader(byte[] data) {
        BinaryReader reader = BinaryReader.from(data, RinhaBinaryFormat.ENDIANNESS);
        reader.expectMagic(RinhaBinaryFormat.MAGIC);
        reader.expectVersion(RinhaBinaryFormat.VERSION);

        int logicalDimensions = reader.readInt();
        int physicalDimensions = reader.readInt();
        int vectorCount = reader.readInt();
        int labelWordCount = reader.readInt();
        int reserved1 = reader.readInt();
        int reserved2 = reader.readInt();

        if (logicalDimensions != RinhaBinaryFormat.LOGICAL_DIMENSIONS) {
            throw new BinaryFormatException(RinhaBinaryFormat.HEADER_SIZE,
                    "Expected logical_dimensions=" + RinhaBinaryFormat.LOGICAL_DIMENSIONS
                            + ", got " + logicalDimensions);
        }
        if (physicalDimensions != RinhaBinaryFormat.PHYSICAL_DIMENSIONS) {
            throw new BinaryFormatException(RinhaBinaryFormat.HEADER_SIZE,
                    "Expected physical_dimensions=" + RinhaBinaryFormat.PHYSICAL_DIMENSIONS
                            + ", got " + physicalDimensions);
        }
        if (vectorCount <= 0) {
            throw new BinaryFormatException(RinhaBinaryFormat.HEADER_SIZE,
                    "Expected vector_count > 0, got " + vectorCount);
        }
        int expectedLabelWords = RinhaBinaryFormat.labelWordCount(vectorCount);
        if (labelWordCount != expectedLabelWords) {
            throw new BinaryFormatException(RinhaBinaryFormat.HEADER_SIZE,
                    "Expected label_word_count=" + expectedLabelWords + ", got " + labelWordCount);
        }

        long expectedSize = RinhaBinaryFormat.expectedFileSize(vectorCount);
        if (data.length != expectedSize) {
            throw new BinaryFormatException(0,
                    "File size mismatch: expected " + expectedSize + " bytes, got " + data.length);
        }

        return new HeaderInfo(logicalDimensions, physicalDimensions, vectorCount,
                labelWordCount, reserved1, reserved2);
    }

    static short[] readVector(byte[] data, int vectorIndex, HeaderInfo header) {
        BinaryReader reader = BinaryReader.from(data, RinhaBinaryFormat.ENDIANNESS);
        long offset = RinhaBinaryFormat.vectorOffset(vectorIndex);
        for (int i = 0; i < offset; i++) {
            reader.readByte();
        }
        return reader.readShortArray(RinhaBinaryFormat.PHYSICAL_DIMENSIONS);
    }

    static short[] readVectorMapped(Path file, int vectorIndex) throws IOException {
        try (MappedBinaryReader mmap = MappedBinaryReader.from(file, RinhaBinaryFormat.ENDIANNESS)) {
            long offset = RinhaBinaryFormat.vectorOffset(vectorIndex);
            return mmap.readShortArray(offset, RinhaBinaryFormat.PHYSICAL_DIMENSIONS);
        }
    }

    static boolean readLabel(byte[] data, int vectorIndex, HeaderInfo header) {
        BinaryReader reader = BinaryReader.from(data, RinhaBinaryFormat.ENDIANNESS);
        long labelOffset = RinhaBinaryFormat.labelDataOffset(header.vectorCount);
        for (long i = 0; i < labelOffset; i++) {
            reader.readByte();
        }
        long[] labelWords = reader.readLongArray(header.labelWordCount);
        return RinhaBinaryFormat.getLabel(labelWords, vectorIndex);
    }

    static boolean readLabelMapped(Path file, int vectorIndex, HeaderInfo header) throws IOException {
        try (MappedBinaryReader mmap = MappedBinaryReader.from(file, RinhaBinaryFormat.ENDIANNESS)) {
            long labelOffset = RinhaBinaryFormat.labelDataOffset(header.vectorCount);
            long[] labelWords = mmap.readLongArray(labelOffset, header.labelWordCount);
            return RinhaBinaryFormat.getLabel(labelWords, vectorIndex);
        }
    }

    static long sequentialChecksum(byte[] data, HeaderInfo header) {
        BinaryReader reader = BinaryReader.from(data, RinhaBinaryFormat.ENDIANNESS);
        reader.readByteArray(RinhaBinaryFormat.HEADER_SIZE);

        long checksum = 0;
        for (int i = 0; i < header.vectorCount; i++) {
            short[] vec = reader.readShortArray(RinhaBinaryFormat.PHYSICAL_DIMENSIONS);
            for (short s : vec) {
                checksum += s;
            }
        }
        return checksum;
    }

    static long sequentialChecksumMapped(Path file, HeaderInfo header) throws IOException {
        try (MappedBinaryReader mmap = MappedBinaryReader.from(file, RinhaBinaryFormat.ENDIANNESS)) {
            long checksum = 0;
            for (int i = 0; i < header.vectorCount; i++) {
                long offset = RinhaBinaryFormat.vectorOffset(i);
                short[] vec = mmap.readShortArray(offset, RinhaBinaryFormat.PHYSICAL_DIMENSIONS);
                for (short s : vec) {
                    checksum += s;
                }
            }
            return checksum;
        }
    }

    static int countFraudLabels(byte[] data, HeaderInfo header) {
        BinaryReader reader = BinaryReader.from(data, RinhaBinaryFormat.ENDIANNESS);
        long labelOffset = RinhaBinaryFormat.labelDataOffset(header.vectorCount);
        reader.readByteArray((int) labelOffset);
        long[] labelWords = reader.readLongArray(header.labelWordCount);

        int fraudCount = 0;
        for (long word : labelWords) {
            fraudCount += Long.bitCount(word);
        }
        return fraudCount;
    }

    record HeaderInfo(int logicalDimensions, int physicalDimensions, int vectorCount,
                      int labelWordCount, int reserved1, int reserved2) {}
}
