package com.d_m.pass;

import com.d_m.ssa.Constant;

public sealed interface Lattice {
    record NeverDefined() implements Lattice {
    }

    record Defined(Constant value) implements Lattice {
    }

    record Overdefined() implements Lattice {
    }
}
