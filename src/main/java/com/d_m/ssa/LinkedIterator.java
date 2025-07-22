package com.d_m.ssa;

import java.util.Iterator;
import java.util.function.Consumer;

public class LinkedIterator<T extends Listable<T>> implements Iterator<T> {
    private T prev;
    private T curr;
    private final Consumer<T> setHead;
    private final Consumer<T> setLast;

    public LinkedIterator(T curr, Consumer<T> setHead, Consumer<T> setLast) {
        this.prev = null;
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
        } else if (setLast != null) {
            setLast.accept(prev.getPrev());
        }
        if (prev.getPrev() != null) {
            prev.getPrev().setNext(prev.getNext());
        } else if (setHead != null) {
            setHead.accept(prev.getNext());
        }
        prev = prev.getPrev();
    }

    /**
     * Inserts specified element immediately before the element that would be returned by next().
     *
     * @param node
     */
    public void add(T node) {
        if (prev != null) {
            prev.setNext(node);
            node.setPrev(prev);
        } else if (setHead != null) {
            setHead.accept(node);
        }
        node.setNext(curr);
        if (curr != null) {
            curr.setPrev(node);
        } else if (setLast != null) {
            setLast.accept(node);
        }
        prev = node;
    }
}
