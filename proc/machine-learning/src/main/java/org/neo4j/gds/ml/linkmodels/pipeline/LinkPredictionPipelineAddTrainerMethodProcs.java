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
package org.neo4j.gds.ml.linkmodels.pipeline;

import org.neo4j.gds.BaseProc;
import org.neo4j.gds.core.ConfigKeyValidation;
import org.neo4j.gds.ml.api.TrainingMethod;
import org.neo4j.gds.ml.models.automl.TunableTrainerConfig;
import org.neo4j.gds.ml.models.mlp.MLPClassifierTrainConfig;
import org.neo4j.gds.ml.models.randomforest.RandomForestClassifierTrainerConfig;
import org.neo4j.gds.ml.pipeline.PipelineCatalog;
import org.neo4j.gds.ml.pipeline.linkPipeline.LinkPredictionTrainingPipeline;
import org.neo4j.gds.procedures.GraphDataScienceProcedures;
import org.neo4j.gds.procedures.pipelines.PipelineInfoResult;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Internal;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.procedure.Mode.READ;

public class LinkPredictionPipelineAddTrainerMethodProcs extends BaseProc {
    @Context
    public GraphDataScienceProcedures facade;

    @Procedure(name = "gds.beta.pipeline.linkPrediction.addLogisticRegression", mode = READ)
    @Description("Add a logistic regression configuration to the parameter space of the link prediction train pipeline.")
    public Stream<PipelineInfoResult> addLogisticRegression(
        @Name("pipelineName") String pipelineName,
        @Name(value = "config", defaultValue = "{}") Map<String, Object> logisticRegressionClassifierConfig
    ) {
        return facade.pipelines().linkPrediction().addLogisticRegression(pipelineName, logisticRegressionClassifierConfig);
    }

    @Procedure(name = "gds.beta.pipeline.linkPrediction.addRandomForest", mode = READ)
    @Description("Add a random forest configuration to the parameter space of the link prediction train pipeline.")
    public Stream<PipelineInfoResult> addRandomForest(
        @Name("pipelineName") String pipelineName,
        @Name(value = "config") Map<String, Object> randomForestClassifierConfig
    ) {
        var pipeline = PipelineCatalog.getTyped(username(), pipelineName, LinkPredictionTrainingPipeline.class);

        var allowedKeys = RandomForestClassifierTrainerConfig.DEFAULT.configKeys();
        ConfigKeyValidation.requireOnlyKeysFrom(allowedKeys, randomForestClassifierConfig.keySet());

        var tunableTrainerConfig = TunableTrainerConfig.of(randomForestClassifierConfig, TrainingMethod.RandomForestClassification);
        pipeline.addTrainerConfig(
            tunableTrainerConfig
        );

        return Stream.of(PipelineInfoResult.create(pipelineName, pipeline));
    }

    @Procedure(name = "gds.alpha.pipeline.linkPrediction.addRandomForest", mode = READ, deprecatedBy = "gds.beta.pipeline.linkPrediction.addRandomForest")
    @Description("Add a random forest configuration to the parameter space of the link prediction train pipeline.")
    @Internal
    @Deprecated(forRemoval = true)
    public Stream<PipelineInfoResult> addRandomForestAlpha(
        @Name("pipelineName") String pipelineName,
        @Name(value = "config") Map<String, Object> randomForestClassifierConfig
    ) {
        executionContext()
            .metrics()
            .deprecatedProcedures().called("gds.alpha.pipeline.linkPrediction.addRandomForest");
        executionContext()
            .log()
            .warn(
                "Procedure `gds.alpha.pipeline.linkPrediction.addRandomForest` has been deprecated, please use `gds.beta.pipeline.linkPrediction.addRandomForest`.");
        return addRandomForest(pipelineName, randomForestClassifierConfig);
    }

    @Procedure(name = "gds.alpha.pipeline.linkPrediction.addMLP", mode = READ)
    @Description("Add a multilayer perceptron configuration to the parameter space of the link prediction train pipeline.")
    public Stream<PipelineInfoResult> addMLP(
        @Name("pipelineName") String pipelineName,
        @Name(value = "config", defaultValue = "{}") Map<String, Object> mlpClassifierConfig
    ) {
        var pipeline = PipelineCatalog.getTyped(username(), pipelineName, LinkPredictionTrainingPipeline.class);

        var allowedKeys = MLPClassifierTrainConfig.DEFAULT.configKeys();
        ConfigKeyValidation.requireOnlyKeysFrom(allowedKeys, mlpClassifierConfig.keySet());

        pipeline.addTrainerConfig(TunableTrainerConfig.of(mlpClassifierConfig, TrainingMethod.MLPClassification));

        return Stream.of(PipelineInfoResult.create(pipelineName, pipeline));
    }
}
