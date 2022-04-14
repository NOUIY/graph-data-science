/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gds.ml.models.randomforest;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.gds.core.GraphDimensions;
import org.neo4j.gds.core.utils.mem.MemoryRange;
import org.neo4j.gds.core.utils.paged.HugeDoubleArray;
import org.neo4j.gds.core.utils.paged.HugeLongArray;
import org.neo4j.gds.core.utils.paged.HugeObjectArray;
import org.neo4j.gds.core.utils.paged.ReadOnlyHugeLongArray;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.ml.models.Features;
import org.neo4j.gds.ml.models.FeaturesFactory;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RandomForestRegressorTest {
    private static final long NUM_SAMPLES = 10;

    private final HugeDoubleArray targets = HugeDoubleArray.newArray(NUM_SAMPLES);
    private ReadOnlyHugeLongArray trainSet;
    private Features allFeatureVectors;

    @BeforeEach
    void setup() {
        HugeLongArray mutableTrainSet = HugeLongArray.newArray(NUM_SAMPLES);
        mutableTrainSet.setAll(idx -> idx);
        trainSet = ReadOnlyHugeLongArray.of(mutableTrainSet);

        HugeObjectArray<double[]> featureVectorArray = HugeObjectArray.newArray(double[].class, NUM_SAMPLES);

        featureVectorArray.set(0, new double[]{2.771244718, 1.784783929});
        targets.set(0, 0.1);
        featureVectorArray.set(1, new double[]{1.728571309, 1.169761413});
        targets.set(1, 0.2);
        featureVectorArray.set(2, new double[]{3.678319846, 3.31281357});
        targets.set(2, 0.1);
        featureVectorArray.set(3, new double[]{6.961043357, 2.61995032});
        targets.set(3, 0.3);
        featureVectorArray.set(4, new double[]{6.999208922, 2.209014212});
        targets.set(4, 0.15);

        featureVectorArray.set(5, new double[]{7.497545867, 3.162953546});
        targets.set(5, 4.1);
        featureVectorArray.set(6, new double[]{9.00220326, 3.339047188});
        targets.set(6, 4.0);
        featureVectorArray.set(7, new double[]{7.444542326, 0.476683375});
        targets.set(7, 4.7);
        featureVectorArray.set(8, new double[]{10.12493903, 3.234550982});
        targets.set(8, 3.9);
        featureVectorArray.set(9, new double[]{6.642287351, 3.319983761});
        targets.set(9, 4.5);

        allFeatureVectors = FeaturesFactory.wrap(featureVectorArray);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4})
    void usingOneTree(int concurrency) {
        var randomForestTrainer = new RandomForestRegressorTrainer(
            concurrency,
            RandomForestTrainerConfigImpl
                .builder()
                .maxDepth(1)
                .minSplitSize(2)
                .maxFeaturesRatio(1.0D)
                .numberOfDecisionTrees(1)
                .numberOfSamplesRatio(0.0)
                .build(),
            Optional.of(42L),
            ProgressTracker.NULL_TRACKER
        );

        var randomForestRegressor = randomForestTrainer.train(allFeatureVectors, targets, trainSet);

        var featureVector = new double[]{8.0, 0.0};

        assertThat(randomForestRegressor.predict(featureVector)).isCloseTo(4.175, Offset.offset(0.01D));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4})
    void usingTwentyTrees(int concurrency) {
        var randomForestTrainer = new RandomForestRegressorTrainer(
            concurrency,
            RandomForestTrainerConfigImpl
                .builder()
                .maxDepth(3)
                .minSplitSize(2)
                .numberOfSamplesRatio(0.5D)
                .maxFeaturesRatio(1.0D)
                .numberOfDecisionTrees(20)
                .build(),
            Optional.of(1337L),
            ProgressTracker.NULL_TRACKER
        );

        var randomForestRegressor = randomForestTrainer.train(allFeatureVectors, targets, trainSet);

        var featureVector = new double[]{10.0, 3.2};

        assertThat(randomForestRegressor.predict(featureVector)).isCloseTo(3.273, Offset.offset(0.01D));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4})
    void considerTrainSet(int concurrency) {
        var randomForestTrainer = new RandomForestRegressorTrainer(
            concurrency,
            RandomForestTrainerConfigImpl
                .builder()
                .maxDepth(3)
                .minSplitSize(2)
                .numberOfSamplesRatio(1.0D)
                .maxFeaturesRatio(1.0D)
                .numberOfDecisionTrees(10)
                .build(),
            Optional.of(1337L),
            ProgressTracker.NULL_TRACKER
        );

        HugeLongArray mutableTrainSet = HugeLongArray.newArray(NUM_SAMPLES / 2);
        // Use only target ~0.2 vectors => all predictions should be around there
        mutableTrainSet.setAll(idx -> idx);
        trainSet = ReadOnlyHugeLongArray.of(mutableTrainSet);
        var randomForestRegressor = randomForestTrainer.train(allFeatureVectors, targets, trainSet);

        // target 3.9 example (see setup above)
        var featureVector = new double[]{10.12493903, 3.234550982};

        assertThat(randomForestRegressor.predict(featureVector)).isCloseTo(0.235, Offset.offset(0.01D));
    }

    @Test
    void predictOverheadMemoryEstimation() {
        var estimation = RandomForestRegressor.runtimeOverheadMemoryEstimation();

        assertThat(estimation).isEqualTo(MemoryRange.of(16));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "     6, 100_000, 10, 1,   1, 0.1, 1.0,  4013394, 4824202",
        // Should increase fairly little with more trees if training set big.
        "    10, 100_000, 10, 1,  10, 0.1, 1.0,  4013898, 5715410",
        // Should be capped by number of training examples, despite high max depth.
        " 8_000,     500, 10, 1,   1, 0.1, 1.0,    20954,  164786",
        // Should increase very little when using more features for splits.
        "    10, 100_000, 10, 1,  10, 0.9, 1.0,  4013970, 5715574",
        // Should decrease a lot when sampling fewer training examples per tree.
        "    10, 100_000, 10, 1,  10, 0.1, 0.2,   803898, 1865410",
        // Should almost be x4 when concurrency * 4.
        "    10, 100_000, 10, 4,  10, 0.1, 1.0, 16053648, 20404496",
    })
    void trainMemoryEstimation(
        int maxDepth,
        long numberOfTrainingSamples,
        int featureDimension,
        int concurrency,
        int numTrees,
        double maxFeaturesRatio,
        double numberOfSamplesRatio,
        long expectedMin,
        long expectedMax
    ) {
        var config = RandomForestTrainerConfigImpl.builder()
            .maxDepth(maxDepth)
            .numberOfDecisionTrees(numTrees)
            .maxFeaturesRatio(maxFeaturesRatio)
            .numberOfSamplesRatio(numberOfSamplesRatio)
            .build();
        var estimator = RandomForestRegressorTrainer.memoryEstimation(
            unused -> numberOfTrainingSamples,
            MemoryRange.of(featureDimension),
            config
        );
        // Does not depend on node count, only indirectly so with the size of the training set.
        var estimation = estimator.estimate(GraphDimensions.of(10), concurrency).memoryUsage();

        assertThat(estimation)
            .withFailMessage("Got (%s, %s)", estimation.min, estimation.max)
            .isEqualTo(MemoryRange.of(expectedMin, expectedMax));
    }

    @ParameterizedTest
    @CsvSource(value = {
        // Max should almost scale linearly with numberOfDecisionTrees.
        "     6, 100_000,   1,  2,    96,     5_136",
        "     6, 100_000, 100,  2, 5_640,   509_640",
        // Max should increase with maxDepth when maxDepth limiting factor of trees' sizes.
        "    10, 100_000,   1,  2,    96,    81_936",
        // Max should scale almost inverse linearly with minSplitSize.
        "   800, 100_000,   1,  2,    96, 8_000_016",
        "   800, 100_000,   1, 10,    96, 1_600_016",
    })
    void memoryEstimation(
        int maxDepth,
        long numberOfTrainingSamples,
        int numTrees,
        int minSplitSize,
        long expectedMin,
        long expectedMax
    ) {
        var config = RandomForestTrainerConfigImpl.builder()
            .maxDepth(maxDepth)
            .numberOfDecisionTrees(numTrees)
            .minSplitSize(minSplitSize)
            .build();
        var estimator = RandomForestRegressorData.memoryEstimation(
            unused -> numberOfTrainingSamples,
            config
        );
        // Does not depend on node count, only indirectly so with the size of the training set.
        var estimation = estimator.estimate(GraphDimensions.of(10), 4).memoryUsage();

        assertThat(estimation.min).isEqualTo(expectedMin);
        assertThat(estimation.max).isEqualTo(expectedMax);
    }
}