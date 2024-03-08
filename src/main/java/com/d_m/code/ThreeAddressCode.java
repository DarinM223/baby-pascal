package com.d_m.code;

import java.util.List;

import com.d_m.util.Fresh;
import com.d_m.util.Symbol;
import com.d_m.ast.Statement;

public class ThreeAddressCode {
    private Fresh fresh;
    private Symbol symbol;

    public ThreeAddressCode(Fresh fresh, Symbol symbol) {
        this.fresh = fresh;
        this.symbol = symbol;
    }

    public List<Quad> normalize(List<Statement> statements) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
