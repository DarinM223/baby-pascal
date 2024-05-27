package com.d_m.select.dag;

public sealed interface NodeType {
    record Operator(com.d_m.code.Operator op) implements NodeType {
    }

    /**
     * Initial entry token node.
     */
    record Entry() implements NodeType {
    }
}
