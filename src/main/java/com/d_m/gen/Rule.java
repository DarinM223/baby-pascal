package com.d_m.gen;

import java.io.IOException;
import java.io.Writer;

public record Rule(int cost, Tree pattern, Asm code) {
    public void write(Writer writer) throws IOException {
        writer.write("new Rule(" + cost + ", ");
        pattern.write(writer);
        writer.write(", ");
        code.write(writer);
        writer.write(")");
    }
}
