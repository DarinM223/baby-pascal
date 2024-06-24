package com.d_m.gen;

import java.util.List;

public sealed interface Tree {
    record Node(String name, List<Tree> children) implements Tree {
    }

    record Bound(String name) implements Tree {
    }

    record Wildcard() implements Tree {
    }
}
