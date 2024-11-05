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
package org.neo4j.gds.indexInverse;

import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.applications.algorithms.machinery.AlgorithmLabel;
import org.neo4j.gds.core.utils.progress.tasks.Task;
import org.neo4j.gds.core.utils.progress.tasks.Tasks;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InverseRelationshipsProgressTaskCreator {

    public static Task progressTask(long nodeCount, Collection<RelationshipType> relationshipTypes) {
        List<Task> tasks = relationshipTypes.stream().flatMap(type -> Stream.of(
            Tasks.leaf(
                String.format(Locale.US, "Create inverse relationships of type '%s'", type.name),
                nodeCount
            ),
            Tasks.leaf("Build Adjacency list")
        )).collect(Collectors.toList());
        return Tasks.task(AlgorithmLabel.IndexInverse.asString(), tasks);

    }
}