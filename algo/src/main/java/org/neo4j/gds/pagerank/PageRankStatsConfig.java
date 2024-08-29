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
package org.neo4j.gds.pagerank;

import org.neo4j.gds.annotation.Configuration;
import org.neo4j.gds.core.CypherMapWrapper;

@Configuration
public interface PageRankStatsConfig extends PageRankConfig {

   private static PageRankStatsConfig of(CypherMapWrapper userInput, boolean checkDampingFactor) {
        if (checkDampingFactor && userInput.containsKey("dampingFactor")) {
            throw new IllegalArgumentException("Unexpected configuration key: dampingFactor");
        }
        return new PageRankStatsConfigImpl(userInput);
    }

     static PageRankStatsConfig configWithDampingFactor(CypherMapWrapper userInput) {
       return of(userInput, false);
    }

     static PageRankStatsConfig configWithoutDampingFactor(CypherMapWrapper userInput) {
        return of(userInput, true);
    }


}
