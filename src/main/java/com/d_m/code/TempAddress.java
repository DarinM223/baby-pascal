package com.d_m.code;

import com.d_m.util.Symbol;

public record TempAddress(int name) implements Address {
    @Override
    public String toString() {
        return "%" + name;
    }

    @Override
    public String pretty(Symbol symbol) {
        return this.toString();
    }
}
