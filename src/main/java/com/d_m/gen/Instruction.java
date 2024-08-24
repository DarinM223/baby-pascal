package com.d_m.gen;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public record Instruction(String name, List<OperandPair> operands) {
    public void write(Writer writer) throws IOException {
        writer.write("new Instruction(\"" + name + "\", List.of(");
        var it = operands.iterator();
        while (it.hasNext()) {
            OperandPair operand = it.next();
            operand.write(writer);
            if (it.hasNext()) {
                writer.write(", ");
            }
        }
        writer.write("))");
    }
}
