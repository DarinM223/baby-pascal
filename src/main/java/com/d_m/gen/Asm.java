package com.d_m.gen;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public record Asm(List<Instruction> instructions) {
    public void write(Writer writer) throws IOException {
        writer.write("new Asm(List.of(");
        var it = instructions.iterator();
        while (it.hasNext()) {
            Instruction instruction = it.next();
            instruction.write(writer);
            if (it.hasNext()) {
                writer.write(", ");
            }
        }
        writer.write("))");
    }
}
