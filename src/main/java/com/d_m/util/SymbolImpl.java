package com.d_m.util;

import java.util.HashMap;
import java.util.Map;

public class SymbolImpl implements Symbol {
    private final Fresh fresh;
    private final Map<String, Integer> stringToSym;
    private final Map<Integer, String> symToString;

    public SymbolImpl(Fresh fresh) {
        this.fresh = fresh;
        stringToSym = new HashMap<>();
        symToString = new HashMap<>();
    }

    @Override
    public int getSymbol(String name) {
        Integer i = stringToSym.get(name);
        if (i != null) {
            return i;
        }

        int temp = fresh.fresh();
        stringToSym.put(name, temp);
        symToString.put(temp, name);
        return temp;
    }

    @Override
    public String getName(int symbol) {
        return symToString.get(symbol);
    }

    @Override
    public void reset() {
        fresh.reset();
        stringToSym.clear();
        symToString.clear();
    }

    @Override
    public Iterable<Integer> symbols() {
        return symToString.keySet();
    }
}
