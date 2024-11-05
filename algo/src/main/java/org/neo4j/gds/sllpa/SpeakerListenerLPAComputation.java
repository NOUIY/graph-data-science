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
package org.neo4j.gds.sllpa;

import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.LongIntScatterMap;
import com.carrotsearch.hppc.cursors.LongIntCursor;
import org.neo4j.gds.api.nodeproperties.ValueType;
import org.neo4j.gds.beta.pregel.Messages;
import org.neo4j.gds.beta.pregel.Pregel;
import org.neo4j.gds.beta.pregel.PregelComputation;
import org.neo4j.gds.beta.pregel.PregelSchema;
import org.neo4j.gds.beta.pregel.context.ComputeContext;
import org.neo4j.gds.beta.pregel.context.InitContext;
import org.neo4j.gds.mem.MemoryEstimateDefinition;
import org.neo4j.gds.utils.CloseableThreadLocal;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;

import static org.neo4j.gds.sllpa.SpeakerListenerLPA.LABELS_PROPERTY;

class SpeakerListenerLPAComputation implements PregelComputation<SpeakerListenerLPAConfig> {

    private final CloseableThreadLocal<SplittableRandom> random;

    SpeakerListenerLPAComputation(Optional<Long> seed) {
        var splittableRandom = new SplittableRandom(seed.orElse(System.currentTimeMillis()));
        random = CloseableThreadLocal.withInitial(splittableRandom::split);
    }

    @Override
    public void compute(ComputeContext<SpeakerListenerLPAConfig> context, Messages messages) {
        var labels = context.longArrayNodeValue(LABELS_PROPERTY);

        if (context.isInitialSuperstep()) {
            labels[0] = context.nodeId();
            context.sendToNeighbors(context.nodeId());
        } else if (context.superstep() < context.config().propagationSteps()) {
            listen(context, messages, labels);
            speak(context, labels);
        } else {
            listen(context, messages, labels);
            prune(context, labels);
        }
    }

    @Override
    public void init(InitContext<SpeakerListenerLPAConfig> context) {
        var initialLabels = new long[context.config().maxIterations()];
        // when nodes do not have incoming rels, it should vote for itself always
        Arrays.fill(initialLabels, context.nodeId());
        context.setNodeValue(LABELS_PROPERTY, initialLabels);
    }


    @Override
    public PregelSchema schema(SpeakerListenerLPAConfig config) {
        return new PregelSchema.Builder()
            .add(LABELS_PROPERTY, ValueType.LONG_ARRAY)
            .build();
    }

    @Override
    public MemoryEstimateDefinition estimateDefinition(boolean isAsynchronous) {
        return () -> Pregel.memoryEstimation(
            Map.of(LABELS_PROPERTY, ValueType.LONG_ARRAY),
            false,
            false
        );
    }

    private void listen(
        ComputeContext<SpeakerListenerLPAConfig> context,
        Messages messages,
        long[] labels
    ) {
        if (!messages.isEmpty()) {
            var labelVotes = new LongIntScatterMap();
            long winningLabel = 0;
            int maxFrequency = Integer.MIN_VALUE;
            for (Double message : messages) {
                var currentLabel = message.longValue();
                var updatedFrequency = labelVotes.addTo(currentLabel, 1);

                if (updatedFrequency > maxFrequency) {
                    winningLabel = currentLabel;
                    maxFrequency = updatedFrequency;
                } else if (updatedFrequency == maxFrequency && currentLabel < winningLabel) {
                    winningLabel = currentLabel;
                }
            }

            labels[context.superstep()] = winningLabel;
        }
    }

    private void speak(ComputeContext<SpeakerListenerLPAConfig> context, long[] labels) {
        context.forEachNeighbor(neighbor -> {
            var randomLabelPosition = random.get().nextInt(context.superstep() + 1);
            var labelToSend = labels[randomLabelPosition];
            context.sendTo(neighbor, labelToSend);
        });
    }

    // IDEA: Instead of just returning every community the current node is part of, keep the frequency of each community as a, sort of, weight
    private void prune(ComputeContext<SpeakerListenerLPAConfig> context, long[] labels) {
        var labelVotes = new LongIntScatterMap();
        for (long label : labels) {
            labelVotes.addTo(label, 1);
        }

        var labelsToKeep = new LongArrayList(labels.length);

        for (LongIntCursor labelVote : labelVotes) {
            var relativeFrequency = ((double) labelVote.value) / labels.length;
            if (relativeFrequency > context.config().minAssociationStrength()) {
                labelsToKeep.add(labelVote.key);
            }
        }

        context.setNodeValue(
            LABELS_PROPERTY,
            labelsToKeep.size() == labels.length ? labelsToKeep.buffer : labelsToKeep.toArray()
        );
    }

    @Override
    public void close() {
        random.close();
    }
}