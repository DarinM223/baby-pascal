package com.d_m.gen;

import java.util.List;

public sealed interface Tree {
    record Node(Token name, List<Tree> children) implements Tree {
    }

    record Bound(Token name) implements Tree {
    }

    record Wildcard() implements Tree {
    }
}
