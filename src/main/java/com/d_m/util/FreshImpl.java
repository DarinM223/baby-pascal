package com.d_m.util;

public class FreshImpl implements Fresh {
    private int counter;

    public FreshImpl() {
        counter = 0;
    }

    public FreshImpl(int counter) {
        this.counter = counter;
    }

    public int getCounter() {
        return counter;
    }

    @Override
    public int fresh() {
        return counter++;
    }

    @Override
    public void reset() {
        counter = 0;
    }
}
