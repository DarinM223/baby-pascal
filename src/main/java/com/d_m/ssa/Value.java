package com.d_m.ssa;

import com.d_m.ast.Type;

import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;

public abstract class Value {
    protected final int id;
    protected String name;
    protected Type type;
    protected Use uses = null;

    protected Value(String name, Type type) {
        this.id = IdGenerator.newId();
        this.name = name;
        this.type = type;
    }

    public void linkUse(Use use) {
        if (uses != null) {
            use.next = uses;
            uses.prev = use;
        }
        uses = use;
    }

    public void removeUse(Value user) {
        if (uses != null && uses.user.equals(user)) {
            if (uses.next != null) {
                uses.next.prev = null;
            }
            uses = uses.next;
        } else {
            var iterator = uses().iterator();
            while (iterator.hasNext()) {
                Use use = iterator.next();
                if (use.user.equals(user)) {
                    iterator.remove();
                }
            }
        }
    }

    public void replaceUsesWith(Value value) {
        for (Use use : uses()) {
            removeUse(use.getUser());
            use.setValue(value);
            value.linkUse(use);
        }
    }

    public Iterable<Use> uses() {
        return new UseIterable();
    }

    private class UseIterable implements Iterable<Use> {
        @Override
        public Iterator<Use> iterator() {
            return new LinkedIterator<>(uses);
        }
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

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public abstract void acceptDef(PrettyPrinter printer) throws IOException;

    public abstract void acceptUse(PrettyPrinter printer) throws IOException;
}
