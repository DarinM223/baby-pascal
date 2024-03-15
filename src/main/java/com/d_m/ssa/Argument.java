package com.d_m.ssa;

import com.d_m.ast.Type;

public class Argument extends Value {
    private Function parent;
    private int argumentNumber;

    public Argument(Type type, String name, Function parent, int argumentNumber) {
        this.parent = parent;
        this.argumentNumber = argumentNumber;
    }
}
