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
package org.neo4j.gds.applications;

import org.neo4j.gds.applications.algorithms.centrality.CentralityAlgorithms;
import org.neo4j.gds.applications.algorithms.centrality.CentralityAlgorithmsEstimationModeBusinessFacade;
import org.neo4j.gds.applications.algorithms.centrality.CentralityAlgorithmsMutateModeBusinessFacade;
import org.neo4j.gds.applications.algorithms.centrality.CentralityAlgorithmsStatsModeBusinessFacade;
import org.neo4j.gds.applications.algorithms.centrality.CentralityAlgorithmsStreamModeBusinessFacade;
import org.neo4j.gds.applications.algorithms.centrality.CentralityAlgorithmsWriteModeBusinessFacade;
import org.neo4j.gds.applications.algorithms.machinery.AlgorithmEstimationTemplate;
import org.neo4j.gds.applications.algorithms.machinery.AlgorithmProcessingTemplate;
import org.neo4j.gds.applications.algorithms.machinery.MutateNodeProperty;
import org.neo4j.gds.applications.algorithms.machinery.ProgressTrackerCreator;
import org.neo4j.gds.applications.algorithms.machinery.RequestScopedDependencies;
import org.neo4j.gds.logging.Log;

public final class CentralityApplications {
    private final CentralityAlgorithmsEstimationModeBusinessFacade estimation;
    private final CentralityAlgorithmsMutateModeBusinessFacade mutation;
    private final CentralityAlgorithmsStatsModeBusinessFacade stats;
    private final CentralityAlgorithmsStreamModeBusinessFacade streaming;
    private final CentralityAlgorithmsWriteModeBusinessFacade writing;

    private CentralityApplications(
        CentralityAlgorithmsEstimationModeBusinessFacade estimation,
        CentralityAlgorithmsMutateModeBusinessFacade mutation,
        CentralityAlgorithmsStatsModeBusinessFacade stats,
        CentralityAlgorithmsStreamModeBusinessFacade streaming,
        CentralityAlgorithmsWriteModeBusinessFacade writing
    ) {
        this.estimation = estimation;
        this.mutation = mutation;
        this.stats = stats;
        this.streaming = streaming;
        this.writing = writing;
    }

    static CentralityApplications create(
        Log log,
        RequestScopedDependencies requestScopedDependencies,
        AlgorithmEstimationTemplate estimationTemplate,
        AlgorithmProcessingTemplate processingTemplate,
        ProgressTrackerCreator progressTrackerCreator,
        MutateNodeProperty mutateNodeProperty
    ) {
        var estimation = new CentralityAlgorithmsEstimationModeBusinessFacade(estimationTemplate);
        var algorithms = new CentralityAlgorithms(
            progressTrackerCreator,
            requestScopedDependencies.getTerminationFlag()
        );
        var mutation = new CentralityAlgorithmsMutateModeBusinessFacade(
            estimation,
            algorithms,
            processingTemplate,
            mutateNodeProperty
        );
        var stats = new CentralityAlgorithmsStatsModeBusinessFacade(estimation, algorithms, processingTemplate);
        var streaming = new CentralityAlgorithmsStreamModeBusinessFacade(estimation, algorithms, processingTemplate);
        var writing = CentralityAlgorithmsWriteModeBusinessFacade.create(
            log,
            requestScopedDependencies,
            estimation,
            algorithms,
            processingTemplate
        );

        return new CentralityApplications(estimation, mutation, stats, streaming, writing);
    }

    public CentralityAlgorithmsEstimationModeBusinessFacade estimate() {
        return estimation;
    }

    public CentralityAlgorithmsMutateModeBusinessFacade mutate() {
        return mutation;
    }

    public CentralityAlgorithmsStatsModeBusinessFacade stats() {
        return stats;
    }

    public CentralityAlgorithmsStreamModeBusinessFacade stream() {
        return streaming;
    }

    public CentralityAlgorithmsWriteModeBusinessFacade write() {
        return writing;
    }
}
