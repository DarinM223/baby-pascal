package com.d_m.select.dag;

public sealed interface NodeType {
    record Type(com.d_m.ast.Type type) implements NodeType {}
    record Token() implements NodeType {}
}
