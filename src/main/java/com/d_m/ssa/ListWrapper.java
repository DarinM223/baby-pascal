package com.d_m.ssa;

import com.google.common.collect.Iterables;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ListWrapper<T extends Listable<T>> implements Iterable<T> {
    private T first = null;
    private T last = null;

    public T getFirst() {
        return first;
    }

    public T getLast() {
        return last;
    }

    void setFirst(T first) {
        this.first = first;
    }

    void setLast(T last) {
        this.last = last;
    }

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
        if (list.last != null) {
            last = list.last;
        }
    }

    public LinkedIterator<T> linkedIterator() {
        return new LinkedIterator<>(first, (T node) -> first = node, (T node) -> last = node);
    }

    @Override
    public Iterator<T> iterator() {
        return linkedIterator();
    }

    public ReverseLinkedIterator<T> reversed() {
        return new ReverseLinkedIterator<>(last, (T node) -> first = node, (T node) -> last = node);
    }

    /**
     * Converts a list wrapper to a list. Should only be used for testing.
     *
     * @return a list representation of the linked list.
     */
    public List<T> toList() {
        return StreamSupport.stream(spliterator(), false).collect(Collectors.toList());
    }
}
