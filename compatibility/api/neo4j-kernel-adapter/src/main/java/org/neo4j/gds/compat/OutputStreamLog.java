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

import org.neo4j.gds.annotation.GenerateBuilder;
import org.neo4j.logging.Level;
import org.neo4j.logging.Log;
import org.neo4j.logging.log4j.Log4jLogProvider;

import java.io.OutputStream;
import java.util.Optional;

@GenerateBuilder
public record OutputStreamLog(
    OutputStream outputStream,
    Optional<Level> level,
    Optional<String> category
) {
    public static OutputStreamLogBuilder builder(OutputStream outputStream) {
        return OutputStreamLogBuilder.builder().outputStream(outputStream);
    }

    public Log log() {
        return new Log4jLogProvider(outputStream, level.orElse(Level.INFO)).getLog(category.orElse(""));
    }
}
