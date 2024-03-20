package com.d_m.ssa;

import java.util.Iterator;

public class UsesIterator implements Iterator<Use> {
    private Use currentUse = null;

    public UsesIterator(Use currentUse) {
        this.currentUse = currentUse;
    }

    @Override
    public boolean hasNext() {
        return currentUse != null;
    }

    @Override
    public Use next() {
        currentUse = currentUse.next;
        return currentUse;
    }

    @Override
    public void remove() {
        if (currentUse.next != null) {
            currentUse.next.prev = currentUse.prev;
        }
        currentUse.prev.next = currentUse.next;
        currentUse = currentUse.prev;
        Iterator.super.remove();
    }
}
