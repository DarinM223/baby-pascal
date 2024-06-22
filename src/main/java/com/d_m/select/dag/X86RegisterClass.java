package com.d_m.select.dag;

public class X86RegisterClass {
    public static RegisterClass allIntegerRegs() {
        return new RegisterClass("int", ~0 >>> 16, 1, 8);
    }

    public static RegisterClass rax() {
        return new RegisterClass("rax", 1, 1, 1);
    }

    public static RegisterClass rbx() {
        return new RegisterClass("rbx", 1 << 1, 1, 1);
    }

    public static RegisterClass rcx() {
        return new RegisterClass("rcx", 1 << 2, 1, 1);
    }

    public static RegisterClass rdx() {
        return new RegisterClass("rdx", 1 << 3, 1, 1);
    }

    public static RegisterClass rdi() {
        return new RegisterClass("rdi", 1 << 4, 1, 1);
    }

    public static RegisterClass rsi() {
        return new RegisterClass("rsi", 1 << 5, 1, 1);
    }

    public static RegisterClass rbp() {
        return new RegisterClass("rbp", 1 << 6, 1, 1);
    }

    public static RegisterClass rsp() {
        return new RegisterClass("rsp", 1 << 7, 1, 1);
    }

    public static RegisterClass r8() {
        return new RegisterClass("r8", 1 << 8, 1, 1);
    }

    public static RegisterClass r9() {
        return new RegisterClass("r9", 1 << 9, 1, 1);
    }

    public static RegisterClass r10() {
        return new RegisterClass("r10", 1 << 10, 1, 1);
    }

    public static RegisterClass r11() {
        return new RegisterClass("r11", 1 << 11, 1, 1);
    }

    public static RegisterClass r12() {
        return new RegisterClass("r12", 1 << 12, 1, 1);
    }

    public static RegisterClass r13() {
        return new RegisterClass("r13", 1 << 13, 1, 1);
    }

    public static RegisterClass r14() {
        return new RegisterClass("r14", 1 << 14, 1, 1);
    }

    public static RegisterClass r15() {
        return new RegisterClass("r15", 1 << 15, 1, 1);
    }

    public static RegisterClass functionIntegerCallingConvention(int param) {
        return switch (param) {
            case 0 -> rdi();
            case 1 -> rsi();
            case 2 -> rdx();
            case 3 -> rcx();
            case 4 -> r8();
            case 5 -> r9();
            default -> allIntegerRegs();
        };
    }
}
