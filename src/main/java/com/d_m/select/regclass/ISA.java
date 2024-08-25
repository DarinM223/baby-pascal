package com.d_m.select.regclass;

public interface ISA {
    RegisterConstraint functionCallingConvention(RegisterClass registerClass, int param);
    RegisterConstraint fromRegisterName(String registerName);
    String pretty(Register register);
}
