package com.d_m.util;

public interface Symbol {
    int getSymbol(String name);
    String getName(int symbol);
    void reset();
    Iterable<Integer> symbols();
}
