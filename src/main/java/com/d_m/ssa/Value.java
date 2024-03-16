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

    public static class UsesIterator implements Iterator<Use> {
        private Use currentUse = null;

        private UsesIterator(Use currentUse) {
            this.currentUse = currentUse;
        }

        @Override
        public boolean hasNext() {
            return currentUse != null && currentUse.next != null;
        }

        @Override
        public Use next() {
            currentUse = currentUse.next;
            return currentUse;
        }

        @Override
        public void remove() {
            currentUse.next.prev = currentUse.prev;
            currentUse.prev.next = currentUse.next;
            currentUse = currentUse.prev;
            Iterator.super.remove();
        }
    }
}
