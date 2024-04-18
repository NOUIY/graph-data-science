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
package org.neo4j.gds.closeness;

import org.neo4j.gds.MutatePropertyComputationResultConsumer;
import org.neo4j.gds.api.properties.nodes.EmptyDoubleNodePropertyValues;
import org.neo4j.gds.core.write.ImmutableNodeProperty;
import org.neo4j.gds.executor.AlgorithmSpec;
import org.neo4j.gds.executor.ComputationResult;
import org.neo4j.gds.executor.ComputationResultConsumer;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.procedures.algorithms.configuration.NewConfigFunction;
import org.neo4j.gds.procedures.centrality.betacloseness.BetaClosenessCentralityMutateResult;
import org.neo4j.gds.result.AbstractResultBuilder;

import java.util.List;
import java.util.stream.Stream;

import static org.neo4j.gds.closeness.ClosenessCentrality.CLOSENESS_DESCRIPTION;
import static org.neo4j.gds.executor.ExecutionMode.MUTATE_NODE_PROPERTY;

@GdsCallable(name = "gds.beta.closeness.mutate", description = CLOSENESS_DESCRIPTION, executionMode = MUTATE_NODE_PROPERTY)
public class BetaClosenessCentralityMutateSpec implements AlgorithmSpec<ClosenessCentrality, ClosenessCentralityResult, ClosenessCentralityMutateConfig, Stream<BetaClosenessCentralityMutateResult>, ClosenessCentralityAlgorithmFactory<ClosenessCentralityMutateConfig>> {

    @Override
    public String name() {
            return "ClosenessCentralityMutate";
    }

    @Override
    public ClosenessCentralityAlgorithmFactory<ClosenessCentralityMutateConfig> algorithmFactory(ExecutionContext executionContext) {
        return new ClosenessCentralityAlgorithmFactory<>();
    }

    @Override
    public NewConfigFunction<ClosenessCentralityMutateConfig> newConfigFunction() {
        return (___, config) -> ClosenessCentralityMutateConfig.of(config);
    }

    @Override
    public ComputationResultConsumer<ClosenessCentrality, ClosenessCentralityResult, ClosenessCentralityMutateConfig, Stream<BetaClosenessCentralityMutateResult>> computationResultConsumer() {
        return new MutatePropertyComputationResultConsumer<>(
            computationResult -> List.of(ImmutableNodeProperty.of(
                computationResult.config().mutateProperty(),
                computationResult.result()
                    .map(ClosenessCentralityResult::nodePropertyValues)

                    .orElse(EmptyDoubleNodePropertyValues.INSTANCE)
            )),
            this::resultBuilder
        );
    }

    private AbstractResultBuilder<BetaClosenessCentralityMutateResult> resultBuilder(
        ComputationResult<ClosenessCentrality, ClosenessCentralityResult, ClosenessCentralityMutateConfig> computationResult,
        ExecutionContext executionContext
    ) {
        var builder = new BetaClosenessCentralityMutateResult.Builder(
            executionContext.returnColumns(),
            computationResult.config().typedConcurrency()
        );

        builder.withMutateProperty(computationResult.config().mutateProperty());
        computationResult.result()
            .ifPresent(result -> builder.withCentralityFunction(result.centralityScoreProvider()));

        return builder;
    }

}
