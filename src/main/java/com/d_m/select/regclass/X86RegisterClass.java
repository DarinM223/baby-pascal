package com.d_m.select.regclass;

import com.d_m.ast.IntegerType;
import com.d_m.ast.Type;

public class X86RegisterClass implements ISARegisterClass<RegisterClass> {
    public RegisterClass allIntegerRegs() {
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

    @Override
    public RegisterClass functionCallingConvention(Type type, int param) {
        return switch (type) {
            case IntegerType _ -> functionIntegerCallingConvention(param);
            default -> allIntegerRegs();
        };
    }

    public RegisterClass functionIntegerCallingConvention(int param) {
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

    @Override
    public RegisterClass fromRegisterName(String registerName) {
        return switch (registerName) {
            case "rax" -> rax();
            case "rbx" -> rbx();
            case "rcx" -> rcx();
            case "rdx" -> rdx();
            case "rdi" -> rdi();
            case "rsi" -> rsi();
            case "rbp" -> rbp();
            case "rsp" -> rsp();
            case "r8" -> r8();
            case "r9" -> r9();
            case "r10" -> r10();
            case "r11" -> r11();
            case "r12" -> r12();
            case "r13" -> r13();
            case "r14" -> r14();
            case "r15" -> r15();
            default -> throw new UnsupportedOperationException("Unknown register name " + registerName);
        };
    }
}
