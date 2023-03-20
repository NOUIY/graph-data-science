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
package org.neo4j.gds.core.compression.common;

import org.neo4j.gds.api.compress.AdjacencyCompressor;
import org.neo4j.gds.api.compress.AdjacencyListBuilder;
import org.neo4j.gds.api.compress.LongArrayBuffer;

public final class ThreadLocalRelationshipsCompressor implements AutoCloseable {

    private final AdjacencyCompressor adjacencyCompressor;

    public ThreadLocalRelationshipsCompressor(AdjacencyCompressor adjacencyCompressor) {
        this.adjacencyCompressor = adjacencyCompressor;
    }

    public int applyVariableDeltaEncoding(
        long nodeId,
        byte[] targets,
        long[][] properties,
        int numberOfCompressedTargets,
        int compressedBytesSize,
        LongArrayBuffer buffer,
        AdjacencyListBuilder.Slice<byte[]> adjacencySlice,
        AdjacencyListBuilder.Slice<long[]> propertySlice,
        AdjacencyCompressor.ValueMapper mapper
    ) {
        return adjacencyCompressor.compress(
            nodeId,
            targets,
            properties,
            numberOfCompressedTargets,
            compressedBytesSize,
            buffer,
            adjacencySlice,
            propertySlice,
            mapper
        );
    }

    @Override
    public void close() {
        adjacencyCompressor.close();
    }
}
