package com.d_m.code;

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
}
