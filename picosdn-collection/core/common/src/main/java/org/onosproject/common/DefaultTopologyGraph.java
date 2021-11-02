/*
 * Copyright 2015-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.common;

import java.util.Set;
import java.util.UUID;

import org.onlab.graph.AdjacencyListsGraph;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyVertex;

/**
 * Default implementation of an immutable topology graph based on a generic
 * implementation of adjacency lists graph.
 */
public class DefaultTopologyGraph
        extends AdjacencyListsGraph<TopologyVertex, TopologyEdge>
        implements TopologyGraph {

    private UUID entityUuid;

    /**
     * Creates a topology graph comprising of the specified vertexes and edges.
     *
     * @param vertexes set of graph vertexes
     * @param edges    set of graph edges
     */
    public DefaultTopologyGraph(Set<TopologyVertex> vertexes,
            Set<TopologyEdge> edges) {
        super(vertexes, edges);
    }

    @Override
    public UUID getEntityUuid() {
        return entityUuid;
    }

    @Override
    public void setEntityUuid(UUID uuid) {
        entityUuid = uuid;
    }

}
