package com.d_m.ssa;

import com.d_m.ast.Type;
import com.d_m.code.Operator;

public abstract class Constant extends Value {
    protected Constant(String name, Type type) {
        super(name, type);
    }

    public abstract Constant applyOp(Operator op, Constant other);
}
