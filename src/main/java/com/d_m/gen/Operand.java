package com.d_m.gen;

public sealed interface Operand {
    record Immediate(int value) implements Operand {
    }

    record VirtualRegister(int register) implements Operand {
    }

    record Parameter(int parameter) implements Operand {
    }
}
