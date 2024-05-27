package com.d_m.select;

import java.util.Collection;

public interface DAG<Node> {
    Collection<Node> reverseTopologicalSort();

    Collection<Node> roots();

    Collection<Node> sharedNodes();

    boolean reachable(Node source, Node destination);
}
