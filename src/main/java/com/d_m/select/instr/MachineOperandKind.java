package com.d_m.select.instr;

import java.io.IOException;
import java.io.Writer;

public enum MachineOperandKind {
    USE,
    DEF;

    public void write(Writer writer) throws IOException {
        switch (this) {
            case USE -> writer.write("MachineOperandKind.USE");
            case DEF -> writer.write("MachineOperandKind.DEF");
        }
    }
}
