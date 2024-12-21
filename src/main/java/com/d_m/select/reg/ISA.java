package com.d_m.select.reg;

public interface ISA {
    Iterable<Register.Physical> allIntegerRegs();
    Iterable<Register.Physical> calleeSaveRegs();
    RegisterConstraint functionCallingConvention(RegisterClass registerClass, int param);
    Register.Physical physicalFromRegisterName(String registerName);
    String physicalToRegisterName(Register.Physical register);
    RegisterConstraint constraintFromRegisterName(String registerName);
    String pretty(Register register);
    boolean isBranch(String opName);
}
