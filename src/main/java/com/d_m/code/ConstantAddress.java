package com.d_m.code;

public record ConstantAddress(int value) implements Address {
    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
