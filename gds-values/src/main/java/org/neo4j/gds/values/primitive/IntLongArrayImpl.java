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
package org.neo4j.gds.values.primitive;

import org.neo4j.gds.api.nodeproperties.ValueType;
import org.neo4j.gds.values.ArrayEquals;
import org.neo4j.gds.values.GdsValue;
import org.neo4j.gds.values.LongArray;

import java.util.Arrays;

public class IntLongArrayImpl implements LongArray {

    private final int[] value;

    public IntLongArrayImpl(int[] value) {
        this.value = value;
    }

    @Override
    public long[] longArrayValue() {
        var copy = new long[value.length];
        for (int i = 0; i < value.length; i++) {
            copy[i] = value[i];
        }
        return copy;
    }

    @Override
    public long longValue(int idx) {
        return value[idx];
    }

    @Override
    public int length() {
        return value.length;
    }

    @Override
    public ValueType type() {
        return ValueType.LONG_ARRAY;
    }

    @Override
    public long[] asObject() {
        return longArrayValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof IntLongArrayImpl) {
            return equals(((IntLongArrayImpl) o).value);
        }
        if (o instanceof GdsValue) {
            return ArrayEquals.intAndObject(value, ((GdsValue) o).asObject());
        }
        return false;
    }

    @Override
    public boolean equals(byte[] o) {
        return ArrayEquals.byteAndInt(o, value);
    }

    @Override
    public boolean equals(short[] o) {
        return ArrayEquals.shortAndInt(o, value);
    }

    @Override
    public boolean equals(int[] o) {
        return Arrays.equals(value, o);
    }

    @Override
    public boolean equals(long[] other) {
        return ArrayEquals.intAndLong(value, other);
    }

    @Override
    public boolean equals(float[] o) {
        return ArrayEquals.intAndFloat(value, o);
    }

    @Override
    public boolean equals(double[] o) {
        return ArrayEquals.intAndDouble(value, o);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return "LongArray" + Arrays.toString(value);
    }
}
