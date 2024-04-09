package com.d_m.code;

import com.d_m.util.Symbol;

public record Quad(Operator op, Address result, Address input1, Address input2) {
    public String pretty(Symbol symbol) {
        return result.pretty(symbol) + " <- " + input1.pretty(symbol) + " " + op + " " + input2.pretty(symbol);
    }
}
