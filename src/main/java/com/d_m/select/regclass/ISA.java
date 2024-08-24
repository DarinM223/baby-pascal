package com.d_m.select.regclass;

public interface ISA {
    RegisterConstraint allIntegerRegs();
    RegisterConstraint functionCallingConvention(RegisterClass registerClass, int param);
    RegisterConstraint fromRegisterName(String registerName);
}
