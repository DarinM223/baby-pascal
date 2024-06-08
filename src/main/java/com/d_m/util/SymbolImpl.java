package com.d_m.util;

import java.util.HashMap;
import java.util.Map;

public class SymbolImpl implements Symbol {
    public static final int TOKEN = -1;
    public static final String TOKEN_STRING = "_TOKEN";

    private final Fresh fresh;
    private final Map<String, Integer> stringToSym;
    private final Map<Integer, String> symToString;

    public SymbolImpl(Fresh fresh) {
        this.fresh = fresh;
        stringToSym = new HashMap<>();
        symToString = new HashMap<>();
        symToString.put(TOKEN, TOKEN_STRING);
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
