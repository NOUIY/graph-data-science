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
package org.neo4j.gds.pricesteiner;

import org.neo4j.gds.collections.ha.HugeObjectArray;
import org.neo4j.gds.core.utils.queue.HugeLongPriorityQueue;

 class EdgeEventsQueue {

    private final HugeObjectArray<PairingHeap> pairingHeaps;
    private final HugeLongPriorityQueue edgeEventsPriorityQueue;
    private long currentlyActive;

    EdgeEventsQueue(long nodeCount){

        this.pairingHeaps= HugeObjectArray.newArray(PairingHeap.class, 2*nodeCount);
        this.edgeEventsPriorityQueue = HugeLongPriorityQueue.min(2*nodeCount);

        for (int i=0;i<nodeCount;i++){
            pairingHeaps.set(i, new PairingHeap());
        }
        currentlyActive = nodeCount;
    }
    long currentlyActive(){
        return currentlyActive;
    }
    double nextEventTime(){
        return  edgeEventsPriorityQueue.cost(edgeEventsPriorityQueue.top());
    }

    long top(){
        return edgeEventsPriorityQueue.top();
    }

    long topEdgePart(){
      return pairingHeaps.get(top()).minElement();
    }

    void pop(){
        var top=top();
        edgeEventsPriorityQueue.pop();
        var relevantHeap = pairingHeaps.get(top);
        relevantHeap.pop();

        if (!relevantHeap.empty()){
            edgeEventsPriorityQueue.set(top, relevantHeap.minValue());
        }
    }

    void addBothWays(long s, long t, long edgePart1, long edgePart2, double w){
        var pairingHeapOfs  = pairingHeaps.get(s);
        var pairingHeapOft  = pairingHeaps.get(t);
        pairingHeapOfs.add(edgePart1,w);
        pairingHeapOft.add(edgePart2,w);
    }

     void addWithCheck(long s,  long edgePart, double w){
         var pairingHeapOfs  = pairingHeaps.get(s);
         pairingHeapOfs.add(edgePart,w);
         if (w < edgeEventsPriorityQueue.cost(s)){
               edgeEventsPriorityQueue.set(s, w);
         }
     }
     void addWithoutCheck(long s,  long edgePart, double w){
         var pairingHeapOfs  = pairingHeaps.get(s);
         pairingHeapOfs.add(edgePart,w);
     }


     void increaseValuesOnInactiveCluster(long clusterId, double value){
        pairingHeaps.get(clusterId).increaseValues(value);
    }

    void mergeAndUpdate(long newCluster, long cluster1,long cluster2){
        pairingHeaps.set(newCluster,pairingHeaps.get(cluster1).join(pairingHeaps.get(cluster2)));

        deactivateCluster(cluster1);
        deactivateCluster(cluster2);

        edgeEventsPriorityQueue.add(newCluster, pairingHeaps.get(newCluster).minValue());
        currentlyActive--;
    }

    void deactivateCluster(long clusterId){
        // very-very bad way of removing from common heap-of-heaps
        edgeEventsPriorityQueue.set(clusterId,Double.MAX_VALUE); //ditto

    }

    void performInitialAssignment(long nodeCount){
        for (long u=0; u<nodeCount; u++){
            if (!pairingHeaps.get(u).empty()) {
                edgeEventsPriorityQueue.add(u, pairingHeaps.get(u).minValue());
            }
        }
    }

    double minOf(long clusterId){
        return  edgeEventsPriorityQueue.cost(clusterId);
    }

}