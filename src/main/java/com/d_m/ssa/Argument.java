package com.d_m.ssa;

import com.d_m.ast.Type;

public class Argument extends Value {
    private Function parent;
    private final int argumentNumber;

    public Argument(String name, Type type, Function parent, int argumentNumber) {
        super(name, type);
        this.parent = parent;
        this.argumentNumber = argumentNumber;
    }

    public Function getParent() {
        return parent;
    }

    public void setParent(Function parent) {
        this.parent = parent;
    }

    public int getArgumentNumber() {
        return argumentNumber;
    }

    @Override
    public <T, E extends Exception> T accept(ValueVisitor<T, E> visitor) throws E {
        return visitor.visit(this);
    }
}
