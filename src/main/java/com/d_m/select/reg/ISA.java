package com.d_m.select.reg;

public interface ISA {
    Iterable<Register.Physical> allIntegerRegs();
    RegisterConstraint functionCallingConvention(RegisterClass registerClass, int param);
    Register.Physical physicalFromRegisterName(String registerName);
    RegisterConstraint constraintFromRegisterName(String registerName);
    String pretty(Register register);
    boolean isBranch(String opName);
}
