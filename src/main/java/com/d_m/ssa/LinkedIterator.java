package com.d_m.ssa;

import java.util.Iterator;

public class LinkedIterator<T extends Listable<T>> implements Iterator<T>, Iterable<T> {
    private T prev = null;
    private T curr = null;

    public LinkedIterator(T curr) {
        this.prev = null;
        this.curr = curr;
    }

    @Override
    public boolean hasNext() {
        return curr != null;
    }

    @Override
    public T next() {
        prev = curr;
        curr = curr.getNext();
        return prev;
    }

    @Override
    public void remove() {
        if (prev == null) {
            return;
        }

        if (prev.getNext() != null) {
            prev.getNext().setPrev(prev.getPrev());
        }
        if (prev.getPrev() != null) {
            prev.getPrev().setNext(prev.getNext());
        }
        prev = prev.getPrev();
    }

    @Override
    public Iterator<T> iterator() {
        return this;
    }
}
