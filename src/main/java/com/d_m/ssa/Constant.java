package com.d_m.ssa;

import com.d_m.ast.Type;

public abstract class Constant extends Value {
    protected Constant(int id, String name, Type type) {
        super(id, name, type);
    }
}
