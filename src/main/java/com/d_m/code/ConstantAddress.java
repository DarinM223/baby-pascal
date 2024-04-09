package com.d_m.code;

import com.d_m.util.Symbol;

public record ConstantAddress(int value) implements Address {
    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public String pretty(Symbol symbol) {
        return this.toString();
    }
}
