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
package org.neo4j.gds.procedures.algorithms.miscellaneous.stubs;

import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.applications.algorithms.machinery.AlgorithmProcessingTimings;
import org.neo4j.gds.applications.algorithms.machinery.ResultBuilder;
import org.neo4j.gds.core.loading.SingleTypeRelationships;
import org.neo4j.gds.indexInverse.InverseRelationshipsConfig;
import org.neo4j.gds.procedures.algorithms.miscellaneous.IndexInverseMutateResult;

import java.util.Map;
import java.util.Optional;

class IndexInverseResultBuilderForMutateMode implements ResultBuilder<InverseRelationshipsConfig, Map<RelationshipType, SingleTypeRelationships>, IndexInverseMutateResult, Void> {
    @Override
    public IndexInverseMutateResult build(
        Graph graph,
        InverseRelationshipsConfig configuration,
        Optional<Map<RelationshipType, SingleTypeRelationships>> result,
        AlgorithmProcessingTimings timings,
        Optional<Void> ignored
    ) {
        if (result.isEmpty()) return IndexInverseMutateResult.emptyFrom(timings, configuration.toMap());

        return new IndexInverseMutateResult(
            timings.preProcessingMillis,
            timings.computeMillis,
            timings.sideEffectMillis,
            0,
            graph.relationshipCount(),
            configuration.toMap()
        );
    }
}