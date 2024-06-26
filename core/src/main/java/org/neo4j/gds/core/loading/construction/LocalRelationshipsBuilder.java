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
package org.neo4j.gds.core.loading.construction;

import org.neo4j.gds.core.loading.PropertyReader;
import org.neo4j.gds.core.loading.RelationshipsBatchBufferBuilder;
import org.neo4j.gds.core.loading.SingleTypeRelationshipImporter;
import org.neo4j.gds.core.loading.ThreadLocalSingleTypeRelationshipImporter;

abstract class LocalRelationshipsBuilder implements AutoCloseable {

    abstract void addRelationship(long source, long target);

    abstract void addRelationship(long source, long target, double relationshipPropertyValue);

    abstract void addRelationship(long source, long target, double[] relationshipPropertyValues);

    static class NonIndexed extends LocalRelationshipsBuilder {

        private final ThreadLocalSingleTypeRelationshipImporter<Integer> importer;
        private final PropertyReader.Buffered<Integer> bufferedPropertyReader;
        private final int propertyCount;
        private int localRelationshipId;

        NonIndexed(
            SingleTypeRelationshipImporter singleTypeRelationshipImporter,
            int bufferSize,
            int propertyCount
        ) {
            this.propertyCount = propertyCount;

            var relationshipsBatchBuffer = new RelationshipsBatchBufferBuilder<Integer>()
                .capacity(bufferSize)
                .propertyReferenceClass(Integer.class)
                .build();

            if (propertyCount > 1) {
                this.bufferedPropertyReader = PropertyReader.buffered(bufferSize, propertyCount);
                this.importer = singleTypeRelationshipImporter.threadLocalImporter(
                    relationshipsBatchBuffer,
                    bufferedPropertyReader
                );
            } else {
                this.bufferedPropertyReader = null;
                this.importer = singleTypeRelationshipImporter.threadLocalImporter(
                    relationshipsBatchBuffer,
                    PropertyReader.preLoaded()
                );
            }
        }

        @Override
        void addRelationship(long source, long target) {
            importer.buffer().add(source, target);
            if (importer.buffer().isFull()) {
                flushBuffer();
            }
        }

        @Override
        void addRelationship(long source, long target, double relationshipPropertyValue) {
            importer
                .buffer()
                .add(
                    source,
                    target,
                    Double.doubleToLongBits(relationshipPropertyValue),
                    RelationshipsBuilder.NO_PROPERTY_REF
                );
            if (importer.buffer().isFull()) {
                flushBuffer();
            }
        }

        @Override
        void addRelationship(long source, long target, double[] relationshipPropertyValues) {
            int nextRelationshipId = localRelationshipId++;
            importer.buffer().add(source, target, nextRelationshipId, RelationshipsBuilder.NO_PROPERTY_REF);
            for (int propertyKeyId = 0; propertyKeyId < this.propertyCount; propertyKeyId++) {
                bufferedPropertyReader
                    .add(nextRelationshipId, propertyKeyId, relationshipPropertyValues[propertyKeyId]);
            }
            if (importer.buffer().isFull()) {
                flushBuffer();
            }
        }

        @Override
        public void close() {
            flushBuffer();
        }

        private void flushBuffer() {
            importer.importRelationships();
            importer.buffer().reset();
            localRelationshipId = 0;
        }
    }

    static class Indexed extends LocalRelationshipsBuilder {

        private final NonIndexed forwardBuilder;
        private final NonIndexed reverseBuilder;

        Indexed(NonIndexed forwardBuilder, NonIndexed reverseBuilder) {
            this.forwardBuilder = forwardBuilder;
            this.reverseBuilder = reverseBuilder;
        }

        @Override
        void addRelationship(long source, long target) {
            this.forwardBuilder.addRelationship(source, target);
            this.reverseBuilder.addRelationship(source, target);
        }

        @Override
        void addRelationship(long source, long target, double relationshipPropertyValue) {
            this.forwardBuilder.addRelationship(source, target, relationshipPropertyValue);
            this.reverseBuilder.addRelationship(source, target, relationshipPropertyValue);
        }

        @Override
        void addRelationship(long source, long target, double[] relationshipPropertyValues) {
            this.forwardBuilder.addRelationship(source, target, relationshipPropertyValues);
            this.reverseBuilder.addRelationship(source, target, relationshipPropertyValues);
        }

        @Override
        public void close() throws Exception {
            this.forwardBuilder.close();
            this.reverseBuilder.close();
        }
    }
}
