package com.d_m.ssa;

import com.d_m.ast.Type;

public class Argument extends Value {
    private Function parent;
    private int argumentNumber;

    public Argument(int id, String name, Type type, Function parent, int argumentNumber) {
        super(id, name, type);
        this.parent = parent;
        this.argumentNumber = argumentNumber;
    }
}
