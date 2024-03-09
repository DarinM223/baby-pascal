package com.d_m.code;

public record NameAddress(int name) implements Address {
    @Override
    public String toString() {
        return "%" + name;
    }
}
