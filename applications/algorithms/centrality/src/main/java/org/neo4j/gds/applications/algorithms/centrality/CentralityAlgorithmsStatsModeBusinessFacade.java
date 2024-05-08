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
package org.neo4j.gds.applications.algorithms.centrality;

import org.neo4j.gds.algorithms.centrality.CentralityAlgorithmResult;
import org.neo4j.gds.api.GraphName;
import org.neo4j.gds.applications.algorithms.machinery.AlgorithmProcessingTemplate;
import org.neo4j.gds.applications.algorithms.machinery.ResultBuilder;
import org.neo4j.gds.betweenness.BetweennessCentralityStatsConfig;
import org.neo4j.gds.closeness.ClosenessCentralityStatsConfig;

import java.util.Optional;

import static org.neo4j.gds.applications.algorithms.centrality.AlgorithmLabels.BETWEENNESS_CENTRALITY;
import static org.neo4j.gds.applications.algorithms.centrality.AlgorithmLabels.CLOSENESS_CENTRALITY;

public class CentralityAlgorithmsStatsModeBusinessFacade {
    private final CentralityAlgorithmsEstimationModeBusinessFacade estimationFacade;
    private final CentralityAlgorithms centralityAlgorithms;
    private final AlgorithmProcessingTemplate algorithmProcessingTemplate;

    public CentralityAlgorithmsStatsModeBusinessFacade(
        CentralityAlgorithmsEstimationModeBusinessFacade estimationFacade,
        CentralityAlgorithms centralityAlgorithms,
        AlgorithmProcessingTemplate algorithmProcessingTemplate
    ) {
        this.estimationFacade = estimationFacade;
        this.centralityAlgorithms = centralityAlgorithms;
        this.algorithmProcessingTemplate = algorithmProcessingTemplate;
    }

    public <RESULT> RESULT betweennessCentrality(
        GraphName graphName,
        BetweennessCentralityStatsConfig configuration,
        ResultBuilder<BetweennessCentralityStatsConfig, CentralityAlgorithmResult, RESULT, Void> resultBuilder
    ) {
        return algorithmProcessingTemplate.processAlgorithm(
            graphName,
            configuration,
            BETWEENNESS_CENTRALITY,
            () -> estimationFacade.betweennessCentrality(configuration),
            graph -> centralityAlgorithms.betweennessCentrality(graph, configuration),
            Optional.empty(),
            resultBuilder
        );
    }

    public <RESULT> RESULT closenessCentrality(
        GraphName graphName,
        ClosenessCentralityStatsConfig configuration,
        ResultBuilder<ClosenessCentralityStatsConfig, CentralityAlgorithmResult, RESULT, Void> resultBuilder
    ) {
        return algorithmProcessingTemplate.processAlgorithm(
            graphName,
            configuration,
            CLOSENESS_CENTRALITY,
            () -> estimationFacade.closenessCentrality(configuration),
            graph -> centralityAlgorithms.closenessCentrality(graph, configuration),
            Optional.empty(),
            resultBuilder
        );
    }
}