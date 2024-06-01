package com.d_m.ssa;

import java.util.Iterator;

public class ReverseLinkedIterator<T extends Listable<T>> implements Iterator<T> {
    private T next = null;
    private T curr = null;

    public ReverseLinkedIterator(T curr) {
        this.next = null;
        this.curr = curr;
    }

    @Override
    public boolean hasNext() {
        return curr != null;
    }

    @Override
    public T next() {
        next = curr;
        curr = curr.getPrev();
        return next;
    }

    @Override
    public void remove() {
        if (next == null) {
            return;
        }

        if (next.getPrev() != null) {
            next.getPrev().setNext(next.getNext());
        }
        if (next.getNext() != null) {
            next.getNext().setPrev(next.getPrev());
        }
        next = next.getNext();
    }
}
