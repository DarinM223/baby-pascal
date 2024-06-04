package com.d_m.select.dag;

import com.d_m.ssa.Listable;

public class SDUse implements Listable<SDUse> {
    SDValue value;
    SDNode user;
    SDUse next;
    SDUse prev;

    public SDUse(SDValue value, SDNode user) {
        this.value = value;
        this.user = user;
        this.next = null;
        this.prev = null;
    }

    @Override
    public SDUse getPrev() {
        return prev;
    }

    @Override
    public void setPrev(SDUse prev) {
        this.prev = prev;
    }

    @Override
    public SDUse getNext() {
        return next;
    }

    @Override
    public void setNext(SDUse next) {
        this.next = next;
    }

    public SDValue getValue() {
        return value;
    }
}
