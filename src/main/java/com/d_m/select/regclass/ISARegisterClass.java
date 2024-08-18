package com.d_m.select.regclass;

import com.d_m.ast.Type;

public interface ISARegisterClass<RegisterClass> {
    RegisterClass allIntegerRegs();
    RegisterClass functionCallingConvention(Type type, int param);
    RegisterClass fromRegisterName(String registerName);
}
