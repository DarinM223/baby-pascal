package com.d_m.ssa;

import java.util.Iterator;
import java.util.function.Consumer;

public class ReverseLinkedIterator<T extends Listable<T>> implements Iterator<T>, Iterable<T> {
    private T next;
    private T curr;
    private final Consumer<T> setHead;
    private final Consumer<T> setLast;

    public ReverseLinkedIterator(T curr, Consumer<T> setHead, Consumer<T> setLast) {
        this.next = null;
        this.curr = curr;
        this.setHead = setHead;
        this.setLast = setLast;
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
        } else if (setHead != null) {
            setHead.accept(next.getNext());
        }
        if (next.getNext() != null) {
            next.getNext().setPrev(next.getPrev());
        } else if (setLast != null) {
            setLast.accept(next.getPrev());
        }
        next = next.getNext();
    }

    @Override
    public Iterator<T> iterator() {
        return this;
    }
}
