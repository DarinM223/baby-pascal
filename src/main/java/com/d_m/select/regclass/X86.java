package com.d_m.select.regclass;

public class X86 implements ISA {
    public RegisterConstraint allIntegerRegs() {
        return new RegisterConstraint.OnRegister();
    }

    public static Register.Physical rax() {
        return new Register.Physical(0, RegisterClass.INT);
    }

    public static Register.Physical rbx() {
        return new Register.Physical(1, RegisterClass.INT);
    }

    public static Register.Physical rcx() {
        return new Register.Physical(2, RegisterClass.INT);
    }

    public static Register.Physical rdx() {
        return new Register.Physical(3, RegisterClass.INT);
    }

    public static Register.Physical rdi() {
        return new Register.Physical(4, RegisterClass.INT);
    }

    public static Register.Physical rsi() {
        return new Register.Physical(5, RegisterClass.INT);
    }

    public static Register.Physical rbp() {
        return new Register.Physical(6, RegisterClass.INT);
    }

    public static Register.Physical rsp() {
        return new Register.Physical(7, RegisterClass.INT);
    }

    public static Register.Physical r8() {
        return new Register.Physical(8, RegisterClass.INT);
    }

    public static Register.Physical r9() {
        return new Register.Physical(9, RegisterClass.INT);
    }

    public static Register.Physical r10() {
        return new Register.Physical(10, RegisterClass.INT);
    }

    public static Register.Physical r11() {
        return new Register.Physical(11, RegisterClass.INT);
    }

    public static Register.Physical r12() {
        return new Register.Physical(12, RegisterClass.INT);
    }

    public static Register.Physical r13() {
        return new Register.Physical(13, RegisterClass.INT);
    }

    public static Register.Physical r14() {
        return new Register.Physical(14, RegisterClass.INT);
    }

    public static Register.Physical r15() {
        return new Register.Physical(15, RegisterClass.INT);
    }

    @Override
    public RegisterConstraint functionCallingConvention(RegisterClass registerClass, int param) {
        return switch (registerClass) {
            case INT -> functionIntegerCallingConvention(param);
            default -> allIntegerRegs();
        };
    }

    public RegisterConstraint functionIntegerCallingConvention(int param) {
        return switch (param) {
            case 0 -> new RegisterConstraint.UsePhysical(rdi());
            case 1 -> new RegisterConstraint.UsePhysical(rsi());
            case 2 -> new RegisterConstraint.UsePhysical(rdx());
            case 3 -> new RegisterConstraint.UsePhysical(rcx());
            case 4 -> new RegisterConstraint.UsePhysical(r8());
            case 5 -> new RegisterConstraint.UsePhysical(r9());
            default -> new RegisterConstraint.OnStack();
        };
    }

    @Override
    public RegisterConstraint fromRegisterName(String registerName) {
        return new RegisterConstraint.UsePhysical(switch (registerName) {
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
        });
    }
}
