package com.d_m.ssa;

import com.d_m.ast.Type;
import com.d_m.code.Operator;
import com.d_m.util.Fresh;

public abstract class Constant extends Value {
    protected Constant(int id, String name, Type type) {
        super(id, name, type);
    }

    protected abstract Constant applyOp(Fresh fresh, Operator op, Constant other);
}
