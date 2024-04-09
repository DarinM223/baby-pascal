package com.d_m.code;

import com.d_m.util.Symbol;

public record EmptyAddress() implements Address {
    @Override
    public String toString() {
        return "_";
    }

    @Override
    public String pretty(Symbol symbol) {
        return this.toString();
    }
}
