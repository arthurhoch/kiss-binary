package io.github.arthurhoch.kissbinary.rinha;

import io.github.arthurhoch.kissbinary.BinaryFormatException;
import io.github.arthurhoch.kissbinary.BinaryWriter;
import io.github.arthurhoch.kissbinary.Endianness;
import io.github.arthurhoch.kissbinary.MappedBinaryReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RinhaSyntheticDatasetTest {

    private static final int VECTOR_COUNT = 1000;
    private static final long SEED = 42L;

    private Path createSyntheticKbin(Path dir) throws Exception {
        Path kbin = dir.resolve("references.kbin");
        RinhaSyntheticGenerator.SyntheticDataset dataset =
                RinhaSyntheticGenerator.generate(VECTOR_COUNT, SEED);
        RinhaConverter.ConversionResult result =
                RinhaConverter.convertFromSynthetic(dataset, kbin, RinhaBinaryFormat.DEFAULT_QUANTIZE_SCALE);
        result.printReport("synthetic-" + VECTOR_COUNT);
        return kbin;
    }

    @Test
    void writeAndReadSynthetic_roundtrip_headerValid(@TempDir Path dir) throws Exception {
        Path kbin = createSyntheticKbin(dir);
        byte[] data = Files.readAllBytes(kbin);

        RinhaBinaryValidator.HeaderInfo header = RinhaBinaryValidator.validateHeader(data);
        assertEquals(RinhaBinaryFormat.LOGICAL_DIMENSIONS, header.logicalDimensions());
        assertEquals(RinhaBinaryFormat.PHYSICAL_DIMENSIONS, header.physicalDimensions());
        assertEquals(VECTOR_COUNT, header.vectorCount());
        assertEquals(RinhaBinaryFormat.labelWordCount(VECTOR_COUNT), header.labelWordCount());
    }

    @Test
    void fileSize_matchesExpectedLayout(@TempDir Path dir) throws Exception {
        Path kbin = createSyntheticKbin(dir);
        long expectedSize = RinhaBinaryFormat.expectedFileSize(VECTOR_COUNT);
        assertEquals(expectedSize, Files.size(kbin));
    }

    @Test
    void firstAndLastVector_readable(@TempDir Path dir) throws Exception {
        Path kbin = createSyntheticKbin(dir);
        byte[] data = Files.readAllBytes(kbin);
        RinhaBinaryValidator.HeaderInfo header = RinhaBinaryValidator.validateHeader(data);

        short[] firstVec = RinhaBinaryValidator.readVector(data, 0, header);
        assertEquals(RinhaBinaryFormat.PHYSICAL_DIMENSIONS, firstVec.length);
        assertEquals(0, firstVec[RinhaBinaryFormat.LOGICAL_DIMENSIONS]);
        assertEquals(0, firstVec[RinhaBinaryFormat.LOGICAL_DIMENSIONS + 1]);

        short[] lastVec = RinhaBinaryValidator.readVector(data, VECTOR_COUNT - 1, header);
        assertEquals(RinhaBinaryFormat.PHYSICAL_DIMENSIONS, lastVec.length);
        assertEquals(0, lastVec[RinhaBinaryFormat.LOGICAL_DIMENSIONS]);
        assertEquals(0, lastVec[RinhaBinaryFormat.LOGICAL_DIMENSIONS + 1]);
    }

    @Test
    void firstAndLastLabel_readable(@TempDir Path dir) throws Exception {
        Path kbin = createSyntheticKbin(dir);
        byte[] data = Files.readAllBytes(kbin);
        RinhaBinaryValidator.HeaderInfo header = RinhaBinaryValidator.validateHeader(data);

        RinhaSyntheticGenerator.SyntheticDataset dataset =
                RinhaSyntheticGenerator.generate(VECTOR_COUNT, SEED);

        boolean firstLabel = RinhaBinaryValidator.readLabel(data, 0, header);
        assertEquals(dataset.labels()[0], firstLabel);

        boolean lastLabel = RinhaBinaryValidator.readLabel(data, VECTOR_COUNT - 1, header);
        assertEquals(dataset.labels()[VECTOR_COUNT - 1], lastLabel);
    }

    @Test
    void mappedReader_firstVector_matches(@TempDir Path dir) throws Exception {
        Path kbin = createSyntheticKbin(dir);
        byte[] data = Files.readAllBytes(kbin);
        RinhaBinaryValidator.HeaderInfo header = RinhaBinaryValidator.validateHeader(data);

        short[] expected = RinhaBinaryValidator.readVector(data, 0, header);
        short[] actual = RinhaBinaryValidator.readVectorMapped(kbin, 0);
        assertArrayEquals(expected, actual);
    }

    @Test
    void mappedReader_randomOffset_matches(@TempDir Path dir) throws Exception {
        Path kbin = createSyntheticKbin(dir);
        byte[] data = Files.readAllBytes(kbin);
        RinhaBinaryValidator.HeaderInfo header = RinhaBinaryValidator.validateHeader(data);

        int[] testIndices = {0, 1, 42, 499, 500, 999, VECTOR_COUNT - 1};
        for (int idx : testIndices) {
            short[] expected = RinhaBinaryValidator.readVector(data, idx, header);
            short[] actual = RinhaBinaryValidator.readVectorMapped(kbin, idx);
            assertArrayEquals(expected, actual, "Mismatch at vector index " + idx);
        }
    }

    @Test
    void mappedReader_label_matches(@TempDir Path dir) throws Exception {
        Path kbin = createSyntheticKbin(dir);
        byte[] data = Files.readAllBytes(kbin);
        RinhaBinaryValidator.HeaderInfo header = RinhaBinaryValidator.validateHeader(data);

        for (int i = 0; i < VECTOR_COUNT; i += 100) {
            boolean expected = RinhaBinaryValidator.readLabel(data, i, header);
            boolean actual = RinhaBinaryValidator.readLabelMapped(kbin, i, header);
            assertEquals(expected, actual, "Label mismatch at vector " + i);
        }
    }

    @Test
    void truncatedFile_detected(@TempDir Path dir) throws Exception {
        Path kbin = createSyntheticKbin(dir);
        byte[] data = Files.readAllBytes(kbin);
        byte[] truncated = new byte[data.length / 2];
        System.arraycopy(data, 0, truncated, 0, truncated.length);

        assertThrows(BinaryFormatException.class, () -> RinhaBinaryValidator.validateHeader(truncated));
    }

    @Test
    void magicMismatch_detected(@TempDir Path dir) throws Exception {
        Path kbin = createSyntheticKbin(dir);
        byte[] data = Files.readAllBytes(kbin);
        data[0] = 'X';
        data[1] = 'X';

        assertThrows(BinaryFormatException.class, () -> RinhaBinaryValidator.validateHeader(data));
    }

    @Test
    void versionMismatch_detected(@TempDir Path dir) throws Exception {
        Path kbin = createSyntheticKbin(dir);
        byte[] data = Files.readAllBytes(kbin);
        BinaryWriter patcher = BinaryWriter.create(RinhaBinaryFormat.ENDIANNESS);
        patcher.writeMagic(RinhaBinaryFormat.MAGIC);
        patcher.writeVersion(99);
        byte[] badVersion = patcher.toByteArray();
        System.arraycopy(badVersion, 0, data, 0, badVersion.length);

        assertThrows(BinaryFormatException.class, () -> RinhaBinaryValidator.validateHeader(data));
    }

    @Test
    void sequentialChecksum_nonZero(@TempDir Path dir) throws Exception {
        Path kbin = createSyntheticKbin(dir);
        byte[] data = Files.readAllBytes(kbin);
        RinhaBinaryValidator.HeaderInfo header = RinhaBinaryValidator.validateHeader(data);

        long checksum = RinhaBinaryValidator.sequentialChecksum(data, header);
        assertNotEquals(0, checksum);
    }

    @Test
    void sequentialChecksumMapped_matchesHeap(@TempDir Path dir) throws Exception {
        Path kbin = createSyntheticKbin(dir);
        byte[] data = Files.readAllBytes(kbin);
        RinhaBinaryValidator.HeaderInfo header = RinhaBinaryValidator.validateHeader(data);

        long heapChecksum = RinhaBinaryValidator.sequentialChecksum(data, header);
        long mappedChecksum = RinhaBinaryValidator.sequentialChecksumMapped(kbin, header);
        assertEquals(heapChecksum, mappedChecksum);
    }

    @Test
    void fraudLabelCount_positive(@TempDir Path dir) throws Exception {
        Path kbin = createSyntheticKbin(dir);
        byte[] data = Files.readAllBytes(kbin);
        RinhaBinaryValidator.HeaderInfo header = RinhaBinaryValidator.validateHeader(data);

        int fraudCount = RinhaBinaryValidator.countFraudLabels(data, header);
        assertTrue(fraudCount > 0, "Expected some fraud labels in synthetic dataset with 5%% fraud rate");
    }

    @Test
    void quantize_clamps() {
        short high = RinhaBinaryFormat.quantizeToInt16(1e9, RinhaBinaryFormat.DEFAULT_QUANTIZE_SCALE);
        assertEquals(Short.MAX_VALUE, high);

        short low = RinhaBinaryFormat.quantizeToInt16(-1e9, RinhaBinaryFormat.DEFAULT_QUANTIZE_SCALE);
        assertEquals(Short.MIN_VALUE, low);
    }

    @Test
    void quantize_rejectsNaN() {
        assertThrows(Exception.class,
                () -> RinhaBinaryFormat.quantizeToInt16(Double.NaN, RinhaBinaryFormat.DEFAULT_QUANTIZE_SCALE));
    }

    @Test
    void quantize_rejectsInfinity() {
        assertThrows(Exception.class,
                () -> RinhaBinaryFormat.quantizeToInt16(Double.POSITIVE_INFINITY,
                        RinhaBinaryFormat.DEFAULT_QUANTIZE_SCALE));
    }

    @Test
    void conversionResult_reportsMetrics(@TempDir Path dir) throws Exception {
        Path kbin = dir.resolve("test.kbin");
        RinhaSyntheticGenerator.SyntheticDataset dataset =
                RinhaSyntheticGenerator.generate(100, SEED);
        RinhaConverter.ConversionResult result =
                RinhaConverter.convertFromSynthetic(dataset, kbin, RinhaBinaryFormat.DEFAULT_QUANTIZE_SCALE);

        assertEquals(100, result.vectorCount());
        assertTrue(result.outputSize() > 0);
        assertTrue(result.elapsedNanos() >= 0);
        assertEquals(RinhaBinaryFormat.PHYSICAL_DIMENSIONS * Short.BYTES, result.bytesPerVector());
    }
}
