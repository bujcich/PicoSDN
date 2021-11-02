package edu.mit.ll.provsdnanalysis;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.io.Attribute;
import org.jgrapht.io.ComponentAttributeProvider;
import org.jgrapht.io.ComponentNameProvider;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.DefaultAttribute;
import org.jgrapht.io.ExportException;
import org.jgrapht.io.GraphExporter;

/**
 * Representation of W3C PROV provenance graph for ProvSDN v2.0.
 *
 * @author Benjamin Ujcich <benjamin.ujcich@ll.mit.edu> <ujcich2@illinois.edu>
 * @version 2.0
 */
public class W3CProvGraph {

    private DirectedAcyclicGraph<W3CProvGraphNode, W3CProvGraphEdge> g;
    private Stack<W3CProvGraphStashedEdge> stashedEdges = new Stack<W3CProvGraphStashedEdge>();

    public W3CProvGraph() {
        this.g = new DirectedAcyclicGraph<W3CProvGraphNode, W3CProvGraphEdge>(
                null);
    }

    /**
     * Add a node to the graph.
     *
     * @param nodeToAdd
     */
    public void addNode(W3CProvGraphNode nodeToAdd) {
        if (getNode(nodeToAdd.getUuid()) == null) {
            g.addVertex(nodeToAdd);
        }
    }

    /**
     * Get a node from the graph by its UUID.
     *
     * @param uuid
     * @return node or null
     */
    public W3CProvGraphNode getNode(String uuid) {
        for (W3CProvGraphNode node : g.vertexSet()) {
            if (node.getUuid().equals(uuid)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Get all nodes of provenance graph
     * 
     * @return nodes
     */
    public Set<W3CProvGraphNode> getNodes() {
        return g.vertexSet();
    }

    /**
     * Get all edges of provenance graph
     * 
     * @return edges
     */
    public Set<W3CProvGraphEdge> getEdges() {
        return g.edgeSet();
    }

    /**
     * Get nodes that contain a specific string value. For AND queries, use this
     * repeatedly to find two or more values contained within the node's value.
     * 
     * @return nodes
     */
    public Set<W3CProvGraphNode> getNodesByValue(String value,
            Set<W3CProvGraphNode> nodes) {
        HashSet<W3CProvGraphNode> returnNodes = new HashSet<W3CProvGraphNode>();
        for (W3CProvGraphNode node : nodes) {
            if (node.getValue().contains(value)) {
                returnNodes.add(node);
            }
        }
        return returnNodes;
    }

    /**
     * Perform a common ancestry trace with one or more pieces of evidence.
     * 
     * @param evidenceSet
     * @return List with sets of agents, activities, and entities
     */
    public List<Set<W3CProvGraphNode>> commonAncestryTrace(
            Set<W3CProvGraphNode> evidenceSet) {

        stashNonDependencyEdges();

        List<Set<W3CProvGraphNode>> commonAncestorsList = new ArrayList<Set<W3CProvGraphNode>>();
        Set<W3CProvGraphNode> commonAncestors = new HashSet<W3CProvGraphNode>();

        // start out by adding every node in graph to commonAncestors
        for (W3CProvGraphNode node : g.vertexSet()) {
            commonAncestors.add(node);
        }

        /*
         * for each piece of evidence, get its ancestry and intersect with
         * commonAncestors
         */
        for (W3CProvGraphNode evidence : evidenceSet) {
            commonAncestors.retainAll(g.getDescendants(evidence));
        }

        commonAncestorsList.add(filterAgents(commonAncestors));
        commonAncestorsList.add(filterActivities(commonAncestors));
        commonAncestorsList.add(filterEntities(commonAncestors));

        unstashNonDependencyEdges();

        return commonAncestorsList;

    }

    /**
     * Perform a network activity summarization. In essence, this describes what
     * data plane activities caused flow rules to be (or not to be) installed.
     */
    public void networkActivitySummarization() {

        stashNonDependencyEdges();

        // get all activities
        Set<W3CProvGraphNode> activities = filterActivities(g.vertexSet());

        // for each activity
        for (W3CProvGraphNode activity : activities) {

            // are there any data plane packet ancestors? (packets)
            Set<W3CProvGraphNode> packets = new HashSet<W3CProvGraphNode>();
            Set<W3CProvGraphNode> ancestors = getAncestryOfNode(activity);
            for (W3CProvGraphNode ancestor : ancestors) {
                if (isInboundPacket(ancestor)) {
                    packets.add(ancestor);
                }
            }

            // are there any immediate data plane packet ancestors?
            // (immediatePackets)
            Set<W3CProvGraphNode> immediatePackets = new HashSet<W3CProvGraphNode>();

            Set<W3CProvGraphEdge> edges = g.outgoingEdgesOf(activity);
            for (W3CProvGraphEdge edge : edges) {
                W3CProvGraphNode target = g.getEdgeTarget(edge);
                if (isInboundPacket(target)) {
                    immediatePackets.add(target);
                }
            }

            // are there any immediate flow rule / forwarding objective
            // descendants? (immediateFlows)
            Set<W3CProvGraphNode> immediateFlows = new HashSet<W3CProvGraphNode>();
            edges = g.edgesOf(activity);
            for (W3CProvGraphEdge edge : edges) {
                W3CProvGraphNode source = g.getEdgeSource(edge);
                if (isFlowRule(source) || isForwardingObjective(source)) {
                    immediateFlows.add(source);
                }
            }

        }

        unstashNonDependencyEdges();

    }

    /**
     * Get ancestry of node (i.e., all things that went into affecting this
     * node).
     * 
     * @param node
     * @return
     */
    public Set<W3CProvGraphNode> getAncestryOfNode(W3CProvGraphNode node) {
        /*
         * Note that W3C PROV has opposite definition of ancestor from jGraphT,
         * so we're actually getting descendants.
         */
        return g.getDescendants(node);
    }

    /**
     * Get descendants of node (i.e., all things this node affected).
     * 
     * @param node
     * @return
     */
    public Set<W3CProvGraphNode> getDescendantsOfNode(W3CProvGraphNode node) {
        /*
         * Note that W3C PROV has opposite definition of descendant from
         * jGraphT, so we're actually getting ancestors.
         */
        return g.getAncestors(node);
    }

    /**
     * Filter out nodes that are W3C PROV entities.
     *
     * @param nodes
     * @return filter
     */
    public Set<W3CProvGraphNode> filterEntities(Set<W3CProvGraphNode> nodes) {
        HashSet<W3CProvGraphNode> filter = new HashSet<W3CProvGraphNode>();
        for (W3CProvGraphNode node : nodes) {
            if (node.getType().equals("entity")) {
                filter.add(node);
            }
        }
        return filter;
    }

    /**
     * Filter out nodes that are W3C PROV activities.
     *
     * @param nodes
     * @return filter
     */
    public Set<W3CProvGraphNode> filterActivities(Set<W3CProvGraphNode> nodes) {
        HashSet<W3CProvGraphNode> filter = new HashSet<W3CProvGraphNode>();
        for (W3CProvGraphNode node : nodes) {
            if (node.getType().equals("activity")) {
                filter.add(node);
            }
        }
        return filter;
    }

    /**
     * Filter out nodes that are W3C PROV agents.
     *
     * @param nodes
     * @return filter
     */
    public Set<W3CProvGraphNode> filterAgents(Set<W3CProvGraphNode> nodes) {
        HashSet<W3CProvGraphNode> filter = new HashSet<W3CProvGraphNode>();
        for (W3CProvGraphNode node : nodes) {
            if (node.getType().equals("agent")) {
                filter.add(node);
            }
        }
        return filter;
    }

    /**
     * Add an edge to the graph. If edge already exists, the existing edge is
     * removed and the new edge is added. Loops and duplicate edges are
     * discarded.
     *
     * @param from
     * @param to
     * @param edge
     */
    public void addEdge(W3CProvGraphNode from, W3CProvGraphNode to,
            W3CProvGraphEdge edge) {
        // don't allow loops
        if (from.getUuid().equals(to.getUuid())) {
            return;
        }
        // don't allow duplicate edges
        if (!g.containsEdge(from, to)) {
            try {
                // may induce loop, so don't add edge if it does
                g.addEdge(from, to, edge);
            } catch (IllegalArgumentException e) {
            }
        } else {
            g.removeEdge(from, to);
            g.addEdge(from, to, edge);
        }
    }

    /**
     * Remove an edge from the provenance graph.
     *
     * @param node
     */
    private void removeEdge(W3CProvGraphEdge edge) {
        g.removeEdge(edge);
    }

    /**
     * Remove a node from the provenance graph.
     *
     * @param node
     */
    private void removeNode(W3CProvGraphNode node) {
        g.removeVertex(node);
    }

    /**
     * Temporarily stash non-dependency edges (e.g., revision relations,
     * invalidation relations, etc.) from the provenance graph so as to perform
     * backward-forward dependency tracing correctly.
     */
    public void stashNonDependencyEdges() {
        for (W3CProvGraphEdge edge : g.edgeSet()) {
            if (edge.getType().equals("wasRevisionOf")
                    || edge.getType().equals("invalidates")) {
                W3CProvGraphNode from = g.getEdgeSource(edge);
                W3CProvGraphNode to = g.getEdgeTarget(edge);
                stashedEdges.push(new W3CProvGraphStashedEdge(from, to, edge));
                removeEdge(edge);
            }
        }
    }

    /**
     * Add stashed non-dependency edges back into the provenance graph.
     */
    public void unstashNonDependencyEdges() {
        while (!stashedEdges.isEmpty()) {
            W3CProvGraphStashedEdge stashedEdge = stashedEdges.pop();
            addEdge(stashedEdge.getFrom(), stashedEdge.getTo(),
                    stashedEdge.getEdge());
        }
    }

    /**
     * Perform an entity-centric backward-forward trace.
     *
     * Starting at a node of interest, get the set of ancestors of that node.
     * For each ancestor (i.e., a potential root cause), generate that
     * ancestor's set of descendants (i.e., what it affected).
     *
     * @param node node of interest
     * @return map of ancestor, descendants
     */
    public Map<W3CProvGraphNode, Set<W3CProvGraphNode>> backwardForwardTrace(
            W3CProvGraphNode node) {

        stashNonDependencyEdges();

        HashMap<W3CProvGraphNode, Set<W3CProvGraphNode>> results = new HashMap<W3CProvGraphNode, Set<W3CProvGraphNode>>();

        Set<W3CProvGraphNode> ancestors = filterEntities(g.getAncestors(node));
        for (W3CProvGraphNode ancestor : ancestors) {
            Set<W3CProvGraphNode> descendants = filterEntities(
                    g.getDescendants(ancestor));
            results.put(ancestor, descendants);
        }

        unstashNonDependencyEdges();

        return results;
    }

    /**
     * Display information about the provenance graph.
     */
    public void pp() {
        System.out.printf("Number of nodes: %s\n", g.vertexSet().size());
        System.out.printf("Number of edges: %s\n", g.edgeSet().size());
    }

    /**
     * Remove any nodes that do not have any edges connecting to them.
     */
    public void removeOrphanNodes() {
        HashSet<W3CProvGraphNode> setToRemove = new HashSet<W3CProvGraphNode>();
        for (W3CProvGraphNode node : g.vertexSet()) {
            if (g.edgesOf(node).size() == 0) {
                setToRemove.add(node);
            }
        }
        for (W3CProvGraphNode node : setToRemove) {
            removeNode(node);
        }
    }

    /**
     * Remove any activity nodes that do not have any effects (i.e., if an
     * activity does not contain any edges with generation or invalidation,
     * remove the activity)
     *
     * Note that removal may change the semantic meaning of the graph. For
     * instance, if we want to represent that the activity *did* receive an
     * entity (even if it didn't have an effect on generating/invalidating
     * entities), we might want to keep that activity. Otherwise, the lack of
     * that activity being represented might imply an error with that activity
     * being able to receive an entity.
     */
    public void removeActivitiesWithoutEffect() {
        HashSet<W3CProvGraphNode> setToRemove = new HashSet<W3CProvGraphNode>();
        for (W3CProvGraphNode activity : filterActivities(g.vertexSet())) {
            Set<W3CProvGraphEdge> edges = g.edgesOf(activity);
            Boolean remove = true;
            for (W3CProvGraphEdge edge : edges) {
                if (edge.getType().equals("wasGeneratedBy")
                        || edge.getType().equals("invalidates")) {
                    remove = false;
                    break;
                }
            }
            if (remove) {
                setToRemove.add(activity);
            }

        }
        for (W3CProvGraphNode node : setToRemove) {
            removeNode(node);
        }
    }

    /**
     * Add agency for each event listener to the app that called it.
     */
    public void addAppAgency() {

        Map<W3CProvGraphNode, W3CProvGraphNode> agentMap = new LinkedHashMap<W3CProvGraphNode, W3CProvGraphNode>();

        Set<W3CProvGraphNode> activities = filterActivities(g.vertexSet());

        for (W3CProvGraphNode activity : activities) {

            String value = activity.getValue();
            String app = value.substring(0, value.indexOf("$"));

            // remove special characters so dot doesn't complain
            W3CProvGraphNode agent = new W3CProvGraphNode(
                    app.replaceAll("[^a-zA-Z0-9]", ""), "agent", app,
                    app.replaceAll("[^a-zA-Z0-9]", ""));

            agentMap.put(activity, agent);

        }

        /*
         * iterate through agents and add nodes + relations
         */
        for (Map.Entry<W3CProvGraphNode, W3CProvGraphNode> entry : agentMap
                .entrySet()) {
            addNode(entry.getValue());

            /* attempt to add edge */
            W3CProvGraphEdge edge = new W3CProvGraphEdge("wasAssociatedWith",
                    "", "0");
            W3CProvGraphNode from = getNode(entry.getKey().getUuid());
            W3CProvGraphNode to = getNode(entry.getValue().getUuid());
            if (from != null && to != null) {
                addEdge(from, to, edge);
            }

        }

    }

    /**
     * Remove derivation relations from InboundPackets to flows. This, in
     * combination with switchport agency, reduces provenance dependency
     * explosion.
     */
    public void removePacketFlowDependency() {

        Set<W3CProvGraphNode> nodes = g.vertexSet();
        for (W3CProvGraphNode node : nodes) {
            if (isInboundPacket(node)) {
                // get outgoing edges from packet
                Set<W3CProvGraphEdge> edges = g.outgoingEdgesOf(node);
                // remove any edges that are derivations
                for (W3CProvGraphEdge edge : edges) {
                    if (edge.getType().equals("wasDerivedFrom")) {
                        removeEdge(edge);
                    }
                }
            }
        }

    }

    /**
     * Add agent nodes and attribution to each InboundPacket. This, in
     * combination with removing packet/flow dependencies, reduces provenance
     * dependency explosion.
     */
    public void addSwitchportAgency() {

        Map<W3CProvGraphNode, W3CProvGraphNode> agentMap = new LinkedHashMap<W3CProvGraphNode, W3CProvGraphNode>();

        /*
         * Iterate through nodes to find packets; note that we cannot add nodes
         * to graph in this loop because it throws off iterator, so store them
         * and add later
         */
        Set<W3CProvGraphNode> nodes = g.vertexSet();
        for (W3CProvGraphNode node : nodes) {
            if (isInboundPacket(node)) {

                String value = node.getValue();
                String port = value.substring(value.indexOf("=") + 1,
                        value.indexOf(","));

                // remove special characters so dot doesn't complain
                W3CProvGraphNode agent = new W3CProvGraphNode(
                        port.replaceAll("[^a-zA-Z0-9]", ""), "agent", port,
                        port.replaceAll("[^a-zA-Z0-9]", ""));

                agentMap.put(node, agent);

            }
        }

        /*
         * iterate through agents and add nodes + relations
         */
        for (Map.Entry<W3CProvGraphNode, W3CProvGraphNode> entry : agentMap
                .entrySet()) {
            addNode(entry.getValue());

            /* attempt to add edge */
            W3CProvGraphEdge edge = new W3CProvGraphEdge("wasAttributedTo", "",
                    "0");
            W3CProvGraphNode from = getNode(entry.getKey().getUuid());
            W3CProvGraphNode to = getNode(entry.getValue().getUuid());
            if (from != null && to != null) {
                addEdge(from, to, edge);
            }

        }

    }

    /**
     * Helper function to determine if node is an inbound packet
     * 
     * @param node
     * @return
     */
    private boolean isInboundPacket(W3CProvGraphNode node) {
        if (node.getValue().contains("DefaultInboundPacket")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Helper function to determine if node is a flow rule
     * 
     * @param node
     * @return
     */
    private boolean isFlowRule(W3CProvGraphNode node) {
        if (node.getValue().contains("DefaultFlowRule")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Helper function to determine if node is a forwarding objective
     * 
     * @param node
     * @return
     */
    private boolean isForwardingObjective(W3CProvGraphNode node) {
        if (node.getValue().contains("DefaultForwardingObjective")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Generate graphviz/dot output of provenance graph.
     */
    public void generateDot() {

        ComponentNameProvider<W3CProvGraphNode> nodeIdProvider = new ComponentNameProvider<W3CProvGraphNode>() {
            public String getName(W3CProvGraphNode node) {
                return "node" + node.getUuid().toString().replace("-", "");
            }
        };

        ComponentNameProvider<W3CProvGraphNode> nodeLabelProvider = new ComponentNameProvider<W3CProvGraphNode>() {
            public String getName(W3CProvGraphNode node) {
                if (node.getType().equals("activity")) {
                    return node.getValue().replace("$", "\n") + "\nts="
                            + node.getTs();
                } else if (node.getType().equals("entity")) {
                    return node.getValue().replace(",", "\n") + "\nts="
                            + node.getTs();
                } else {
                    return node.getValue();
                }
            }
        };

        ComponentNameProvider<W3CProvGraphEdge> edgeLabelProvider = new ComponentNameProvider<W3CProvGraphEdge>() {
            public String getName(W3CProvGraphEdge edge) {
                return edge.getType();
            }
        };

        ComponentAttributeProvider<W3CProvGraphNode> nodeAttributeProvider = new ComponentAttributeProvider<W3CProvGraphNode>() {
            public Map<String, Attribute> getComponentAttributes(
                    W3CProvGraphNode node) {
                Map<String, Attribute> map = new LinkedHashMap<String, Attribute>();
                map.put("style", DefaultAttribute.createAttribute("filled"));
                map.put("penwidth", DefaultAttribute.createAttribute("1"));
                map.put("fontsize", DefaultAttribute.createAttribute("12"));
                if (node.getType().equals("agent")) {
                    map.put("shape", DefaultAttribute.createAttribute("house"));
                    map.put("fillcolor",
                            DefaultAttribute.createAttribute("rosybrown1"));
                } else if (node.getType().equals("activity")) {
                    map.put("shape", DefaultAttribute.createAttribute("box"));
                    map.put("fillcolor", DefaultAttribute
                            .createAttribute("lightsteelblue1"));
                } else if (node.getType().equals("entity")) {
                    map.put("shape",
                            DefaultAttribute.createAttribute("ellipse"));
                    map.put("fillcolor",
                            DefaultAttribute.createAttribute("khaki1"));
                }
                return map;
            }
        };

        ComponentAttributeProvider<W3CProvGraphEdge> edgeAttributeProvider = new ComponentAttributeProvider<W3CProvGraphEdge>() {
            public Map<String, Attribute> getComponentAttributes(
                    W3CProvGraphEdge edge) {
                Map<String, Attribute> map = new LinkedHashMap<String, Attribute>();
                map.put("fontsize", DefaultAttribute.createAttribute("12"));
                return map;
            }
        };

        GraphExporter<W3CProvGraphNode, W3CProvGraphEdge> exporter = new DOTExporter<W3CProvGraphNode, W3CProvGraphEdge>(
                nodeIdProvider, nodeLabelProvider, edgeLabelProvider,
                nodeAttributeProvider, edgeAttributeProvider);
        try {
            // write out to DOT
            FileWriter fw = new FileWriter("prov-graph.dot");
            Writer writer = new StringWriter();
            /* graph-wide attributes */
            ((DOTExporter<W3CProvGraphNode, W3CProvGraphEdge>) exporter)
                    .putGraphAttribute("ratio", "compress");
            ((DOTExporter<W3CProvGraphNode, W3CProvGraphEdge>) exporter)
                    .putGraphAttribute("nodesep", "0.2");
            ((DOTExporter<W3CProvGraphNode, W3CProvGraphEdge>) exporter)
                    .putGraphAttribute("ranksep", "1");
            ((DOTExporter<W3CProvGraphNode, W3CProvGraphEdge>) exporter)
                    .putGraphAttribute("margin", "0");
            ((DOTExporter<W3CProvGraphNode, W3CProvGraphEdge>) exporter)
                    .putGraphAttribute("rankdir", "BT");

            exporter.exportGraph(g, writer);
            fw.write(writer.toString());
            fw.close();
            // write out to PDF
            String stringExec = "dot -Tpdf prov-graph.dot -o prov-graph.pdf";
            Runtime.getRuntime()
                    .exec(new String[] { "bash", "-c", stringExec });
            stringExec = "dot -Tsvg prov-graph.dot -o prov-graph.svg";
            Runtime.getRuntime()
                    .exec(new String[] { "bash", "-c", stringExec });
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (ExportException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Representation of a stashed edge in the provenance graph.
     */
    public class W3CProvGraphStashedEdge {
        private W3CProvGraphNode from;
        private W3CProvGraphNode to;
        private W3CProvGraphEdge edge;

        public W3CProvGraphStashedEdge(W3CProvGraphNode from,
                W3CProvGraphNode to, W3CProvGraphEdge edge) {
            super();
            this.from = from;
            this.to = to;
            this.edge = edge;
        }

        public W3CProvGraphNode getFrom() {
            return from;
        }

        public void setFrom(W3CProvGraphNode from) {
            this.from = from;
        }

        public W3CProvGraphNode getTo() {
            return to;
        }

        public void setTo(W3CProvGraphNode to) {
            this.to = to;
        }

        public W3CProvGraphEdge getEdge() {
            return edge;
        }

        public void setEdge(W3CProvGraphEdge edge) {
            this.edge = edge;
        }

    }

}
