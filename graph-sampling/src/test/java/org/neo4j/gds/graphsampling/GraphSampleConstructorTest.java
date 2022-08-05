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
package org.neo4j.gds.graphsampling;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.compat.Neo4jProxy;
import org.neo4j.gds.compat.TestLog;
import org.neo4j.gds.core.utils.paged.HugeAtomicBitSet;
import org.neo4j.gds.core.utils.progress.EmptyTaskRegistryFactory;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.core.utils.progress.tasks.Task;
import org.neo4j.gds.core.utils.progress.tasks.TaskProgressTracker;
import org.neo4j.gds.core.utils.progress.tasks.Tasks;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.IdFunction;
import org.neo4j.gds.extension.Inject;
import org.neo4j.gds.graphsampling.config.RandomWalkWithRestartsConfigImpl;
import org.neo4j.gds.graphsampling.samplers.NodesSampler;
import org.neo4j.gds.graphsampling.samplers.RandomWalkWithRestarts;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.neo4j.gds.TestSupport.assertGraphEquals;
import static org.neo4j.gds.TestSupport.fromGdl;
import static org.neo4j.gds.assertj.Extractors.removingThreadId;

@GdlExtension
class GraphSampleConstructorTest {

    @GdlGraph(idOffset = 42)
    private static final String DB_CYPHER =
        "CREATE" +
        "  (x:Z {prop: 42})" +
        ", (x1:Z {prop: 43})" +
        ", (x2:Z {prop: 44})" +
        ", (x3:Z {prop: 45})" +
        ", (a:N {prop: 46})" +
        ", (b:N {prop: 47})" +
        ", (c:N {prop: 48, attr: 48})" +
        ", (d:N {prop: 49, attr: 48})" +
        ", (e:M {prop: 50, attr: 48})" +
        ", (f:M {prop: 51, attr: 48})" +
        ", (g:M {prop: 52})" +
        ", (h:M {prop: 53})" +
        ", (i:X {prop: 54})" +
        ", (j:M {prop: 55})" +
        ", (x)-[:R1]->(x1)" +
        ", (x)-[:R1]->(x2)" +
        ", (x)-[:R1]->(x3)" +
        ", (e)-[:R1]->(d)" +
        ", (i)-[:R1]->(g)" +
        ", (a)-[:R1 {cost: 10.0, distance: 5.8}]->(b)" +
        ", (a)-[:R1 {cost: 10.0, distance: 4.8}]->(c)" +
        ", (c)-[:R1 {cost: 10.0, distance: 5.8}]->(d)" +
        ", (d)-[:R1 {cost:  4.2, distance: 2.6}]->(e)" +
        ", (e)-[:R1 {cost: 10.0, distance: 5.8}]->(f)" +
        ", (f)-[:R1 {cost: 10.0, distance: 9.9}]->(g)" +
        ", (h)-[:R2 {cost: 10.0, distance: 5.8}]->(i)";

    @Inject
    private GraphStore graphStore;

    @Inject
    private IdFunction idFunction;

    class TestNodesSampler implements NodesSampler {

        private final List<Long> originalIds;

        TestNodesSampler(List<Long> originalIds) {
            this.originalIds = originalIds;
        }

        @Override
        public HugeAtomicBitSet compute(Graph inputGraph, ProgressTracker unused) {
            var bitset = HugeAtomicBitSet.create(inputGraph.nodeCount());
            for (long originalId : originalIds) {
                bitset.set(inputGraph.toMappedNodeId(originalId));
            }
            return bitset;
        }

        @Override
        public Task progressTask(GraphStore graphStore) {
            return Tasks.empty();
        }

        @Override
        public String progressTaskName() {
            return "DUMMY";
        }
    }


    @Test
    void shouldSampleAndFilterSchema() {
        var config = RandomWalkWithRestartsConfigImpl.builder()
            .startNodes(List.of(idFunction.of("a")))
            .samplingRatio(0.5)
            .restartProbability(0.1)
            .concurrency(1)
            .randomSeed(42L)
            .build();

        var originalIds = List.of(
            idFunction.of("a"),
            idFunction.of("b"),
            idFunction.of("c"),
            idFunction.of("d"),
            idFunction.of("e"),
            idFunction.of("f"),
            idFunction.of("g")
        );

        var graphConstructor = new GraphSampleConstructor(
            config,
            graphStore,
            new TestNodesSampler(originalIds),
            ProgressTracker.NULL_TRACKER
        );

        var subgraph = graphConstructor.compute();
        assertThat(subgraph.getUnion().nodeCount()).isEqualTo(7);
        assertThat(graphStore.schema().nodeSchema().filter(Set.of(NodeLabel.of("N"), NodeLabel.of("M"))))
            .usingRecursiveComparison()
            .isEqualTo(subgraph.schema().nodeSchema());
        assertThat(graphStore.schema().relationshipSchema().filter(Set.of(RelationshipType.of("R1"))))
            .usingRecursiveComparison()
            .isEqualTo(subgraph.schema().relationshipSchema());
        assertThat(graphStore.capabilities()).usingRecursiveComparison().isEqualTo(subgraph.capabilities());
        assertThat(graphStore.databaseId()).usingRecursiveComparison().isEqualTo(subgraph.databaseId());

        var expectedGraph =
            "  (a:N {prop: 46})" +
            ", (b:N {prop: 47})" +
            ", (c:N {prop: 48, attr: 48})" +
            ", (d:N {prop: 49, attr: 48})" +
            ", (e:M {prop: 50, attr: 48})" +
            ", (f:M {prop: 51, attr: 48})" +
            ", (g:M {prop: 52})" +
            ", (e)-[:R1]->(d)" +
            ", (a)-[:R1 {distance: 5.8}]->(b)" +
            ", (a)-[:R1 {distance: 4.8}]->(c)" +
            ", (c)-[:R1 {distance: 5.8}]->(d)" +
            ", (d)-[:R1 {distance: 2.6}]->(e)" +
            ", (e)-[:R1 {distance: 5.8}]->(f)" +
            ", (f)-[:R1 {distance: 9.9}]->(g)";
        assertGraphEquals(fromGdl(expectedGraph), subgraph.getGraph("distance"));
    }

    @Test
    void shouldFilterGraph() {
        var config = RandomWalkWithRestartsConfigImpl.builder()
            .startNodes(List.of(idFunction.of("e")))
            .nodeLabels(List.of("M", "X"))
            .relationshipTypes(List.of("R1"))
            .samplingRatio(0.5)
            .restartProbability(0.1)
            .concurrency(1)
            .randomSeed(42L)
            .build();

        var originalIds = List.of(
            idFunction.of("e"),
            idFunction.of("f"),
            idFunction.of("g")
        );

        var graphConstructor = new GraphSampleConstructor(
            config,
            graphStore,
            new TestNodesSampler(originalIds),
            ProgressTracker.NULL_TRACKER
        );

        var subgraph = graphConstructor.compute();
        assertThat(subgraph.getUnion().nodeCount()).isEqualTo(3);
        var expectedGraph =
            "  (e:M {prop: 50, attr: 48})" +
            ", (f:M {prop: 51, attr: 48})" +
            ", (g:M {prop: 52})" +
            ", (e)-[:R1 {distance: 5.8}]->(f)" +
            ", (f)-[:R1 {distance: 9.9}]->(g)";
        assertGraphEquals(fromGdl(expectedGraph), subgraph.getGraph("distance"));
    }

    @Test
    void shouldLogProgressWithRWR() {
        var config = RandomWalkWithRestartsConfigImpl.builder()
            .startNodes(List.of(idFunction.of("a")))
            .samplingRatio(0.5)
            .restartProbability(0.1)
            .concurrency(1)
            .randomSeed(42L)
            .build();
        var rwr = new RandomWalkWithRestarts(config);

        var log = Neo4jProxy.testLog();
        var progressTracker = new TaskProgressTracker(
            GraphSampleConstructor.progressTask(graphStore, rwr),
            log,
            1,
            EmptyTaskRegistryFactory.INSTANCE
        );

        var rwrGraphConstructor = new GraphSampleConstructor(
            config,
            graphStore,
            rwr,
            progressTracker
        );
        rwrGraphConstructor.compute();

        var messages = log.getMessages(TestLog.INFO);

        assertThat(messages)
            // avoid asserting on the thread id
            .extracting(removingThreadId())
            .contains(
                "Random walk with restarts sampling :: Start",
                "Random walk with restarts sampling :: Sample with RWR :: Start",
                "Random walk with restarts sampling :: Sample with RWR 28%",
                "Random walk with restarts sampling :: Sample with RWR 100%",
                "Random walk with restarts sampling :: Sample with RWR :: Finished",
                "Random walk with restarts sampling :: Construct graph :: Start",
                "Random walk with restarts sampling :: Construct graph :: Construct node id map :: Start",
                "Random walk with restarts sampling :: Construct graph :: Construct node id map 100%",
                "Random walk with restarts sampling :: Construct graph :: Construct node id map :: Finished",
                "Random walk with restarts sampling :: Construct graph :: Filter node properties :: Start",
                "Random walk with restarts sampling :: Construct graph :: Filter node properties 7%",
                "Random walk with restarts sampling :: Construct graph :: Filter node properties 14%",
                "Random walk with restarts sampling :: Construct graph :: Filter node properties 21%",
                "Random walk with restarts sampling :: Construct graph :: Filter node properties 28%",
                "Random walk with restarts sampling :: Construct graph :: Filter node properties 35%",
                "Random walk with restarts sampling :: Construct graph :: Filter node properties 42%",
                "Random walk with restarts sampling :: Construct graph :: Filter node properties 50%",
                "Random walk with restarts sampling :: Construct graph :: Filter node properties 57%",
                "Random walk with restarts sampling :: Construct graph :: Filter node properties 64%",
                "Random walk with restarts sampling :: Construct graph :: Filter node properties 71%",
                "Random walk with restarts sampling :: Construct graph :: Filter node properties 78%",
                "Random walk with restarts sampling :: Construct graph :: Filter node properties 85%",
                "Random walk with restarts sampling :: Construct graph :: Filter node properties 92%",
                "Random walk with restarts sampling :: Construct graph :: Filter node properties 100%",
                "Random walk with restarts sampling :: Construct graph :: Filter node properties :: Finished",
                "Random walk with restarts sampling :: Construct graph :: Filter relationship properties :: Start",
                "Random walk with restarts sampling :: Construct graph :: Filter relationship properties :: Relationship type 1 of 2 :: Start",
                "Random walk with restarts sampling :: Construct graph :: Filter relationship properties :: Relationship type 1 of 2 27%",
                "Random walk with restarts sampling :: Construct graph :: Filter relationship properties :: Relationship type 1 of 2 45%",
                "Random walk with restarts sampling :: Construct graph :: Filter relationship properties :: Relationship type 1 of 2 54%",
                "Random walk with restarts sampling :: Construct graph :: Filter relationship properties :: Relationship type 1 of 2 100%",
                "Random walk with restarts sampling :: Construct graph :: Filter relationship properties :: Relationship type 1 of 2 :: Finished",
                "Random walk with restarts sampling :: Construct graph :: Filter relationship properties :: Relationship type 2 of 2 :: Start",
                "Random walk with restarts sampling :: Construct graph :: Filter relationship properties :: Relationship type 2 of 2 100%",
                "Random walk with restarts sampling :: Construct graph :: Filter relationship properties :: Relationship type 2 of 2 :: Finished",
                "Random walk with restarts sampling :: Construct graph :: Filter relationship properties :: Finished",
                "Random walk with restarts sampling :: Construct graph :: Finished",
                "Random walk with restarts sampling :: Finished"
            );
    }
}