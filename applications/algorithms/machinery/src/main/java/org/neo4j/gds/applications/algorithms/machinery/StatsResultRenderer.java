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
package org.neo4j.gds.applications.algorithms.machinery;

import org.neo4j.gds.core.loading.GraphResources;

import java.util.Optional;

class StatsResultRenderer<RESULT_FROM_ALGORITHM, RESULT_TO_CALLER> implements ResultRenderer<RESULT_FROM_ALGORITHM, RESULT_TO_CALLER, Void> {
    private final StatsResultBuilder<RESULT_FROM_ALGORITHM, RESULT_TO_CALLER> statsResultBuilder;

    StatsResultRenderer(StatsResultBuilder<RESULT_FROM_ALGORITHM, RESULT_TO_CALLER> statsResultBuilder) {
        this.statsResultBuilder = statsResultBuilder;
    }

    @Override
    public RESULT_TO_CALLER render(
        GraphResources graphResources,
        Optional<RESULT_FROM_ALGORITHM> result,
        AlgorithmProcessingTimings timings,
        Optional<Void> unused
    ) {
        return statsResultBuilder.build(
            graphResources.graph(),
            result,
            timings
        );
    }
}