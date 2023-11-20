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
package org.neo4j.gds.similarity.knn;

public final class K {
    static K create(int value, double sampleRate, long nodeCount) {
        if (value < 1) throw new IllegalArgumentException("K value must be 1 or more");
        if (Double.compare(sampleRate, 0.0) < 1 || Double.compare(sampleRate, 1.0) > 0)
            throw new IllegalArgumentException("sampleRate must be more than 0.0 and less than or equal to 1.0");
        // (int) is safe because value is at most `topK`, which is an int
        var boundedValue = Math.max(0, (int) Math.min(value, nodeCount - 1));
        // (int) is safe because value is at most `topK`, which is an int
        // This could be violated if a sampleRate outside of [0,1] is used
        // which is only possible from our tests


        // Nah that shouldn't be possible anymore anywhere, yeah?


        var sampledValue = Math.max(0, (int) Math.min((long) Math.ceil(sampleRate * value), nodeCount - 1));
        return new K(value, boundedValue, sampledValue);
    }

    public int value;
    public int boundedValue;
    public int sampledValue;

    private K(int value, int boundedValue, int sampledValue) {
        this.value = value;
        this.boundedValue = boundedValue;
        this.sampledValue = sampledValue;
    }
}
