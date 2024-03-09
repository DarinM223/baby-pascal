package com.d_m.util;

public class FreshImpl implements Fresh {
    private int counter = 0;

    @Override
    public int fresh() {
        return counter++;
    }

    @Override
    public void reset() {
        counter = 0;
    }
}
