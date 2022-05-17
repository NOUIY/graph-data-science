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
package org.neo4j.gds.similarity.filteredknn;

import org.neo4j.gds.Algorithm;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.similarity.knn.Knn;
import org.neo4j.gds.similarity.knn.KnnContext;

public class FilteredKnn extends Algorithm<FilteredKnnResult> {
    /**
     * This is KNN instrumented with neighbour consumers
     */
    private final Knn delegate;

    private final TargetNodeFilteringNeighbourConsumers neighbourConsumers;
    private final NodeFilter sourceNodeFilter;

    public static FilteredKnn create(Graph graph, FilteredKnnBaseConfig config, KnnContext context) {
        var targetNodeFilter = config.sourceNodeFilter().toNodeFilter(graph);
        var neighbourConsumers = TargetNodeFilteringNeighbourConsumers.create(graph.nodeCount()/*, targetNodeFilter*/);
        var knn = Knn.createWithDefaultsss(graph, config, context, neighbourConsumers);
        var sourceNodeFilter = config.sourceNodeFilter().toNodeFilter(graph);

        return new FilteredKnn(context.progressTracker(), knn, neighbourConsumers, sourceNodeFilter);
    }

    private FilteredKnn(
        ProgressTracker progressTracker,
        Knn delegate,
        TargetNodeFilteringNeighbourConsumers neighbourConsumers,
        NodeFilter sourceNodeFilter
    ) {
        super(progressTracker);
        this.delegate = delegate;
        this.neighbourConsumers = neighbourConsumers;
        this.sourceNodeFilter = sourceNodeFilter;
    }

    @Override
    public FilteredKnnResult compute() {
        Knn.Result result = delegate.compute();

        return ImmutableFilteredKnnResult.of(
            neighbourConsumers,
            result.ranIterations(),
            result.didConverge(),
            result.nodePairsConsidered(),
            sourceNodeFilter
        );
    }

    @Override
    public void release() {
        delegate.release();
    }

    @Override
    public void assertRunning() {
        delegate.assertRunning();
    }
}
