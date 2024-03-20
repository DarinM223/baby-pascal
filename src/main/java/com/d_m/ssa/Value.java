package com.d_m.ssa;

import com.d_m.ast.Type;

import java.util.Iterator;

public abstract class Value {
    protected final int id;
    protected String name;
    protected Type type;
    protected Use uses = null;

    protected Value(int id, String name, Type type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public Iterator<Use> uses() {
        return new UsesIterator(uses);
    }
}
