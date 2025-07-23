package com.d_m.ssa;

import com.d_m.ast.Type;
import com.d_m.code.Operator;

public class Global extends Constant {
    private Module parent;

    public Global(String name, Type type, Module parent) {
        super(name, type);
        this.parent = parent;
    }

    @Override
    public <T, E extends Exception> T accept(ValueVisitor<T, E> visitor) throws E {
        return visitor.visit(this);
    }

    public Module getParent() {
        return parent;
    }

    public void setParent(Module parent) {
        this.parent = parent;
    }

    @Override
    public Constant applyOp(Operator op, Constant other) {
        throw new UnsupportedOperationException("Cannot apply operator to Global");
    }
}
