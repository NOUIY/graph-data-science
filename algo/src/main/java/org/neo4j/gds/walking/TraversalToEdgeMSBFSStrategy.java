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
package org.neo4j.gds.walking;

import org.neo4j.gds.api.properties.relationships.RelationshipIterator;
import org.neo4j.gds.collections.ha.HugeLongArray;
import org.neo4j.gds.msbfs.ANPStrategy;
import org.neo4j.gds.msbfs.BfsConsumer;

final class TraversalToEdgeMSBFSStrategy extends ANPStrategy {
    private final RelationshipIterator[] relationshipIterators;

    TraversalToEdgeMSBFSStrategy(RelationshipIterator[] relationshipIterators, BfsConsumer perNodeAction) {
        super(perNodeAction);
        this.relationshipIterators = relationshipIterators;
    }

    @Override
    protected boolean stopTraversal(boolean hasNext, int depth) {
        return !hasNext || depth >= relationshipIterators.length;
    }

    @Override
    protected void prepareNextVisit(
        RelationshipIterator ignored,
        long nodeVisit,
        long nodeId,
        HugeLongArray nextSet,
        int depth
    ) {
        // pick the layer we got to
        RelationshipIterator relationshipIterator = relationshipIterators[depth];

        // take steps out from node in this layer == with the right rel type
        relationshipIterator.forEachRelationship(
            nodeId,
            (src, tgt) -> {
                nextSet.or(tgt, nodeVisit);
                return true;
            }
        );
    }
}
