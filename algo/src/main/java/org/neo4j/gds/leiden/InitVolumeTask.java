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
package org.neo4j.gds.leiden;

import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.utils.paged.HugeDoubleArray;
import org.neo4j.gds.core.utils.paged.HugeLongArray;
import org.neo4j.gds.core.utils.partition.Partition;

class InitVolumeTask implements Runnable {

    private final Partition partition;
    private final Graph graph;
    private final HugeDoubleArray nodeVolumes;
    private final HugeDoubleArray communityVolumes;
    private final HugeLongArray initialCommunities;

    InitVolumeTask(
        Graph graph,
        HugeDoubleArray nodeVolumes,
        HugeDoubleArray communityVolumes,
        HugeLongArray initialCommunities,
        Partition partition
    ) {
        this.graph = graph;
        this.nodeVolumes = nodeVolumes;
        this.communityVolumes = communityVolumes;
        this.initialCommunities = initialCommunities;
        this.partition = partition;
    }

    @Override
    public void run() {
        long startId = partition.startNode();
        long endId = startId + partition.nodeCount();
        for (long nodeId = startId; nodeId < endId; ++nodeId) {
            var communityId = initialCommunities.get(nodeId);
            final long finalNodeId = nodeId;
            graph.forEachRelationship(nodeId, 1.0, (s, t, w) -> {
                nodeVolumes.addTo(finalNodeId, w);
                communityVolumes.addTo(communityId, w);
                return true;
            });
        }
    }
}
