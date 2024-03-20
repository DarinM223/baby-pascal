package com.d_m.ssa;

import com.d_m.ast.Type;

import java.util.Iterator;
import java.util.Objects;

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

    public void addUse(Value user) {
        Use newUse = new Use(this, user);
        if (uses != null) {
            newUse.next = uses;
            uses.prev = newUse;
        }
        uses = newUse;
    }

    public void removeUse(Value user) {
        if (uses != null && uses.user.equals(user)) {
            if (uses.next != null) {
                uses.next.prev = null;
            }
            uses = uses.next;
        } else {
            var iterator = uses();
            while (iterator.hasNext()) {
                Use use = iterator.next();
                if (use.user.equals(user)) {
                    iterator.remove();
                }
            }
        }
    }

    public Iterator<Use> uses() {
        return new UsesIterator(uses);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Value value = (Value) o;
        return id == value.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
