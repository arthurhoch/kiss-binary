package io.github.arthurhoch.kissbinary.rinha;

import java.util.Random;

final class RinhaSyntheticGenerator {

    private RinhaSyntheticGenerator() {}

    static SyntheticDataset generate(int vectorCount, long seed) {
        Random rng = new Random(seed);
        double[][] vectors = new double[vectorCount][RinhaBinaryFormat.LOGICAL_DIMENSIONS];
        boolean[] labels = new boolean[vectorCount];

        for (int i = 0; i < vectorCount; i++) {
            for (int d = 0; d < RinhaBinaryFormat.LOGICAL_DIMENSIONS; d++) {
                vectors[i][d] = rng.nextDouble() * 2.0 - 1.0;
            }
            labels[i] = rng.nextDouble() < 0.05;
        }

        return new SyntheticDataset(vectors, labels);
    }

    record SyntheticDataset(double[][] vectors, boolean[] labels) {
        int vectorCount() {
            return vectors.length;
        }
    }
}
