package com.d_m.code;

public record EmptyAddress() implements Address {
    @Override
    public String toString() {
        return "_";
    }
}
