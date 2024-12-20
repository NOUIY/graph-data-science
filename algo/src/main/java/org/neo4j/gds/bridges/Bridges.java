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
package org.neo4j.gds.bridges;

import com.carrotsearch.hppc.BitSet;
import org.neo4j.gds.Algorithm;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.collections.ha.HugeLongArray;
import org.neo4j.gds.collections.ha.HugeObjectArray;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class Bridges extends Algorithm<BridgeResult> {

    private final Graph graph;
    private final BitSet visited;
    private final HugeLongArray tin;
    private final HugeLongArray low;
    private long timer;
    private long stackIndex = -1;
    private List<Bridge> result = new ArrayList<>();
    private final Optional<TreeSizeTracker> treeSizeTracker;

    public static  Bridges create(Graph graph, ProgressTracker progressTracker, boolean shouldComputeComponents){

        if (shouldComputeComponents) {
            var treeSizeTracker = new TreeSizeTracker(graph.nodeCount());
            return new Bridges(graph, progressTracker, Optional.of(treeSizeTracker));
        }else{
            return new Bridges(graph,progressTracker,Optional.empty());
        }
    }

    private Bridges(Graph graph, ProgressTracker progressTracker, Optional<TreeSizeTracker> treeSizeTracker){
        super(progressTracker);

        this.graph = graph;
        this.visited = new BitSet(graph.nodeCount());
        this.tin = HugeLongArray.newArray(graph.nodeCount());
        this.low = HugeLongArray.newArray(graph.nodeCount());
        this.treeSizeTracker = treeSizeTracker;
    }

    @Override
    public BridgeResult compute() {
        timer = 0;
        visited.clear();
        tin.setAll(__ -> -1);
        low.setAll(__ -> -1);
        progressTracker.beginSubTask("Bridges");
        //each edge may have at most one event to the stack at the same time
        var stack = HugeObjectArray.newArray(StackEvent.class, graph.relationshipCount());

        BiConsumer<Long,Long> onLastChildVisit = (treeSizeTracker.isPresent()) ? treeSizeTracker.get()::recordTreeChild : (a,b)->{};
        int listIndex=0;
        var n = graph.nodeCount();
        for (long i = 0; i < n; ++i) {
            if (!visited.get(i)) {
                dfs(i, stack,onLastChildVisit);

                if (treeSizeTracker.isPresent()) {
                    var tracker =treeSizeTracker.get();
                    var listEndIndex = result.size();
                    for (var j = listIndex; j < listEndIndex; ++j) {
                        var currentBridge = result.get(j);
                        var remainingSizes = tracker.recordBridge(currentBridge.to(),i);
                        result.set(j,new Bridge(currentBridge.from(), currentBridge.to(),remainingSizes));
                    }
                    listIndex = listEndIndex;
                }
            }
        }
        progressTracker.endSubTask("Bridges");
        return new BridgeResult(result);

    }

    private void dfs(long node, HugeObjectArray<StackEvent> stack, BiConsumer<Long,Long> onLastChildVisit) {
        stack.set(++stackIndex, StackEvent.upcomingVisit(node,-1));
        while (stackIndex >= 0) {
            var stackEvent = stack.get(stackIndex--);
            visitEvent(stackEvent, stack,onLastChildVisit);
        }
        progressTracker.logProgress();
    }

    private void visitEvent(StackEvent event, HugeObjectArray<StackEvent> stack, BiConsumer<Long,Long> onLastChildVisit) {
        if (event.lastVisit()) {
            var to = event.eventNode();
            var v = event.triggerNode();
            var lowV = low.get(v);
            var lowTo = low.get(to);
            low.set(v, Math.min(lowV, lowTo));
            var tinV = tin.get(v);
            onLastChildVisit.accept(v,to);
            if (lowTo > tinV) {
                result.add(new Bridge(v, to, null));
            }
            progressTracker.logProgress();
            return;
        }

        if (!visited.get(event.eventNode())) {
            var v = event.eventNode();
            visited.set(v);
            var p = event.triggerNode();
            tin.set(v, timer);
            low.set(v, timer++);
            var parent_skipped = new AtomicBoolean(false);
            ///add post event (Should be before everything)
            if (p != -1) {
                stack.set(++stackIndex, StackEvent.lastVisit(v, p));
            }
            graph.forEachRelationship(v, (s, to) -> {
                if (to == p && !parent_skipped.get()) {
                    parent_skipped.set(true);
                    return true;
                }
                stack.set(++stackIndex,  StackEvent.upcomingVisit(to, v));

                return true;
            });

        } else {
            long v = event.triggerNode();
            long to = event.eventNode();
            var lowV = low.get(v);
            var tinTo = tin.get(to);
            low.set(v, Math.min(lowV, tinTo));
        }
    }


    record StackEvent(long eventNode, long triggerNode, boolean lastVisit) {
        static StackEvent upcomingVisit(long node, long triggerNode) {
            return new StackEvent(node, triggerNode, false);
        }

        static StackEvent lastVisit(long node, long triggerNode) {
            return new StackEvent(node, triggerNode, true);
        }
    }
}
