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
package org.neo4j.gds.paths.steiner;

import org.neo4j.gds.BaseProc;
import org.neo4j.gds.executor.MemoryEstimationExecutor;
import org.neo4j.gds.executor.ProcedureExecutor;
import org.neo4j.gds.procedures.algorithms.pathfinding.SteinerMutateResult;
import org.neo4j.gds.results.MemoryEstimateResult;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Internal;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.gds.paths.steiner.Constants.STEINER_DESCRIPTION;
import static org.neo4j.procedure.Mode.READ;

public class SteinerTreeMutateProc extends BaseProc {

    @Procedure(value = "gds.steinerTree.mutate", mode = READ)
    @Description(STEINER_DESCRIPTION)
    public Stream<SteinerMutateResult> mutate(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return new ProcedureExecutor<>(
            new SteinerTreeMutateSpec(),
            executionContext()
        ).compute(graphName, configuration);
    }

    @Procedure(value = "gds.steinerTree.mutate.estimate", mode = READ)
    @Description(ESTIMATE_DESCRIPTION)
    public Stream<MemoryEstimateResult> estimate(
        @Name(value = "graphName") Object graphNameOrConfiguration,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return new MemoryEstimationExecutor<>(
            new SteinerTreeMutateSpec(),
            executionContext(),
            transactionContext()
        ).computeEstimate(graphNameOrConfiguration, configuration);
    }

    @Deprecated
    @Procedure(value = "gds.beta.steinerTree.mutate", mode = READ, deprecatedBy = "gds.steinerTree.mutate")
    @Description(STEINER_DESCRIPTION)
    @Internal
    public Stream<SteinerMutateResult> mutateBeta(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        executionContext()
            .metricsFacade()
            .deprecatedProcedures().called("gds.beta.steinerTree.mutate");

        executionContext()
            .log()
            .warn("Procedure `gds.beta.steinerTree.mutate` has been deprecated, please use `gds.steinerTree.mutate`.");
        return mutate(graphName, configuration);
    }

}
