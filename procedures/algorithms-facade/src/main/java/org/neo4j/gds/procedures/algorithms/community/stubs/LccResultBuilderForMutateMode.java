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
package org.neo4j.gds.procedures.algorithms.community.stubs;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.applications.algorithms.machinery.AlgorithmProcessingTimings;
import org.neo4j.gds.applications.algorithms.machinery.ResultBuilder;
import org.neo4j.gds.applications.algorithms.metadata.NodePropertiesWritten;
import org.neo4j.gds.procedures.algorithms.community.LocalClusteringCoefficientMutateResult;
import org.neo4j.gds.triangle.LocalClusteringCoefficientMutateConfig;
import org.neo4j.gds.triangle.LocalClusteringCoefficientResult;

import java.util.Optional;

public class LccResultBuilderForMutateMode implements ResultBuilder<LocalClusteringCoefficientMutateConfig, LocalClusteringCoefficientResult, LocalClusteringCoefficientMutateResult, NodePropertiesWritten> {
    @Override
    public LocalClusteringCoefficientMutateResult build(
        Graph graph,
        LocalClusteringCoefficientMutateConfig configuration,
        Optional<LocalClusteringCoefficientResult> result,
        AlgorithmProcessingTimings timings,
        Optional<NodePropertiesWritten> metadata
    ) {
        if (result.isEmpty()) return LocalClusteringCoefficientMutateResult.emptyFrom(timings, configuration.toMap());

        var localClusteringCoefficientResult = result.get();

        return new LocalClusteringCoefficientMutateResult(
            localClusteringCoefficientResult.averageClusteringCoefficient(),
            localClusteringCoefficientResult.localClusteringCoefficients().size(),
            timings.preProcessingMillis,
            timings.computeMillis,
            timings.sideEffectMillis,
            metadata.orElseThrow().value(),
            configuration.toMap()
        );
    }
}
