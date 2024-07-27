package com.d_m.select;

import java.util.Collection;
import java.util.Set;

public interface Tile<Node> {
    Collection<Node> covered();

    Collection<Node> edgeNodes();

    boolean contains(Node node);

    int cost();

    Node root();
}
