package io.github.arthurhoch.kissbinary.rinha;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RinhaFullDatasetTest {

    private Path datasetDir;
    private boolean datasetAvailable;

    @BeforeAll
    void checkDataset() {
        String dir = System.getenv("RINHA_DATASET_DIR");
        datasetAvailable = dir != null && !dir.isBlank();
        if (datasetAvailable) {
            datasetDir = Path.of(dir);
            Path referencesGz = datasetDir.resolve("references.json.gz");
            datasetAvailable = Files.isRegularFile(referencesGz);
        }
        if (datasetAvailable) {
            System.out.println("RINHA_DATASET_DIR=" + datasetDir + " — full dataset tests enabled");
        } else {
            System.out.println("RINHA_DATASET_DIR not set or references.json.gz not found — "
                    + "full dataset tests skipped");
        }
    }

    @Test
    void convertRealDataset_writesValidKbin(@TempDir Path dir) throws Exception {
        assumeTrue(datasetAvailable, "RINHA_DATASET_DIR not set or references.json.gz missing");

        Path referencesGz = datasetDir.resolve("references.json.gz");
        Path kbin = dir.resolve("references.kbin");

        RinhaConverter.ConversionResult result =
                RinhaConverter.convertFromJson(referencesGz, kbin, RinhaBinaryFormat.DEFAULT_QUANTIZE_SCALE);
        result.printReport("references.json.gz");

        assertTrue(result.vectorCount() > 0, "Expected positive vector count");
        assertTrue(result.outputSize() > 0, "Expected positive output size");

        byte[] data = Files.readAllBytes(kbin);
        RinhaBinaryValidator.HeaderInfo header = RinhaBinaryValidator.validateHeader(data);

        assertEquals(RinhaBinaryFormat.LOGICAL_DIMENSIONS, header.logicalDimensions());
        assertEquals(RinhaBinaryFormat.PHYSICAL_DIMENSIONS, header.physicalDimensions());
        assertEquals(result.vectorCount(), header.vectorCount());
        assertEquals(RinhaBinaryFormat.expectedFileSize(result.vectorCount()), data.length);
    }

    @Test
    void realDataset_firstAndLastVector_readable(@TempDir Path dir) throws Exception {
        assumeTrue(datasetAvailable, "RINHA_DATASET_DIR not set or references.json.gz missing");

        Path kbin = convertDataset(dir);
        byte[] data = Files.readAllBytes(kbin);
        RinhaBinaryValidator.HeaderInfo header = RinhaBinaryValidator.validateHeader(data);

        short[] firstVec = RinhaBinaryValidator.readVector(data, 0);
        assertEquals(RinhaBinaryFormat.PHYSICAL_DIMENSIONS, firstVec.length);

        short[] lastVec = RinhaBinaryValidator.readVector(data, header.vectorCount() - 1);
        assertEquals(RinhaBinaryFormat.PHYSICAL_DIMENSIONS, lastVec.length);
    }

    @Test
    void realDataset_labelsReadable(@TempDir Path dir) throws Exception {
        assumeTrue(datasetAvailable, "RINHA_DATASET_DIR not set or references.json.gz missing");

        Path kbin = convertDataset(dir);
        byte[] data = Files.readAllBytes(kbin);
        RinhaBinaryValidator.HeaderInfo header = RinhaBinaryValidator.validateHeader(data);

        int fraudCount = RinhaBinaryValidator.countFraudLabels(data, header);
        System.out.println("Fraud labels: " + fraudCount + " / " + header.vectorCount()
                + " (" + String.format("%.2f%%", 100.0 * fraudCount / header.vectorCount()) + ")");
        assertTrue(fraudCount >= 0);
    }

    @Test
    void realDataset_mappedReadMatchesHeap(@TempDir Path dir) throws Exception {
        assumeTrue(datasetAvailable, "RINHA_DATASET_DIR not set or references.json.gz missing");

        Path kbin = convertDataset(dir);
        byte[] data = Files.readAllBytes(kbin);
        RinhaBinaryValidator.validateHeader(data);

        short[] heapVec = RinhaBinaryValidator.readVector(data, 0);
        short[] mmapVec = RinhaBinaryValidator.readVectorMapped(kbin, 0);
        assertArrayEquals(heapVec, mmapVec);
    }

    @Test
    void realDataset_sequentialScan_succeeds(@TempDir Path dir) throws Exception {
        assumeTrue(datasetAvailable, "RINHA_DATASET_DIR not set or references.json.gz missing");

        Path kbin = convertDataset(dir);
        byte[] data = Files.readAllBytes(kbin);
        RinhaBinaryValidator.HeaderInfo header = RinhaBinaryValidator.validateHeader(data);

        long checksum = RinhaBinaryValidator.sequentialChecksum(data, header);
        System.out.println("Sequential checksum: " + checksum);
    }

    @Test
    void realDataset_mappedSequentialScan_succeeds(@TempDir Path dir) throws Exception {
        assumeTrue(datasetAvailable, "RINHA_DATASET_DIR not set or references.json.gz missing");

        Path kbin = convertDataset(dir);
        byte[] data = Files.readAllBytes(kbin);
        RinhaBinaryValidator.HeaderInfo header = RinhaBinaryValidator.validateHeader(data);

        long heapChecksum = RinhaBinaryValidator.sequentialChecksum(data, header);
        long mmapChecksum = RinhaBinaryValidator.sequentialChecksumMapped(kbin, header);
        assertEquals(heapChecksum, mmapChecksum);
    }

    private Path convertDataset(Path dir) throws Exception {
        Path referencesGz = datasetDir.resolve("references.json.gz");
        Path kbin = dir.resolve("references.kbin");
        if (!Files.exists(kbin)) {
            RinhaConverter.convertFromJson(referencesGz, kbin, RinhaBinaryFormat.DEFAULT_QUANTIZE_SCALE);
        }
        return kbin;
    }
}
