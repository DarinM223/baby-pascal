package com.d_m.code;

public record Quad(Operator op, Address result, Address input1, Address input2) {
    @Override
    public String toString() {
        return result + " <- " + input1 + " " + op + " " + input2;
    }
}
