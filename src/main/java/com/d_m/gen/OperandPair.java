package com.d_m.gen;

import com.d_m.select.instr.MachineOperandKind;

import java.io.IOException;
import java.io.Writer;

public record OperandPair(Operand operand, MachineOperandKind kind) {
    public void write(Writer writer) throws IOException {
        writer.write("new OperandPair(");
        operand.write(writer);
        writer.write(", ");
        kind.write(writer);
        writer.write(")");
    }
}
