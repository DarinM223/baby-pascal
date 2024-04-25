package com.d_m.ssa;

import com.d_m.ast.Type;

import java.io.IOException;

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
    public void acceptDef(PrettyPrinter printer) throws IOException {
        printer.writeArgument(this);
    }

    @Override
    public void acceptUse(PrettyPrinter printer) throws IOException {
        printer.writeArgumentUse(this);
    }
}
