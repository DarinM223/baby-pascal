package com.d_m.select.dag;

public class X86RegisterClass {
    public static RegisterClass allIntegerRegs() {
        return new RegisterClass("int", ~0 >>> 16, 1, 8);
    }

    public static RegisterClass rax() {
        return new RegisterClass("rax", 1, 1, 1);
    }
}
