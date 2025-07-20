package com.d_m.ssa;

import com.google.common.collect.Iterables;

import java.util.Iterator;

public class ListWrapper<T extends Listable<T>> implements Iterable<T> {
    public T first = null;
    public T last = null;

    public void clear() {
        this.first = null;
        this.last = null;
    }

    public boolean contains(T node) {
        return Iterables.contains(this, node);
    }

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

    public void addAfter(T node, T add) {
        T next = node.getNext();
        node.setNext(add);
        add.setPrev(node);
        add.setNext(next);
        if (next != null) {
            next.setPrev(add);
        } else {
            last = add;
        }
    }

    public void addBefore(T node, T add) {
        T prev = node.getPrev();
        node.setPrev(add);
        add.setNext(node);
        add.setPrev(prev);
        if (prev != null) {
            prev.setNext(add);
        } else {
            first = add;
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

    public void addToEnd(T node) {
        if (last == null) {
            first = node;
        } else {
            last.setNext(node);
            node.setPrev(last);
            node.setNext(null);
        }
        last = node;
    }

    public void append(ListWrapper<T> list) {
        if (last == null) {
            first = list.first;
        } else {
            last.setNext(list.first);
            if (list.first != null) {
                list.first.setPrev(last);
            }
        }
        last = list.last;
    }

    public LinkedIterator<T> linkedIterator() {
        return new LinkedIterator<>(first, (T node) -> first = node, (T node) -> last = node);
    }

    @Override
    public Iterator<T> iterator() {
        return linkedIterator();
    }

    public ReverseLinkedIterator<T> reversed() {
        return new ReverseLinkedIterator<>(last);
    }
}
