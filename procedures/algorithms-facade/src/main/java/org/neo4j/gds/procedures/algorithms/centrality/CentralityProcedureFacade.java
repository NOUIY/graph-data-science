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
package org.neo4j.gds.procedures.algorithms.centrality;

import org.neo4j.gds.api.ProcedureReturnColumns;
import org.neo4j.gds.applications.ApplicationsFacade;
import org.neo4j.gds.applications.algorithms.centrality.CentralityAlgorithmsEstimationModeBusinessFacade;
import org.neo4j.gds.applications.algorithms.centrality.CentralityAlgorithmsStreamModeBusinessFacade;
import org.neo4j.gds.applications.algorithms.machinery.MemoryEstimateResult;
import org.neo4j.gds.betweenness.BetweennessCentralityStreamConfig;
import org.neo4j.gds.procedures.algorithms.centrality.stubs.BetweennessCentralityMutateStub;
import org.neo4j.gds.procedures.algorithms.runners.EstimationModeRunner;
import org.neo4j.gds.procedures.algorithms.runners.StreamModeAlgorithmRunner;
import org.neo4j.gds.procedures.algorithms.stubs.GenericStub;

import java.util.Map;
import java.util.stream.Stream;

public final class CentralityProcedureFacade {
    private final BetweennessCentralityMutateStub betweennessCentralityMutateStub;
    private final ApplicationsFacade applicationsFacade;
    private final EstimationModeRunner estimationModeRunner;
    private final StreamModeAlgorithmRunner streamModeRunner;

    private CentralityProcedureFacade(
        BetweennessCentralityMutateStub betweennessCentralityMutateStub,
        ApplicationsFacade applicationsFacade,
        EstimationModeRunner estimationModeRunner,
        StreamModeAlgorithmRunner streamModeRunner
    ) {
        this.betweennessCentralityMutateStub = betweennessCentralityMutateStub;
        this.applicationsFacade = applicationsFacade;
        this.estimationModeRunner = estimationModeRunner;
        this.streamModeRunner = streamModeRunner;
    }

    public static CentralityProcedureFacade create(
        GenericStub genericStub,
        ApplicationsFacade applicationsFacade,
        ProcedureReturnColumns procedureReturnColumns,
        EstimationModeRunner estimationModeRunner,
        StreamModeAlgorithmRunner streamModeAlgorithmRunner
    ) {
        var betweennessCentralityMutateStub = new BetweennessCentralityMutateStub(
            genericStub,
            applicationsFacade,
            procedureReturnColumns
        );

        return new CentralityProcedureFacade(
            betweennessCentralityMutateStub,
            applicationsFacade,
            estimationModeRunner,
            streamModeAlgorithmRunner
        );
    }

    public BetweennessCentralityMutateStub betweennessCentralityMutateStub() {
        return betweennessCentralityMutateStub;
    }

    public Stream<CentralityStreamResult> betweennessCentralityStream(
        String graphName,
        Map<String, Object> configuration
    ) {
        var resultBuilder = new BetweennessCentralityResultBuilderForStreamMode();

        return streamModeRunner.runStreamModeAlgorithm(
            graphName,
            configuration,
            BetweennessCentralityStreamConfig::of,
            resultBuilder,
            streamMode()::betweennessCentrality
        );
    }

    public Stream<MemoryEstimateResult> betweennessCentralityStreamEstimate(
        Object graphNameOrConfiguration,
        Map<String, Object> algorithmConfiguration
    ) {
        var result = estimationModeRunner.runEstimation(
            algorithmConfiguration,
            BetweennessCentralityStreamConfig::of,
            configuration -> estimationMode().betweennessCentrality(
                configuration,
                graphNameOrConfiguration
            )
        );

        return Stream.of(result);
    }

    private CentralityAlgorithmsEstimationModeBusinessFacade estimationMode() {
        return applicationsFacade.centrality().estimate();
    }

    private CentralityAlgorithmsStreamModeBusinessFacade streamMode() {
        return applicationsFacade.centrality().stream();
    }
}