package com.d_m.ssa;

public class Use implements Listable<Use> {
    protected Value value;
    protected Value user;
    protected Use next;
    protected Use prev;

    public Use(Value value, Value user) {
        this.value = value;
        this.user = user;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public Value getUser() {
        return user;
    }

    public void setUser(Value user) {
        this.user = user;
    }

    @Override
    public Use getNext() {
        return next;
    }

    @Override
    public void setNext(Use next) {
        this.next = next;
    }

    @Override
    public Use getPrev() {
        return prev;
    }

    @Override
    public void setPrev(Use prev) {
        this.prev = prev;
    }
}
