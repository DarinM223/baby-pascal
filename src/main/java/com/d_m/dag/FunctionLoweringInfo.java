package com.d_m.dag;

import com.d_m.ast.Type;
import com.d_m.ssa.Instruction;
import com.d_m.ssa.Value;

import java.util.Map;

public class FunctionLoweringInfo {
    Map<Value, Register> valueRegisterMap;

    public Register initializeRegister(Instruction instruction) {
        return null;
    }

    public Register createRegisters(Type type) {
        return null;
    }

    public Register createRegister(Type type) {
        return null;
    }
}
