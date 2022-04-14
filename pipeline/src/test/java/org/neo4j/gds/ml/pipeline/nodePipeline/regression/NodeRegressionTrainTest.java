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
package org.neo4j.gds.ml.pipeline.nodePipeline.regression;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.gds.ResourceUtil;
import org.neo4j.gds.TestProgressTracker;
import org.neo4j.gds.compat.Neo4jProxy;
import org.neo4j.gds.core.utils.TerminationFlag;
import org.neo4j.gds.core.utils.progress.EmptyTaskRegistryFactory;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.core.utils.progress.tasks.Tasks;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.Inject;
import org.neo4j.gds.extension.TestGraph;
import org.neo4j.gds.ml.metrics.regression.RegressionMetrics;
import org.neo4j.gds.ml.models.TrainingMethod;
import org.neo4j.gds.ml.models.automl.TunableTrainerConfig;
import org.neo4j.gds.ml.models.linearregression.LinearRegressionData;
import org.neo4j.gds.ml.models.linearregression.LinearRegressionTrainConfig;
import org.neo4j.gds.ml.models.linearregression.LinearRegressionTrainConfigImpl;
import org.neo4j.gds.ml.models.linearregression.LinearRegressor;
import org.neo4j.gds.ml.models.randomforest.RandomForestRegressor;
import org.neo4j.gds.ml.models.randomforest.RandomForestTrainerConfig;
import org.neo4j.gds.ml.models.randomforest.RandomForestTrainerConfigImpl;
import org.neo4j.gds.ml.pipeline.AutoTuningConfigImpl;
import org.neo4j.gds.ml.pipeline.nodePipeline.NodeFeatureStep;
import org.neo4j.gds.ml.pipeline.nodePipeline.NodePropertyPredictionSplitConfigImpl;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.neo4j.gds.assertj.Extractors.keepingFixedNumberOfDecimals;
import static org.neo4j.gds.assertj.Extractors.removingThreadId;
import static org.neo4j.gds.compat.TestLog.INFO;

@GdlExtension
class NodeRegressionTrainTest {

    @GdlGraph
    private static final String DB_QUERY =
        "({scalar: 1.5,     target:   3 })," +
        "({scalar: 2.5,     target:   5 })," +
        "({scalar: 3.5,     target:   7 })," +
        "({scalar: 4.5,     target:   9 })," +
        "({scalar: 5.5,     target:  11 })," +
        "({scalar: 6.5,     target:  13 })," +
        "({scalar: 7.5,     target:  15 })," +
        "({scalar: 8.5,     target:  17 })," +
        "({scalar: 9.5,     target:  19 })," +
        "({scalar: 10.5,    target:  21 })," +
        "({scalar: -5.5,    target: -11 })," +
        "({scalar: -12.5,   target: -25 })," +
        "({scalar: 42.5,    target:  85 }),";

    @Inject
    TestGraph graph;

    @Test
    void trainWithOnlyLR() {
        LinearRegressionTrainConfig candidate1 = LinearRegressionTrainConfig.DEFAULT;
        LinearRegressionTrainConfig candidate2 = LinearRegressionTrainConfigImpl.builder().maxEpochs(5).build();

        var pipeline = new NodeRegressionTrainingPipeline();

        pipeline.addFeatureStep(NodeFeatureStep.of("scalar"));

        pipeline.addTrainerConfig(TrainingMethod.LinearRegression, candidate1);
        pipeline.addTrainerConfig(TrainingMethod.LinearRegression, candidate2);


        NodeRegressionPipelineTrainConfig trainConfig = NodeRegressionPipelineTrainConfigImpl.builder()
            .username("DUMMY")
            .pipeline("DUMMY")
            .graphName("DUMMY")
            .modelName("DUMMY")
            .targetProperty("target")
            .randomSeed(42L)
            .metrics(List.of(RegressionMetrics.MEAN_SQUARED_ERROR.name()))
            .build();

        var nrTrain = NodeRegressionTrain.create(
            graph,
            pipeline,
            trainConfig,
            ProgressTracker.NULL_TRACKER,
            TerminationFlag.RUNNING_TRUE
        );

        NodeRegressionTrainResult result = nrTrain.compute();
        var trainingStatistics = result.trainingStatistics();

        assertThat(result.regressor()).isInstanceOf(LinearRegressor.class);
        assertThat(trainingStatistics.bestParameters().toMap()).isEqualTo(candidate1.toMap());

        var bestMetricData = trainingStatistics.metricsForWinningModel().get(RegressionMetrics.MEAN_SQUARED_ERROR);

        // FIXME change these with actual loss implementation
        assertThat(bestMetricData.outerTrain()).isEqualTo(259.52916, Offset.offset(1e-5));
        assertThat(bestMetricData.test()).isEqualTo(2109.82749,  Offset.offset(1e-5));
    }

    @Test
    void trainWithOnlyRF() {
        var candidate1 = RandomForestTrainerConfig.DEFAULT;
        var candidate2 = RandomForestTrainerConfigImpl.builder().numberOfDecisionTrees(20).build();

        var pipeline = new NodeRegressionTrainingPipeline();

        pipeline.addFeatureStep(NodeFeatureStep.of("scalar"));
        pipeline.addTrainerConfig(TrainingMethod.RandomForest, candidate1);
        pipeline.addTrainerConfig(TrainingMethod.RandomForest, candidate2);

        NodeRegressionPipelineTrainConfig trainConfig = NodeRegressionPipelineTrainConfigImpl.builder()
            .username("DUMMY")
            .pipeline("DUMMY")
            .graphName("DUMMY")
            .modelName("DUMMY")
            .targetProperty("target")
            .randomSeed(42L)
            .metrics(List.of(RegressionMetrics.MEAN_SQUARED_ERROR.name()))
            .build();

        var nrTrain = NodeRegressionTrain.create(
            graph,
            pipeline,
            trainConfig,
            ProgressTracker.NULL_TRACKER,
            TerminationFlag.RUNNING_TRUE
        );

        NodeRegressionTrainResult result = nrTrain.compute();
        var trainingStatistics = result.trainingStatistics();

        assertThat(result.regressor()).isInstanceOf(RandomForestRegressor.class);
        assertThat(trainingStatistics.bestParameters().toMap()).isEqualTo(candidate1.toMap());

        var bestMetricData = trainingStatistics.metricsForWinningModel().get(RegressionMetrics.MEAN_SQUARED_ERROR);

        assertThat(bestMetricData.outerTrain()).isEqualTo(21.36533, Offset.offset(1e-5));
        assertThat(bestMetricData.test()).isEqualTo(1056.95979,  Offset.offset(1e-5));
    }

    @Test
    void trainWithMultipleEvaluationMetrics() {
        var candidate1 = RandomForestTrainerConfig.DEFAULT;
        var candidate2 = LinearRegressionTrainConfig.DEFAULT;

        var pipeline = new NodeRegressionTrainingPipeline();

        pipeline.addFeatureStep(NodeFeatureStep.of("scalar"));
        pipeline.addTrainerConfig(TrainingMethod.RandomForest, candidate1);
        pipeline.addTrainerConfig(TrainingMethod.LinearRegression, candidate2);

        List<RegressionMetrics> evaluationMetrics = List.of(
            RegressionMetrics.MEAN_SQUARED_ERROR,
            RegressionMetrics.MEAN_ABSOLUTE_ERROR,
            RegressionMetrics.ROOT_MEAN_SQUARED_ERROR
        );
        NodeRegressionPipelineTrainConfig trainConfig = NodeRegressionPipelineTrainConfigImpl.builder()
            .username("DUMMY")
            .pipeline("DUMMY")
            .graphName("DUMMY")
            .modelName("DUMMY")
            .targetProperty("target")
            .randomSeed(42L)
            .metrics(evaluationMetrics)
            .build();

        NodeRegressionTrainResult result = NodeRegressionTrain.create(
            graph,
            pipeline,
            trainConfig,
            ProgressTracker.NULL_TRACKER,
            TerminationFlag.RUNNING_TRUE
        ).compute();

        var trainingStatistics = result.trainingStatistics();

        assertThat(trainingStatistics.bestParameters().toMap()).isEqualTo(candidate2.toMap());

        for (RegressionMetrics metric : evaluationMetrics) {
            assertThat(trainingStatistics.getTrainStats(metric)).hasSize(pipeline.numberOfModelSelectionTrials());
            assertThat(trainingStatistics.getValidationStats(metric)).hasSize(pipeline.numberOfModelSelectionTrials());

            var bestMetricData = trainingStatistics.metricsForWinningModel().get(metric);
            assertThat(bestMetricData.outerTrain()).isPositive();
            assertThat(bestMetricData.test()).isPositive();
        }
    }

    @Test
    void logProgressWithRange() {
        int MAX_TRIALS = 2;
        var pipeline = new NodeRegressionTrainingPipeline();

        pipeline.setSplitConfig(NodePropertyPredictionSplitConfigImpl.builder().validationFolds(2).testFraction(0.5D).build());
        pipeline.addFeatureStep(NodeFeatureStep.of("scalar"));

        pipeline.addTrainerConfig(
            TunableTrainerConfig.of(
                Map.of("maxDepth", Map.of("range", List.of(2, 4)), "numberOfDecisionTrees", 5),
                TrainingMethod.RandomForest
            )
        );
        pipeline.setAutoTuningConfig(AutoTuningConfigImpl.builder().maxTrials(MAX_TRIALS).build());

        NodeRegressionPipelineTrainConfig config = NodeRegressionPipelineTrainConfigImpl.builder()
            .username("DUMMY")
            .pipeline("DUMMY")
            .graphName("DUMMY")
            .modelName("DUMMY")
            .targetProperty("target")
            .randomSeed(42L)
            .concurrency(1)
            .metrics(List.of(RegressionMetrics.MEAN_SQUARED_ERROR.name()))
            .build();

        var progressTasks = NodeRegressionTrain.progressTask(pipeline.splitConfig().validationFolds(), MAX_TRIALS);
        var progressTask = Tasks.task("MY CONTEXT", progressTasks);

        var testLog = Neo4jProxy.testLog();
        var progressTracker = new TestProgressTracker(progressTask, testLog, 1, EmptyTaskRegistryFactory.INSTANCE);

        progressTracker.beginSubTask("MY CONTEXT");

        NodeRegressionTrain.create(graph, pipeline, config, progressTracker, TerminationFlag.RUNNING_TRUE).compute();

        progressTracker.endSubTask("MY CONTEXT");

        assertThat(testLog.getMessages(INFO))
            .extracting(removingThreadId())
            .extracting(keepingFixedNumberOfDecimals())
            .containsExactlyElementsOf(ResourceUtil.lines("expectedLogs/node-regression-with-range-log"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4})
    void seededNodeClassification(int concurrency) {
        var pipeline = new NodeRegressionTrainingPipeline();

        pipeline.addFeatureStep(NodeFeatureStep.of("scalar"));
        pipeline.addTrainerConfig(TrainingMethod.LinearRegression, LinearRegressionTrainConfig.DEFAULT);

        var config = NodeRegressionPipelineTrainConfigImpl.builder()
            .graphName("IGNORE")
            .pipeline("IGNORE")
            .username("IGNORE")
            .modelName("model")
            .randomSeed(42L)
            .targetProperty("target")
            .metrics(List.of(RegressionMetrics.MEAN_ABSOLUTE_ERROR))
            .concurrency(concurrency)
            .build();

        Supplier<NodeRegressionTrain> algoSupplier = () -> NodeRegressionTrain.create(
            graph,
            pipeline,
            config,
            ProgressTracker.NULL_TRACKER,
            TerminationFlag.RUNNING_TRUE
        );

        var firstResult = algoSupplier.get().compute();
        var secondResult = algoSupplier.get().compute();

        assertThat(((LinearRegressionData) firstResult.regressor().data()).weights().data())
            .matches(matrix -> matrix.equals(
                ((LinearRegressionData) secondResult.regressor().data()).weights().data(),
                1e-10
            ));
    }
}