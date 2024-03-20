package com.d_m.ssa;

import java.util.Iterator;

public class UsesIterator implements Iterator<Use> {
    private Use previousUse = null;
    private Use currentUse = null;

    public UsesIterator(Use currentUse) {
        this.previousUse = null;
        this.currentUse = currentUse;
    }

    @Override
    public boolean hasNext() {
        return currentUse != null;
    }

    @Override
    public Use next() {
        previousUse = currentUse;
        currentUse = currentUse.next;
        return previousUse;
    }

    @Override
    public void remove() {
        if (previousUse == null) {
            return;
        }

        if (previousUse.next != null) {
            previousUse.next.prev = previousUse.prev;
        }
        if (previousUse.prev != null) {
            previousUse.prev.next = previousUse.next;
        }
        previousUse = previousUse.prev;
    }
}
