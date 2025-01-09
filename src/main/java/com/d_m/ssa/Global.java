package com.d_m.ssa;

import com.d_m.ast.Type;

import java.io.IOException;

public class Global extends Value {
    private Module parent;

    public Global(String name, Type type, Module parent) {
        super(name, type);
        this.parent = parent;
    }

    @Override
    public void acceptDef(PrettyPrinter printer) throws IOException {
        printer.writeArgument(this);
    }

    @Override
    public void acceptUse(PrettyPrinter printer) throws IOException {
        printer.writeArgumentUse(this);
    }

    public Module getParent() {
        return parent;
    }

    public void setParent(Module parent) {
        this.parent = parent;
    }
}
