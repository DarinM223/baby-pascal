package com.d_m.code;

public record TempAddress(int name) implements Address {
    @Override
    public String toString() {
        return "%" + name;
    }
}
