package com.d_m.code;

import com.d_m.util.Symbol;

public record NameAddress(int name, int unique) implements Address {
    public NameAddress(int name) {
        this(name, 0);
    }

    @Override
    public String toString() {
        if (unique == 0) {
            return "%" + name;
        }
        return "%" + name + "_" + unique;
    }

    @Override
    public String pretty(Symbol symbol) {
        if (symbol == null) {
            return this.toString();
        }
        String nameString = symbol.getName(name);
        return unique == 0 ? nameString : nameString + "_" + unique;
    }
}
