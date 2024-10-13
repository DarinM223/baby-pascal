package com.d_m.cfg;

import com.d_m.ast.Type;
import com.d_m.code.Address;
import com.d_m.util.Symbol;

import java.util.List;

public record Phi(Type type, Address name, List<Address> ins) {
    public String pretty(Symbol symbol) {
        return name.pretty(symbol) + " <- Φ(" + String.join(", ", ins.stream().map(in -> in.pretty(symbol)).toList()) + ")";
    }
}
