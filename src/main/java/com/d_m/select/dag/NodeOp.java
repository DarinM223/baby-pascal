package com.d_m.select.dag;

import java.util.List;

public sealed interface NodeOp {
    record Operator(com.d_m.code.Operator op) implements NodeOp {
    }

    /**
     * Initial entry token node.
     */
    record Entry() implements NodeOp {
    }

    /**
     * Merge multiple SDValues into a node.
     */
    record Merge(List<NodeType> outputTypes) implements NodeOp {
    }
}
