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
package org.neo4j.gds.procedures.algorithms.community;

import org.neo4j.gds.applications.algorithms.machinery.AlgorithmProcessingTimings;

import java.util.Map;

public class K1ColoringWriteResult {
    public final long preProcessingMillis;
    public final long computeMillis;
    public final long writeMillis;

    public final long nodeCount;
    public final long colorCount;
    public final long ranIterations;
    public final boolean didConverge;

    public Map<String, Object> configuration;

    public K1ColoringWriteResult(
        long preProcessingMillis,
        long computeMillis,
        long writeMillis,
        long nodeCount,
        long colorCount,
        long ranIterations,
        boolean didConverge,
        Map<String, Object> configuration
    ) {
        this.preProcessingMillis = preProcessingMillis;
        this.computeMillis = computeMillis;
        this.writeMillis = writeMillis;
        this.nodeCount = nodeCount;
        this.colorCount = colorCount;
        this.ranIterations = ranIterations;
        this.didConverge = didConverge;
        this.configuration = configuration;
    }

    static K1ColoringWriteResult emptyFrom(AlgorithmProcessingTimings timings, Map<String, Object> configurationMap) {
        return new K1ColoringWriteResult(
            timings.preProcessingMillis,
            timings.computeMillis,
            timings.sideEffectMillis,
            0,
            0,
            0,
            false,
            configurationMap
        );
    }
}
