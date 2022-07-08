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
import org.neo4j.gds.InspectableTestProgressTracker;
import org.neo4j.gds.ResourceUtil;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.Inject;
import org.neo4j.gds.ml.metrics.regression.RegressionMetrics;
import org.neo4j.gds.ml.models.TrainingMethod;
import org.neo4j.gds.ml.models.automl.TunableTrainerConfig;
import org.neo4j.gds.ml.models.linearregression.LinearRegressionData;
import org.neo4j.gds.ml.models.linearregression.LinearRegressionTrainConfig;
import org.neo4j.gds.ml.models.linearregression.LinearRegressionTrainConfigImpl;
import org.neo4j.gds.ml.models.linearregression.LinearRegressor;
import org.neo4j.gds.ml.models.randomforest.RandomForestRegressor;
import org.neo4j.gds.ml.models.randomforest.RandomForestRegressorTrainerConfig;
import org.neo4j.gds.ml.models.randomforest.RandomForestRegressorTrainerConfigImpl;
import org.neo4j.gds.ml.pipeline.AutoTuningConfigImpl;
import org.neo4j.gds.ml.pipeline.nodePipeline.NodeFeatureProducer;
import org.neo4j.gds.ml.pipeline.nodePipeline.NodeFeatureStep;
import org.neo4j.gds.ml.pipeline.nodePipeline.NodePropertyPredictionSplitConfigImpl;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.neo4j.gds.assertj.Extractors.keepingFixedNumberOfDecimals;
import static org.neo4j.gds.assertj.Extractors.removingThreadId;
import static org.neo4j.gds.compat.TestLog.DEBUG;
import static org.neo4j.gds.compat.TestLog.INFO;

@GdlExtension
class NodeRegressionTrainTest {

    @GdlGraph(idOffset=42)
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
    GraphStore graphStore;

    @Test
    void trainWithOnlyLR() {
        LinearRegressionTrainConfig candidate1 = LinearRegressionTrainConfig.DEFAULT;
        LinearRegressionTrainConfig candidate2 = LinearRegressionTrainConfigImpl.builder().maxEpochs(5).build();

        var pipeline = new NodeRegressionTrainingPipeline();

        pipeline.addFeatureStep(NodeFeatureStep.of("scalar"));

        pipeline.addTrainerConfig(candidate1);
        pipeline.addTrainerConfig(candidate2);


        NodeRegressionPipelineTrainConfig trainConfig = NodeRegressionPipelineTrainConfigImpl.builder()
            .username("DUMMY")
            .pipeline("DUMMY")
            .graphName("DUMMY")
            .modelName("DUMMY")
            .targetProperty("target")
            .randomSeed(42L)
            .metrics(List.of(RegressionMetrics.MEAN_SQUARED_ERROR.name()))
            .build();

        var nrTrain = createWithExecutionContext(graphStore, pipeline, trainConfig, ProgressTracker.NULL_TRACKER);

        NodeRegressionTrainResult result = nrTrain.run();
        var trainingStatistics = result.trainingStatistics();

        assertThat(result.regressor()).isInstanceOf(LinearRegressor.class);
        assertThat(trainingStatistics.bestParameters().toMap()).isEqualTo(candidate1.toMap());

        assertThat(trainingStatistics
            .winningModelOuterTrainMetrics()
            .get(RegressionMetrics.MEAN_SQUARED_ERROR)).isEqualTo(209.59583, Offset.offset(1e-5));
        assertThat(trainingStatistics.winningModelTestMetrics().get(RegressionMetrics.MEAN_SQUARED_ERROR)).isEqualTo(
            1718.22750,
            Offset.offset(1e-5)
        );
    }

    @Test
    void trainWithOnlyRF() {
        var candidate1 = RandomForestRegressorTrainerConfig.DEFAULT;
        var candidate2 = RandomForestRegressorTrainerConfigImpl.builder().numberOfDecisionTrees(20).build();

        var pipeline = new NodeRegressionTrainingPipeline();

        pipeline.addFeatureStep(NodeFeatureStep.of("scalar"));
        pipeline.addTrainerConfig(candidate1);
        pipeline.addTrainerConfig(candidate2);

        NodeRegressionPipelineTrainConfig trainConfig = NodeRegressionPipelineTrainConfigImpl.builder()
            .username("DUMMY")
            .pipeline("DUMMY")
            .graphName("DUMMY")
            .modelName("DUMMY")
            .targetProperty("target")
            .randomSeed(42L)
            .metrics(List.of(RegressionMetrics.MEAN_SQUARED_ERROR.name()))
            .build();

        var nrTrain = createWithExecutionContext(graphStore, pipeline, trainConfig, ProgressTracker.NULL_TRACKER);

        NodeRegressionTrainResult result = nrTrain.run();
        var trainingStatistics = result.trainingStatistics();

        assertThat(result.regressor()).isInstanceOf(RandomForestRegressor.class);
        assertThat(trainingStatistics.bestParameters().toMap()).isEqualTo(candidate2.toMap());



        assertThat(trainingStatistics.winningModelOuterTrainMetrics().get(RegressionMetrics.MEAN_SQUARED_ERROR)).isEqualTo(104.012222, Offset.offset(1e-5));
        assertThat(trainingStatistics.winningModelTestMetrics().get(RegressionMetrics.MEAN_SQUARED_ERROR)).isEqualTo(1107.292499, Offset.offset(1e-5));
    }

    @Test
    void trainWithMultipleEvaluationMetrics() {
        var candidate1 = RandomForestRegressorTrainerConfig.DEFAULT;
        var candidate2 = LinearRegressionTrainConfig.DEFAULT;

        var pipeline = new NodeRegressionTrainingPipeline();

        pipeline.addFeatureStep(NodeFeatureStep.of("scalar"));
        pipeline.addTrainerConfig(candidate1);
        pipeline.addTrainerConfig(candidate2);

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

        NodeRegressionTrainResult result = createWithExecutionContext(graphStore, pipeline, trainConfig, ProgressTracker.NULL_TRACKER).run();

        var trainingStatistics = result.trainingStatistics();

        assertThat(trainingStatistics.bestParameters().toMap()).isEqualTo(candidate1.toMap());

        for (RegressionMetrics metric : evaluationMetrics) {
            assertThat(trainingStatistics.getTrainStats(metric)).hasSize(pipeline.numberOfModelSelectionTrials());
            assertThat(trainingStatistics.getValidationStats(metric)).hasSize(pipeline.numberOfModelSelectionTrials());

            assertThat(trainingStatistics.winningModelOuterTrainMetrics().get(metric)).isPositive();
            assertThat(trainingStatistics.winningModelTestMetrics().get(metric)).isPositive();
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
                TrainingMethod.RandomForestRegression
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

        var progressTask = NodeRegressionTrain.progressTask(pipeline, graphStore.nodeCount());

        var progressTracker = new InspectableTestProgressTracker(progressTask, config.username(), config.jobId());

        createWithExecutionContext(graphStore, pipeline, config, progressTracker).run();

        assertThat(progressTracker.log().getMessages(INFO))
            .extracting(removingThreadId())
            .extracting(keepingFixedNumberOfDecimals(4))
            .containsExactlyElementsOf(ResourceUtil.lines("expectedLogs/node-regression-with-range-log-info"));

        assertThat(progressTracker.log().getMessages(DEBUG))
            .extracting(removingThreadId())
            .extracting(keepingFixedNumberOfDecimals(4))
            .containsExactlyElementsOf(ResourceUtil.lines("expectedLogs/node-regression-with-range-log-debug"));

        progressTracker.assertValidProgressEvolution();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4})
    void seededNodeRegression(int concurrency) {
        var pipeline = new NodeRegressionTrainingPipeline();

        pipeline.addFeatureStep(NodeFeatureStep.of("scalar"));
        pipeline.addTrainerConfig(LinearRegressionTrainConfig.DEFAULT);

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

        Supplier<NodeRegressionTrain> algoSupplier = () -> createWithExecutionContext(graphStore, pipeline, config, ProgressTracker.NULL_TRACKER);

        var firstResult = algoSupplier.get().run();
        var secondResult = algoSupplier.get().run();

        assertThat(((LinearRegressionData) firstResult.regressor().data()).weights().data())
            .matches(matrix -> matrix.equals(
                ((LinearRegressionData) secondResult.regressor().data()).weights().data(),
                1e-10
            ));
    }

    @Test
    void failGivenTooSmallTestSet() {
        var pipeline = new NodeRegressionTrainingPipeline();
        pipeline.featureProperties().addAll(List.of("scalar"));
        pipeline.setSplitConfig(NodePropertyPredictionSplitConfigImpl.builder().testFraction(0.001).build());

        var config = NodeRegressionPipelineTrainConfigImpl.builder()
            .pipeline("")
            .username("myUser")
            .graphName("dummy")
            .modelName("myModel")
            .targetProperty("target")
            .metrics(List.of(RegressionMetrics.MEAN_SQUARED_ERROR))
            .build();

        var nodeFeatureProducer = NodeFeatureProducer.create(graphStore, config, ExecutionContext.EMPTY, ProgressTracker.NULL_TRACKER);

        // we are mostly interested in the fact that the validation method is called
        assertThatThrownBy(() -> NodeRegressionTrain.create(graphStore, pipeline, config, nodeFeatureProducer, ProgressTracker.NULL_TRACKER))
            .hasMessage("The specified `testFraction` is too low for the current graph. The test set would have 0 node(s) but it must have at least 1.");
    }

    static NodeRegressionTrain createWithExecutionContext(
        GraphStore graphStore,
        NodeRegressionTrainingPipeline pipeline,
        NodeRegressionPipelineTrainConfig config,
        ProgressTracker progressTracker
    ) {
        var nodeFeatureProducer = NodeFeatureProducer.create(graphStore, config, ExecutionContext.EMPTY, progressTracker);
        return NodeRegressionTrain.create(
            graphStore,
            pipeline,
            config,
            nodeFeatureProducer,
            progressTracker
        );
    }
}
