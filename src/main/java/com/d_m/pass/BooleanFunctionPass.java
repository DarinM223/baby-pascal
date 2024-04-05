package com.d_m.pass;

import com.d_m.ssa.Function;
import com.d_m.ssa.Module;

public abstract class BooleanFunctionPass implements FunctionPass<Boolean> {
    @Override
    public Boolean runModule(Module module) {
        boolean changed = false;
        for (Function function : module.getFunctionList()) {
            changed |= runFunction(function);
        }
        return changed;
    }
}
