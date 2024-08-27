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
package org.neo4j.gds.compat;

import org.neo4j.configuration.Config;
import org.neo4j.gds.compat.batchimport.BatchImporter;
import org.neo4j.gds.compat.batchimport.ExecutionMonitor;
import org.neo4j.gds.compat.batchimport.ImportConfig;
import org.neo4j.gds.compat.batchimport.Monitor;
import org.neo4j.gds.compat.batchimport.input.Collector;
import org.neo4j.gds.compat.batchimport.input.Estimates;
import org.neo4j.gds.compat.batchimport.input.ReadableGroups;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.io.layout.DatabaseLayout;
import org.neo4j.logging.internal.LogService;
import org.neo4j.scheduler.JobScheduler;

import java.io.OutputStream;
import java.util.function.LongConsumer;

public interface Neo4jProxyApi {

    @CompatSince(minor = 23)
    BatchImporter instantiateBlockBatchImporter(
        DatabaseLayout dbLayout,
        FileSystemAbstraction fileSystem,
        ImportConfig config,
        Monitor monitor,
        LogService logService,
        Config dbConfig,
        JobScheduler jobScheduler,
        Collector badCollector
    );

    @CompatSince(minor = 23)
    BatchImporter instantiateRecordBatchImporter(
        DatabaseLayout directoryStructure,
        FileSystemAbstraction fileSystem,
        ImportConfig config,
        ExecutionMonitor executionMonitor,
        LogService logService,
        Config dbConfig,
        JobScheduler jobScheduler,
        Collector badCollector
    );

    @CompatSince(minor = 23)
    default ExecutionMonitor newCoarseBoundedProgressExecutionMonitor(
        long highNodeId,
        long highRelationshipId,
        int batchSize,
        LongConsumer progress,
        LongConsumer outNumberOfBatches
    ) {
        throw new UnsupportedOperationException(
            "`org.neo4j.gds.compat._519.Neo4jProxyApi.newCoarseBoundedProgressExecutionMonitor` is not yet implemented.");
    }

    @CompatSince(minor = 23)
    ReadableGroups newGroups();

    @CompatSince(minor = 23)
    ReadableGroups newInitializedGroups();

    @CompatSince(minor = 23)
    Collector emptyCollector();

    @CompatSince(minor = 23)
    Collector badCollector(OutputStream outputStream, int batchSize);

    @CompatSince(minor = 23)
    Estimates knownEstimates(
        long numberOfNodes,
        long numberOfRelationships,
        long numberOfNodeProperties,
        long numberOfRelationshipProperties,
        long sizeOfNodeProperties,
        long sizeOfRelationshipProperties,
        long numberOfNodeLabels
    );
}
