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
package org.neo4j.gds.core;

import org.neo4j.gds.LicensingServiceBuilder;
import org.neo4j.gds.core.loading.IdMappingAllocator;
import org.neo4j.gds.core.loading.InternalIdMappingBuilder;

import java.util.Comparator;
import java.util.ServiceLoader;

public final class IdMapBehaviorServiceLoader {

    public static final IdMapBehavior<? extends InternalIdMappingBuilder<? extends IdMappingAllocator>, ? extends IdMappingAllocator> INSTANCE = ServiceLoader.load(IdMapBehavior.class)
        .stream()
        .map(ServiceLoader.Provider::get)
        .max(Comparator.comparing(IdMapBehavior::priority))
        .map(instance -> (IdMapBehavior<? extends InternalIdMappingBuilder<? extends IdMappingAllocator>, ? extends IdMappingAllocator>) instance)
        .orElseThrow(() -> new LinkageError("Could not load " + LicensingServiceBuilder.class + " implementation"));

    private IdMapBehaviorServiceLoader() {}
}
