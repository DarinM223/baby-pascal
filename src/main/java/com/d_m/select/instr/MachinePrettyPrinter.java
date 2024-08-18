package com.d_m.select.instr;

import java.io.IOException;
import java.io.Writer;

public class MachinePrettyPrinter {
    private final Writer out;
    private int indentationLevel;

    public MachinePrettyPrinter(Writer out) {
        this.out = out;
        this.indentationLevel = 0;
    }

    public void writeFunction(MachineFunction function) throws IOException {
        start();
    }

    private void start() throws IOException {
        for (int i = 0; i < indentationLevel; i++) {
            out.write("  ");
        }
    }
}
