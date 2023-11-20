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

import com.carrotsearch.hppc.LongArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.neo4j.gds.Algorithm;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.collections.ha.HugeObjectArray;
import org.neo4j.gds.core.concurrency.ParallelUtil;
import org.neo4j.gds.core.concurrency.RunWithConcurrency;
import org.neo4j.gds.core.utils.partition.Partition;
import org.neo4j.gds.core.utils.partition.PartitionUtils;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.similarity.knn.metrics.SimilarityComputer;

import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.concurrent.ExecutorService;
import java.util.stream.LongStream;

public class Knn extends Algorithm<KnnResult> {
    private final Graph graph;
    private final KnnBaseConfig config;
    private final int concurrency;
    private final NeighborFilterFactory neighborFilterFactory;
    private final ExecutorService executorService;
    private final SplittableRandom splittableRandom;
    private final SimilarityFunction similarityFunction;
    private final NeighbourConsumers neighborConsumers;

    private long nodePairsConsidered;

    public static Knn createWithDefaults(Graph graph, KnnBaseConfig config, KnnContext context) {
        return createWithDefaultsAndInstrumentation(graph, config, context, NeighbourConsumers.no_op, defaultSimilarityFunction(graph, config.nodeProperties()));
    }

    public static SimilarityFunction defaultSimilarityFunction(Graph graph, List<KnnNodePropertySpec> nodeProperties) {
        return defaultSimilarityFunction(SimilarityComputer.ofProperties(graph, nodeProperties));
    }

    private static SimilarityFunction defaultSimilarityFunction(SimilarityComputer similarityComputer) {
        return new SimilarityFunction(similarityComputer);
    }

    @NotNull
    public static Knn createWithDefaultsAndInstrumentation(
        Graph graph,
        KnnBaseConfig config,
        KnnContext context,
        NeighbourConsumers neighborConsumers,
        SimilarityFunction similarityFunction
    ) {
        return new Knn(
            context.progressTracker(),
            graph,
            config,
            similarityFunction,
            new KnnNeighborFilterFactory(graph.nodeCount()),
            context.executor(),
            getSplittableRandom(config.randomSeed()),
            neighborConsumers
        );
    }

    public static Knn create(
        Graph graph,
        KnnBaseConfig config,
        SimilarityComputer similarityComputer,
        NeighborFilterFactory neighborFilterFactory,
        KnnContext context
    ) {
        SplittableRandom splittableRandom = getSplittableRandom(config.randomSeed());
        SimilarityFunction similarityFunction = defaultSimilarityFunction(similarityComputer);
        return new Knn(
            context.progressTracker(),
            graph,
            config,
            similarityFunction,
            neighborFilterFactory,
            context.executor(),
            splittableRandom,
            NeighbourConsumers.no_op
        );
    }

    @NotNull
    private static SplittableRandom getSplittableRandom(Optional<Long> randomSeed) {
        return randomSeed.map(SplittableRandom::new).orElseGet(SplittableRandom::new);
    }

    Knn(
        ProgressTracker progressTracker,
        Graph graph,
        KnnBaseConfig config,
        SimilarityFunction similarityFunction,
        NeighborFilterFactory neighborFilterFactory,
        ExecutorService executorService,
        SplittableRandom splittableRandom,
        NeighbourConsumers neighborConsumers
    ) {
        super(progressTracker);
        this.graph = graph;
        this.config = config;
        this.concurrency = config.concurrency();
        this.similarityFunction = similarityFunction;
        this.neighborFilterFactory = neighborFilterFactory;
        this.executorService = executorService;
        this.splittableRandom = splittableRandom;
        this.neighborConsumers = neighborConsumers;
    }

    public ExecutorService executorService() {
        return this.executorService;
    }

    @Override
    public KnnResult compute() {
        if (graph.nodeCount() < 2 || config.topK() == 0) {
            return new EmptyResult();
        }
        this.progressTracker.beginSubTask();
        this.progressTracker.beginSubTask();
        HugeObjectArray<NeighborList> neighbors = initializeRandomNeighbors();
        this.progressTracker.endSubTask();

        var maxIterations = this.config.maxIterations();
        var maxUpdates = (long) Math.ceil(this.config.sampleRate() * this.config.topK() * graph.nodeCount());
        var updateThreshold = (long) Math.floor(this.config.deltaThreshold() * maxUpdates);

        long updateCount;
        int iteration = 0;
        boolean didConverge = false;

        this.progressTracker.beginSubTask();
        for (; iteration < maxIterations; iteration++) {
            updateCount = iteration(neighbors);
            if (updateCount <= updateThreshold) {
                iteration++;
                didConverge = true;
                break;
            }
        }
        if (config.similarityCutoff() > 0) {
            var similarityCutoff = config.similarityCutoff();
            var neighborFilterTasks = PartitionUtils.rangePartition(
                concurrency,
                neighbors.size(),
                partition -> (Runnable) () -> partition.consume(
                    nodeId -> neighbors.get(nodeId).filterHighSimilarityResults(similarityCutoff)
                ),
                Optional.of(config.minBatchSize())
            );
            RunWithConcurrency.builder()
                .concurrency(concurrency)
                .tasks(neighborFilterTasks)
                .terminationFlag(terminationFlag)
                .executor(this.executorService)
                .run();
        }
        this.progressTracker.endSubTask();

        this.progressTracker.endSubTask();
        return ImmutableKnnResult.of(
            neighbors,
            iteration,
            didConverge,
            this.nodePairsConsidered,
            graph.nodeCount()
        );
    }

    private HugeObjectArray<NeighborList> initializeRandomNeighbors() {
        var neighbors = HugeObjectArray.newArray(NeighborList.class, graph.nodeCount());

        var randomNeighborGenerators = PartitionUtils.rangePartition(
            concurrency,
            graph.nodeCount(),
            partition -> {
                var localRandom = splittableRandom.split();
                return new GenerateRandomNeighbors(
                    initializeSampler(localRandom),
                    localRandom,
                    this.similarityFunction,
                    this.neighborFilterFactory.create(),
                    neighbors,
                    config.boundedK(graph.nodeCount()),
                    partition,
                    progressTracker,
                    neighborConsumers
                );
            },
            Optional.of(config.minBatchSize())
        );

        RunWithConcurrency.builder()
            .concurrency(concurrency)
            .tasks(randomNeighborGenerators)
            .terminationFlag(terminationFlag)
            .executor(this.executorService)
            .run();

        this.nodePairsConsidered += randomNeighborGenerators.stream().mapToLong(GenerateRandomNeighbors::neighborsFound).sum();

        return neighbors;
    }

    private KnnSampler initializeSampler(SplittableRandom random) {
        switch(config.initialSampler()) {
            case UNIFORM: {
                return new UniformKnnSampler(random, graph.nodeCount());
            }
            case RANDOMWALK: {
                return new RandomWalkKnnSampler(
                    graph.concurrentCopy(),
                    random,
                    config.randomSeed(),
                    config.boundedK(graph.nodeCount())
                );
            }
            default:
                throw new IllegalStateException("Invalid KnnSampler");
        }
    }

    private long iteration(HugeObjectArray<NeighborList> neighbors) {
        var nodeCount = graph.nodeCount();
        var minBatchSize = this.config.minBatchSize();
        var sampledK = this.config.sampledK(nodeCount);

        // TODO: init in ctor and reuse - benchmark against new allocations
        var allOldNeighbors = HugeObjectArray.newArray(LongArrayList.class, nodeCount);
        var allNewNeighbors = HugeObjectArray.newArray(LongArrayList.class, nodeCount);

        progressTracker.beginSubTask();
        ParallelUtil.readParallel(concurrency, nodeCount, this.executorService, new SplitOldAndNewNeighbors(
            this.splittableRandom,
            neighbors,
            allOldNeighbors,
            allNewNeighbors,
            sampledK,
            progressTracker
        ));
        progressTracker.endSubTask();

        // TODO: init in ctor and reuse - benchmark against new allocations
        var reverseOldNeighbors = HugeObjectArray.newArray(LongArrayList.class, nodeCount);
        var reverseNewNeighbors = HugeObjectArray.newArray(LongArrayList.class, nodeCount);

        progressTracker.beginSubTask();
        reverseOldAndNewNeighbors(
            allOldNeighbors,
            allNewNeighbors,
            reverseOldNeighbors,
            reverseNewNeighbors,
            concurrency,
            minBatchSize,
            progressTracker
        );
        progressTracker.endSubTask();

        var neighborsJoiners = PartitionUtils.rangePartition(
            concurrency,
            nodeCount,
            partition -> new JoinNeighbors(
                this.splittableRandom.split(),
                this.similarityFunction,
                this.neighborFilterFactory.create(),
                neighbors,
                allOldNeighbors,
                allNewNeighbors,
                reverseOldNeighbors,
                reverseNewNeighbors,
                sampledK,
                this.config.perturbationRate(),
                this.config.randomJoins(),
                partition,
                progressTracker
            ),
            Optional.of(minBatchSize)
        );

        progressTracker.beginSubTask();
        RunWithConcurrency.builder()
            .concurrency(concurrency)
            .tasks(neighborsJoiners)
            .terminationFlag(terminationFlag)
            .executor(this.executorService)
            .run();
        progressTracker.endSubTask();

        this.nodePairsConsidered += neighborsJoiners.stream().mapToLong(JoinNeighbors::nodePairsConsidered).sum();

        return neighborsJoiners.stream().mapToLong(joiner -> joiner.updateCount).sum();
    }

    private static void reverseOldAndNewNeighbors(
        HugeObjectArray<LongArrayList> allOldNeighbors,
        HugeObjectArray<LongArrayList> allNewNeighbors,
        HugeObjectArray<LongArrayList> reverseOldNeighbors,
        HugeObjectArray<LongArrayList> reverseNewNeighbors,
        int concurrency,
        int minBatchSize,
        ProgressTracker progressTracker
    ) {
        long nodeCount = allNewNeighbors.size();
        long logBatchSize = ParallelUtil.adjustedBatchSize(nodeCount, concurrency, minBatchSize);

        // TODO: cursors
        for (long nodeId = 0; nodeId < nodeCount; nodeId++) {
            reverseNeighbors(nodeId, allOldNeighbors, reverseOldNeighbors);
            reverseNeighbors(nodeId, allNewNeighbors, reverseNewNeighbors);

            if ((nodeId + 1) % logBatchSize == 0) {
                progressTracker.logProgress(logBatchSize);
            }
        }
    }

    static void reverseNeighbors(
        long nodeId,
        HugeObjectArray<LongArrayList> allNeighbors,
        HugeObjectArray<LongArrayList> reverseNeighbors
    ) {
        // adding nodeId to the neighbors of its neighbors (reversing the neighbors direction)
        var neighbors = allNeighbors.get(nodeId);
        if (neighbors != null) {
            for (var neighbor : neighbors) {
                assert neighbor.value != nodeId;
                var oldReverse = reverseNeighbors.get(neighbor.value);
                if (oldReverse == null) {
                    oldReverse = new LongArrayList();
                    reverseNeighbors.set(neighbor.value, oldReverse);
                }
                oldReverse.add(nodeId);
            }
        }
    }

    static final class JoinNeighbors implements Runnable {
        private final SplittableRandom random;
        private final SimilarityFunction similarityFunction;
        private final NeighborFilter neighborFilter;
        private final HugeObjectArray<NeighborList> allNeighbors;
        private final HugeObjectArray<LongArrayList> allOldNeighbors;
        private final HugeObjectArray<LongArrayList> allNewNeighbors;
        private final HugeObjectArray<LongArrayList> allReverseOldNeighbors;
        private final HugeObjectArray<LongArrayList> allReverseNewNeighbors;
        private final int sampledK;
        private final int randomJoins;
        private final ProgressTracker progressTracker;
        private final long nodeCount;
        private long updateCount;
        private final Partition partition;
        private long nodePairsConsidered;
        private final double perturbationRate;

        JoinNeighbors(
            SplittableRandom random,
            SimilarityFunction similarityFunction,
            NeighborFilter neighborFilter,
            HugeObjectArray<NeighborList> allNeighbors,
            HugeObjectArray<LongArrayList> allOldNeighbors,
            HugeObjectArray<LongArrayList> allNewNeighbors,
            HugeObjectArray<LongArrayList> allReverseOldNeighbors,
            HugeObjectArray<LongArrayList> allReverseNewNeighbors,
            int sampledK,
            double perturbationRate,
            int randomJoins,
            Partition partition,
            ProgressTracker progressTracker
        ) {
            this.random = random;
            this.similarityFunction = similarityFunction;
            this.neighborFilter = neighborFilter;
            this.allNeighbors = allNeighbors;
            this.nodeCount = allNewNeighbors.size();
            this.allOldNeighbors = allOldNeighbors;
            this.allNewNeighbors = allNewNeighbors;
            this.allReverseOldNeighbors = allReverseOldNeighbors;
            this.allReverseNewNeighbors = allReverseNewNeighbors;
            this.sampledK = sampledK;
            this.randomJoins = randomJoins;
            this.partition = partition;
            this.progressTracker = progressTracker;
            this.perturbationRate = perturbationRate;
            this.updateCount = 0;
            this.nodePairsConsidered = 0;
        }

        @Override
        public void run() {
            var startNode = partition.startNode();
            long endNode = startNode + partition.nodeCount();

            for (long nodeId = startNode; nodeId < endNode; nodeId++) {
                // old[v] ∪ Sample(old′[v], ρK)
                var oldNeighbors = allOldNeighbors.get(nodeId);
                if (oldNeighbors != null) {
                    combineNeighbors(allReverseOldNeighbors.get(nodeId), oldNeighbors);
                }


                // new[v] ∪ Sample(new′[v], ρK)
                var newNeighbors = allNewNeighbors.get(nodeId);
                if (newNeighbors != null) {
                    combineNeighbors(allReverseNewNeighbors.get(nodeId), newNeighbors);

                    this.updateCount += joinNewNeighbors(nodeId, oldNeighbors, newNeighbors);
                }

                // this isn't in the paper
                randomJoins(nodeCount, nodeId);
            }
            progressTracker.logProgress(partition.nodeCount());
        }

        private long joinNewNeighbors(long nodeId, LongArrayList oldNeighbors, LongArrayList newNeighbors
        ) {
            long updateCount = 0;

            var newNeighborElements = newNeighbors.buffer;
            var newNeighborsCount = newNeighbors.elementsCount;
            boolean similarityIsSymmetric = similarityFunction.isSymmetric();

            for (int i = 0; i < newNeighborsCount; i++) {
                var elem1 = newNeighborElements[i];
                assert elem1 != nodeId;

                // join(u1, nodeId), this isn't in the paper
                updateCount += join(elem1, nodeId);

                //  try out using the new neighbors between themselves / join(new_nbd, new_ndb)
                for (int j = i + 1; j < newNeighborsCount; j++) {
                    var elem2 = newNeighborElements[j];
                    if (elem1 == elem2) {
                        continue;
                    }

                    if (similarityIsSymmetric) {
                        updateCount += joinSymmetric(elem1, elem2);
                    } else {
                        updateCount += join(elem1, elem2);
                        updateCount += join(elem2, elem1);
                    }
                }

                // try out joining the old neighbors with the new neighbor / join(new_nbd, old_ndb)
                if (oldNeighbors != null) {
                    for (var oldElemCursor : oldNeighbors) {
                        var elem2 = oldElemCursor.value;

                        if (elem1 == elem2) {
                            continue;
                        }

                        if (similarityIsSymmetric) {
                            updateCount += joinSymmetric(elem1, elem2);
                        } else {
                            updateCount += join(elem1, elem2);
                            updateCount += join(elem2, elem1);
                        }
                    }
                }
            }
            return updateCount;
        }

        private void combineNeighbors(@Nullable LongArrayList reversedNeighbors, LongArrayList neighbors) {
            if (reversedNeighbors != null) {
                var numberOfReverseNeighbors = reversedNeighbors.size();
                for (var elem : reversedNeighbors) {
                    if (random.nextInt(numberOfReverseNeighbors) < sampledK) {
                        // TODO: this could add nodes twice, maybe? should this be a set?
                        neighbors.add(elem.value);
                    }
                }
            }
        }

        private void randomJoins(long nodeCount, long nodeId) {
            for (int i = 0; i < randomJoins; i++) {
                var randomNodeId = random.nextLong(nodeCount - 1);
                // shifting the randomNode as the randomNode was picked from [0, n-1)
                if (randomNodeId >= nodeId) {
                    ++randomNodeId;
                }
                // random joins are not counted towards the actual update counter
                join(nodeId, randomNodeId);
            }
        }

        private long joinSymmetric(long node1, long node2) {
            assert node1 != node2;

            if (neighborFilter.excludeNodePair(node1, node2)) {
                return 0;
            }

            nodePairsConsidered++;
            var similarity = similarityFunction.computeSimilarity(node1, node2);

            var neighbors1 = allNeighbors.get(node1);

            var updates = 0L;

            synchronized (neighbors1) {
                updates += neighbors1.add(node2, similarity, random, perturbationRate);
            }

            var neighbors2 = allNeighbors.get(node2);

            synchronized (neighbors2) {
                updates += neighbors2.add(node1, similarity, random, perturbationRate);
            }

            return updates;
        }

        private long join(long node1, long node2) {
            assert node1 != node2;

            if (neighborFilter.excludeNodePair(node1, node2)) {
                return 0;
            }

            var similarity = similarityFunction.computeSimilarity(node1, node2);
            nodePairsConsidered++;
            var neighbors = allNeighbors.get(node1);

            synchronized (neighbors) {
                return neighbors.add(node2, similarity, random, perturbationRate);
            }
        }

        long nodePairsConsidered() {
            return nodePairsConsidered;
        }
    }

    private static final class EmptyResult extends KnnResult {

        @Override
        HugeObjectArray<NeighborList> neighborList() {
            return HugeObjectArray.of();
        }

        @Override
        public int ranIterations() {
            return 0;
        }

        @Override
        public boolean didConverge() {
            return false;
        }

        @Override
        public long nodePairsConsidered() {
            return 0;
        }

        @Override
        public LongStream neighborsOf(long nodeId) {
            return LongStream.empty();
        }

        @Override
        public long size() {
            return 0;
        }

        @Override
        public long nodesCompared() {
            return 0;
        }
    }
}
