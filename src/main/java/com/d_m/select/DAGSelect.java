package com.d_m.select;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;

import java.util.*;
import java.util.function.Function;

public class DAGSelect<Node extends Comparable<Node>, TileImpl extends Tile<Node>, DAGImpl extends DAG<Node>> {
    private final Map<Node, TileImpl> bestChoiceForNodeTile;
    private final Map<Node, Integer> bestChoiceForNodeCost;
    private final Set<Node> fixedNodes;
    private final Set<TileImpl> matchedTiles;
    private final SetMultimap<Node, TileImpl> coveringTiles;
    private final DAGImpl dag;
    private final Function<Node, Set<TileImpl>> matchingTiles;

    public DAGSelect(DAGImpl dag, Function<Node, Set<TileImpl>> matchingTiles) {
        this.dag = dag;
        this.matchingTiles = matchingTiles;
        bestChoiceForNodeTile = new HashMap<>();
        bestChoiceForNodeCost = new HashMap<>();
        fixedNodes = new HashSet<>();
        matchedTiles = new LinkedHashSet<>();
        coveringTiles = LinkedHashMultimap.create();
    }

    public void select() {
        fixedNodes.clear();
        bottomUpDP();
        topDownSelect();
        improveCSEDecisions();
        bottomUpDP();
        topDownSelect();
    }

    public Collection<TileImpl> matchedTiles() {
        return matchedTiles;
    }

    public Collection<TileImpl> coveringTiles(Node node) {
        return coveringTiles.get(node);
    }

    private void bottomUpDP() {
        for (Node n : dag.postorder()) {
            bestChoiceForNodeCost.put(n, Integer.MAX_VALUE);
            for (TileImpl tile : matchingTiles.apply(n)) {
                boolean hasInteriorFixedNode = false;
                for (Node fixed : fixedNodes) {
                    hasInteriorFixedNode |= tile.contains(fixed);
                }

                if (!hasInteriorFixedNode) {
                    int val = tile.cost();
                    for (Node n2 : tile.edgeNodes()) {
                        val += bestChoiceForNodeCost.get(n2);
                    }

                    if (val < bestChoiceForNodeCost.get(n)) {
                        bestChoiceForNodeCost.put(n, val);
                        bestChoiceForNodeTile.put(n, tile);
                    }
                }
            }
        }
    }

    private void topDownSelect() {
        matchedTiles.clear();
        coveringTiles.clear();
        Queue<Node> queue = new LinkedList<>(dag.roots());
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            TileImpl bestTile = bestChoiceForNodeTile.get(node);
            matchedTiles.add(bestTile);
            for (Node coveredNode : bestTile.covered()) {
                coveringTiles.put(coveredNode, bestTile);
            }
            queue.addAll(bestTile.edgeNodes());
        }
    }

    private void improveCSEDecisions() {
        for (Node n : dag.sharedNodes()) {
            if (coveringTiles.containsKey(n) && coveringTiles.get(n).size() > 1) {
                int overlapCost = getOverlapCost(n);
                int cseCost = bestChoiceForNodeCost.get(n);
                for (TileImpl tile : coveringTiles.get(n)) {
                    cseCost += getTileCutCost(tile, n);
                }
                if (cseCost < overlapCost) {
                    fixedNodes.add(n);
                }
            }
        }
    }

    private int getOverlapCost(Node node) {
        int cost = 0;
        Set<TileImpl> seen = new HashSet<>();
        Queue<TileImpl> queue = new LinkedList<>();
        for (TileImpl tile : coveringTiles.get(node)) {
            queue.add(tile);
            seen.add(tile);
        }

        while (!queue.isEmpty()) {
            TileImpl tile = queue.poll();
            cost += tile.cost();
            for (Node otherNode : tile.edgeNodes()) {
                if (dag.reachable(node, otherNode)) {
                    TileImpl otherTile = bestChoiceForNodeTile.get(otherNode);
                    if (coveringTiles.get(otherNode).size() == 1) {
                        cost += bestChoiceForNodeCost.get(otherNode);
                    } else if (!seen.contains(otherTile)) {
                        seen.add(otherTile);
                        queue.add(otherTile);
                    }
                }
            }
        }
        return cost;
    }

    private int getTileCutCost(TileImpl tile, Node node) {
        int bestCost = Integer.MAX_VALUE;
        Node root = tile.root();
        for (TileImpl otherTile : matchingTiles.apply(root)) {
            if (otherTile.edgeNodes().contains(node)) {
                int cost = otherTile.cost();
                for (Node otherNode : otherTile.edgeNodes()) {
                    if (!otherNode.equals(node)) {
                        cost += bestChoiceForNodeCost.get(otherNode);
                    }
                }
                if (cost < bestCost) {
                    bestCost = cost;
                }
            }
        }
        // Subtract edge costs of original tile.
        for (Node otherNode : tile.edgeNodes()) {
            for (Set<Node> path : tile.paths(root, otherNode)) {
                if (!path.contains(node)) {
                    bestCost -= bestChoiceForNodeCost.get(otherNode);
                    break;
                }
            }
        }
        return bestCost;
    }
}
