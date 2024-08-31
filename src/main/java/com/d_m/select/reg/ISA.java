package com.d_m.select.reg;

public interface ISA {
    RegisterConstraint functionCallingConvention(RegisterClass registerClass, int param);
    RegisterConstraint fromRegisterName(String registerName);
    String pretty(Register register);
    boolean isBranch(String opName);
}
