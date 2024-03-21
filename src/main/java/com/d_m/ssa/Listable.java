package com.d_m.ssa;

public interface Listable<T> {
    T getPrev();
    void setPrev(T prev);
    T getNext();
    void setNext(T next);
}
