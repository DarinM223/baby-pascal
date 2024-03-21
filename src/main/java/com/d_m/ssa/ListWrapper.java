package com.d_m.ssa;

import java.util.Iterator;

public class ListWrapper<T extends Listable<T>> implements Iterable<T> {
    public T first = null;
    public T last = null;

    public void addToFront(T node) {
        node.setPrev(null);
        node.setNext(first);
        if (first != null) {
            first.setPrev(node);
        }
        first = node;
        if (last == null) {
            last = node;
        }
    }

    public void addBeforeLast(T node) {
        if (last != null) {
            if (last.getPrev() != null) {
                last.getPrev().setNext(node);
            }
            node.setPrev(last.getPrev());
            node.setNext(last);
            last.setPrev(node);
            if (last.equals(first)) {
                first = node;
            }
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new LinkedIterator<>(first);
    }
}
