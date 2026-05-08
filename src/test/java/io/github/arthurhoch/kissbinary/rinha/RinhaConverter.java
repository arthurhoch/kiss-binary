package io.github.arthurhoch.kissbinary.rinha;

import io.github.arthurhoch.kissbinary.BinaryWriter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

final class RinhaConverter {

    private RinhaConverter() {}

    static ConversionResult convertFromJson(Path jsonGzPath, Path outputPath, int quantizeScale) throws IOException {
        long inputSize = Files.size(jsonGzPath);
        long startTime = System.nanoTime();

        int[] vectorCountHolder = {0};
        long[][] labelWordsHolder = {new long[256]};
        BinaryWriter vectorWriter = BinaryWriter.create(RinhaBinaryFormat.ENDIANNESS);

        try (InputStream fis = Files.newInputStream(jsonGzPath)) {
            RinhaJsonLineParser.parseRecords(fis, (vector, label) -> {
                int idx = vectorCountHolder[0];
                if (vector.length != RinhaBinaryFormat.LOGICAL_DIMENSIONS) {
                    throw new IllegalStateException("Expected " + RinhaBinaryFormat.LOGICAL_DIMENSIONS
                            + " dimensions, got " + vector.length + " at record " + idx);
                }

                for (int d = 0; d < RinhaBinaryFormat.LOGICAL_DIMENSIONS; d++) {
                    vectorWriter.writeShort(RinhaBinaryFormat.quantizeToInt16(vector[d], quantizeScale));
                }
                for (int d = RinhaBinaryFormat.LOGICAL_DIMENSIONS; d < RinhaBinaryFormat.PHYSICAL_DIMENSIONS; d++) {
                    vectorWriter.writeShort((short) 0);
                }

                long[] labelWords = labelWordsHolder[0];
                int wordCount = (idx + 64) / 64 + 1;
                if (wordCount > labelWords.length) {
                    long[] newWords = new long[wordCount * 2];
                    System.arraycopy(labelWords, 0, newWords, 0, labelWords.length);
                    labelWordsHolder[0] = newWords;
                    labelWords = newWords;
                }
                RinhaBinaryFormat.setLabel(labelWords, idx, label != 0);

                vectorCountHolder[0]++;
            });
        }

        int vectorCount = vectorCountHolder[0];
        int labelWordCount = RinhaBinaryFormat.labelWordCount(vectorCount);
        long[] finalLabels = new long[labelWordCount];
        System.arraycopy(labelWordsHolder[0], 0, finalLabels, 0, labelWordCount);

        BinaryWriter headerWriter = BinaryWriter.create(RinhaBinaryFormat.ENDIANNESS);
        headerWriter.writeMagic(RinhaBinaryFormat.MAGIC);
        headerWriter.writeVersion(RinhaBinaryFormat.VERSION);
        headerWriter.writeInt(RinhaBinaryFormat.LOGICAL_DIMENSIONS);
        headerWriter.writeInt(RinhaBinaryFormat.PHYSICAL_DIMENSIONS);
        headerWriter.writeInt(vectorCount);
        headerWriter.writeInt(labelWordCount);
        headerWriter.writeInt(0);
        headerWriter.writeInt(0);

        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(outputPath.toFile()))) {
            headerWriter.writeTo(out);
            vectorWriter.writeTo(out);
            BinaryWriter labelWriter = BinaryWriter.create(RinhaBinaryFormat.ENDIANNESS);
            for (long word : finalLabels) {
                labelWriter.writeLong(word);
            }
            labelWriter.writeTo(out);
        }

        long elapsed = System.nanoTime() - startTime;
        long outputSize = Files.size(outputPath);

        return new ConversionResult(vectorCount, inputSize, outputSize, elapsed);
    }

    static ConversionResult convertFromSynthetic(RinhaSyntheticGenerator.SyntheticDataset dataset,
                                                  Path outputPath, int quantizeScale) throws IOException {
        long startTime = System.nanoTime();
        int vectorCount = dataset.vectorCount();

        BinaryWriter writer = BinaryWriter.create(RinhaBinaryFormat.ENDIANNESS);

        writer.writeMagic(RinhaBinaryFormat.MAGIC);
        writer.writeVersion(RinhaBinaryFormat.VERSION);
        writer.writeInt(RinhaBinaryFormat.LOGICAL_DIMENSIONS);
        writer.writeInt(RinhaBinaryFormat.PHYSICAL_DIMENSIONS);
        writer.writeInt(vectorCount);
        writer.writeInt(RinhaBinaryFormat.labelWordCount(vectorCount));
        writer.writeInt(0);
        writer.writeInt(0);

        for (int i = 0; i < vectorCount; i++) {
            double[] vec = dataset.vectors()[i];
            for (int d = 0; d < RinhaBinaryFormat.LOGICAL_DIMENSIONS; d++) {
                writer.writeShort(RinhaBinaryFormat.quantizeToInt16(vec[d], quantizeScale));
            }
            for (int d = RinhaBinaryFormat.LOGICAL_DIMENSIONS; d < RinhaBinaryFormat.PHYSICAL_DIMENSIONS; d++) {
                writer.writeShort((short) 0);
            }
        }

        long[] labelWords = new long[RinhaBinaryFormat.labelWordCount(vectorCount)];
        for (int i = 0; i < vectorCount; i++) {
            RinhaBinaryFormat.setLabel(labelWords, i, dataset.labels()[i]);
        }
        for (long word : labelWords) {
            writer.writeLong(word);
        }

        Files.createDirectories(outputPath.getParent());
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(outputPath.toFile()))) {
            writer.writeTo(out);
        }

        long elapsed = System.nanoTime() - startTime;
        long outputSize = Files.size(outputPath);

        return new ConversionResult(vectorCount, 0, outputSize, elapsed);
    }

    record ConversionResult(int vectorCount, long inputSize, long outputSize, long elapsedNanos) {
        double elapsedSeconds() {
            return elapsedNanos / 1_000_000_000.0;
        }

        double writeThroughputMBps() {
            if (elapsedNanos == 0) return 0;
            return (outputSize / (1024.0 * 1024.0)) / elapsedSeconds();
        }

        int bytesPerVector() {
            if (vectorCount == 0) return 0;
            return (int) ((outputSize - RinhaBinaryFormat.HEADER_SIZE) / vectorCount);
        }

        void printReport(String source) {
            System.out.println("=== Rinha Conversion Report (" + source + ") ===");
            System.out.println("  Input size:       " + formatBytes(inputSize));
            System.out.println("  Output size:      " + formatBytes(outputSize));
            System.out.println("  Vector count:     " + String.format("%,d", vectorCount));
            System.out.println("  Bytes per vector: " + bytesPerVector());
            System.out.println("  Conversion time:  " + String.format("%.3f", elapsedSeconds()) + " s");
            System.out.println("  Write throughput: " + String.format("%.1f", writeThroughputMBps()) + " MB/s");
        }

        private static String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
}
